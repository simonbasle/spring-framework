/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.bean.override;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import static org.springframework.core.annotation.MergedAnnotations.SearchStrategy.DIRECT;

/**
 * Internal parsing utilities to discover the presence of
 * {@link BeanOverride @BeanOverride} and create the relevant
 * {@link OverrideMetadata} if necessary.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
abstract class BeanOverrideParsingUtils {

	/**
	 * Check if at least one field of the given {@code clazz} is meta-annotated
	 * with {@link BeanOverride @BeanOverride}.
	 * @param clazz the class which fields to inspect
	 * @return {@code true} if there is a bean override annotation present,
	 * {@code false} otherwise
	 */
	static boolean hasBeanOverride(Class<?> clazz) {
		AtomicBoolean hasBeanOverride = new AtomicBoolean();
		ReflectionUtils.doWithFields(clazz, field -> {
			if (hasBeanOverride.get()) {
				return;
			}
			boolean present = MergedAnnotations.from(field, DIRECT).isPresent(BeanOverride.class);
			hasBeanOverride.compareAndSet(false, present);
		});
		return hasBeanOverride.get();
	}

	/**
	 * Parse the specified classes for the presence of fields annotated with
	 * {@link BeanOverride @BeanOverride}, and create an {@link OverrideMetadata}
	 * for each logically identical identified injection point, suitable for
	 * being used as a key in the TCF context cache.
	 * @param clazz the class to parse
	 * @see #parseAll(Class)
	 */
	static Set<OverrideMetadata> parseDistinct(Class<?> clazz) {
		List<OverrideMetadata> all = new LinkedList<>();
		ReflectionUtils.doWithFields(clazz, field -> parseField(field, clazz, all));
		return new HashSet<>(all);
	}

	static List<OverrideMetadata> parseAll(Class<?> clazz) {
		return parseAll(List.of(clazz));
	}

	static List<OverrideMetadata> parseAll(Iterable<Class<?>> classes) {
		List<OverrideMetadata> all = new LinkedList<>();
		classes.forEach(c -> ReflectionUtils.doWithFields(c, field -> parseField(field, c, all)));
		return all;
	}

	private static void parseField(Field field, Class<?> testClass, List<OverrideMetadata> metadataList) {
		AtomicBoolean overrideAnnotationFound = new AtomicBoolean();
		MergedAnnotations.from(field, DIRECT).stream(BeanOverride.class).forEach(mergedAnnotation -> {
			MergedAnnotation<?> metaSource = mergedAnnotation.getMetaSource();
			Assert.state(metaSource != null, "@BeanOverride annotation must be meta-present");

			BeanOverride beanOverride = mergedAnnotation.synthesize();
			BeanOverrideProcessor processor = BeanUtils.instantiateClass(beanOverride.value());
			Annotation composedAnnotation = metaSource.synthesize();

			Assert.state(overrideAnnotationFound.compareAndSet(false, true),
					() -> "Multiple @BeanOverride annotations found on field: " + field);
			OverrideMetadata metadata = processor.createMetadata(composedAnnotation, testClass, field);
			metadataList.add(metadata);
		});
	}

}

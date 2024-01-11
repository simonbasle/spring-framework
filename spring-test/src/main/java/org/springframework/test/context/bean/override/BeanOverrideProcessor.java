/*
 * Copyright 2002-2023 the original author or authors.
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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.lang.Nullable;

/**
 * An interface for Bean Overriding concrete processing.
 * Processors are generally linked to one or more specific
 * concrete annotations (meta-annotated with {@link BeanOverride})
 * and specify different steps in the process of parsing these
 * annotations, ultimately creating {@link OverrideMetadata}
 * which will be used to instantiate the overrides.
 * <p>Implementations are required to have a no-argument constructor.
 */
@FunctionalInterface
public interface BeanOverrideProcessor {

	/**
	 * Determine a {@link Set} of {@link ResolvableType} for each of
	 * which an {@link OverrideMetadata} instance will be created.
	 * Override to provide more than one type per annotated element, or to use
	 * the annotation to determine type(s).
	 * <p>Defaults to an empty set if the annotated element is not a {@link Field}.
	 * For fields, defaults to a single {@link ResolvableType} that additionally tracks
	 * the source class if the field is a {@link TypeVariable}.
	 */
	default Set<ResolvableType> getOrDeduceTypes(AnnotatedElement element, Annotation annotation, Class<?> source) {
		Set<ResolvableType> types = new LinkedHashSet<>();
		if (element instanceof  Field field) {
			types.add((field.getGenericType() instanceof TypeVariable) ? ResolvableType.forField(field, source)
				: ResolvableType.forField(field));
		}
		return types;
	}

	/**
	 * Create an {@link OverrideMetadata} for a given annotated element and target
	 * {@link #getOrDeduceTypes(AnnotatedElement, Annotation, Class) type}.
	 * Specific implementations of metadata can have state to be used during override
	 * {@link OverrideMetadata#createOverride(String, BeanDefinition, Object) instance creation}
	 * (e.g. from further parsing the annotation or the annotated field).
	 * @param element the annotated field, method or class
	 * @param overrideAnnotation the element annotation
	 * @param typeToOverride the target type (there can be multiple types per annotated element, e.g. derived from the annotation)
	 * @param qualifier the optional {@link QualifierMetadata}
	 * @return a new {@link OverrideMetadata}
	 * @see #getOrDeduceTypes(AnnotatedElement, Annotation, Class)
	 * @see #isQualifierAnnotation(Annotation)
	 * @see MergedAnnotation#synthesize()
	 */
	OverrideMetadata createMetadata(AnnotatedElement element, Annotation overrideAnnotation, ResolvableType typeToOverride,
			@Nullable QualifierMetadata qualifier);

	/**
	 * Define if an annotation should be considered as a qualifier annotation,
	 * in which case it will be used in the bean definition when creating the
	 * override.
	 */
	default boolean isQualifierAnnotation(Annotation annotation) {
		return false;
	}
}

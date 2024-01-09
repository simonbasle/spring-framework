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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * A parser that discovers annotations meta-annotated with {@link BeanOverride} on
 * fields of a class and creates {@link OverrideMetadata} accordingly.
 */
class BeanOverrideParser {

	private final Set<OverrideMetadata> parsedMetadata;

	BeanOverrideParser() {
		this(Collections.emptySet());
	}

	BeanOverrideParser(Collection<? extends OverrideMetadata> existing) {
		this.parsedMetadata = new LinkedHashSet<>();
		this.parsedMetadata.addAll(existing);
	}

	void parse(Class<?> configurationClass) {
		ReflectionUtils.doWithFields(configurationClass, field -> parseField(field, configurationClass));
	}

	Set<OverrideMetadata> getOverrideMetadata() {
		return Collections.unmodifiableSet(this.parsedMetadata);
	}

	private void parseField(Field field, Class<?> source) {
		MergedAnnotations annotations = MergedAnnotations.from(field, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
		annotations.stream(BeanOverride.class)
				.map(MergedAnnotation::synthesize)
				.forEach((annotation) -> {
					final BeanOverrideProcessor processor = getProcessorInstance(annotation.processor());
					if (processor == null) {
						return;
					}
					Set<ResolvableType> typesToOverride = processor.getOrDeduceTypes(field, annotation, source);
					QualifierMetadata qualifier = QualifierMetadata.forElement(field, processor::isQualifierAnnotation);

					for (ResolvableType type : typesToOverride) {
						// By default, a simple implementation of OverrideMetadata is provided. Can be a subclass if
						// the processing has specific needs.
						OverrideMetadata metadata = processor.createMetadata(field, annotation, type, qualifier);

						boolean isNewDefinition = this.parsedMetadata.add(metadata);
						Assert.state(isNewDefinition, () -> "Duplicate " + metadata.getBeanOverrideDescription() + " overrideMetadata " + metadata);
					}
				});
	}

	@Nullable //TODO implement caching ? have a processor attribute to determine if stateful ?
	private BeanOverrideProcessor getProcessorInstance(Class<? extends BeanOverrideProcessor> processorClass) {
		final Constructor<? extends BeanOverrideProcessor> constructor = ClassUtils.getConstructorIfAvailable(processorClass);
		if (constructor != null) {
			ReflectionUtils.makeAccessible(constructor);
			try {
				return constructor.newInstance();
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new BeanDefinitionValidationException("Could not get an instance of BeanOverrideProcessor", e);
			}
		}
		return null;
	}
}

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
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Base abstract parser to create {@link BeanOverrideDefinition} instances from
 * annotations declared on or in a class.
 * <p>This parser specifically tracks definitions of {@link Field fields} and {@link Method methods}.
 */
public abstract class BeanOverrideDefinitionsParser<A extends Annotation, D extends BeanOverrideDefinition> {

	private final Set<D> definitions;

	private final Map<D, Field> definitionFields;

	private final Map<D, Method> definitionMethods;

	protected BeanOverrideDefinitionsParser() {
		this(Collections.emptySet());
	}

	protected BeanOverrideDefinitionsParser(Collection<? extends D> existing) {
		this.definitions = new LinkedHashSet<>();
		this.definitionFields = new LinkedHashMap<>();
		this.definitionMethods = new LinkedHashMap<>();
		if (existing != null) {
			this.definitions.addAll(existing);
		}
	}

	protected abstract Class<A> annotationClass();

	void parse(Class<?> source) {
		parseElement(source, null);
		ReflectionUtils.doWithFields(source, (element) -> parseElement(element, source));
		ReflectionUtils.doWithMethods(source, (method) -> parseElement(method, source));
	}

	private void parseElement(AnnotatedElement element, Class<?> source) {
		MergedAnnotations annotations = MergedAnnotations.from(element, SearchStrategy.SUPERCLASS);
		annotations.stream(annotationClass())
			.map(MergedAnnotation::synthesize)
			.forEach((annotation) -> doParseBeanOverrideAnnotation(annotation, element, source));
	}

	/**
	 * Find a {@link org.springframework.core.ResolvableType} for the annotated element and
	 * use {@link #addDefinition(AnnotatedElement, BeanOverrideDefinition, String)} on type(s)
	 * as needed.
	 */
	protected abstract void doParseBeanOverrideAnnotation(A annotation, AnnotatedElement element, Class<?> source);

	protected final void addDefinition(AnnotatedElement element, D definition, String type) {
		boolean isNewDefinition = this.definitions.add(definition);
		Assert.state(isNewDefinition, () -> "Duplicate " + type + " definition " + definition);
		if (element instanceof Field field) {
			this.definitionFields.put(definition, field);
		}
		else if (element instanceof Method method) {
			this.definitionMethods.put(definition, method);
		}
	}

	Set<D> getDefinitions() {
		return Collections.unmodifiableSet(this.definitions);
	}

	@Nullable
	Field getField(D definition) {
		return this.definitionFields.get(definition);
	}

	@Nullable
	Method getMethod(D definition) {
		return this.definitionMethods.get(definition);
	}

}

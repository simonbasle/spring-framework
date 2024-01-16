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

package org.springframework.test.context.bean.override.example;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Set;

import org.springframework.core.ResolvableType;
import org.springframework.test.context.bean.override.BeanOverrideProcessor;
import org.springframework.test.context.bean.override.OverrideMetadata;
import org.springframework.test.context.bean.override.QualifierMetadata;

public class TestBeanOverrideProcessor implements BeanOverrideProcessor {

	public TestBeanOverrideProcessor() {
	}

	@Override
	public boolean isQualifierAnnotation(Annotation annotation) {
		return !(annotation instanceof TestBeanOverrideAnnotation);
	}

	@Override
	public Set<ResolvableType> getOrDeduceTypes(AnnotatedElement element, Annotation annotation, Class<?> source) {
		var superSet = BeanOverrideProcessor.super.getOrDeduceTypes(element, annotation, source);
		if (superSet.isEmpty() && element instanceof Class c) {
			superSet.add(ResolvableType.forClass(c));
		}
		return superSet;
	}

	private static final TestOverrideMetadata CONSTANT = new TestOverrideMetadata() {
		@Override
		public String toString() {
			return "{DUPLICATE_TRIGGER}";
		}
	};
	public static final String DUPLICATE_TRIGGER = "CONSTANT";

	@Override
	public List<OverrideMetadata> createMetadata(AnnotatedElement element, Annotation overrideAnnotation,
			Set<ResolvableType> typesToOverride, QualifierMetadata qualifier) {
		if (!(overrideAnnotation instanceof TestBeanOverrideAnnotation annotation) || typesToOverride.size() != 1) {
			throw new IllegalStateException("unexpected annotation or typesToOverride size");
		}
		if (annotation.value().equals(DUPLICATE_TRIGGER)) {
			return List.of(CONSTANT);
		}
		return List.of(new TestOverrideMetadata(element, annotation, typesToOverride.iterator().next(), qualifier));
	}
}

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

package org.springframework.test.bean.override;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OverrideMetadataTests {

	static class ConcreteOverrideMetadata extends OverrideMetadata {

		ConcreteOverrideMetadata(AnnotatedElement element, Annotation overrideAnnotation, ResolvableType typeToOverride, @Nullable QualifierMetadata qualifier) {
			super(element, overrideAnnotation, typeToOverride, qualifier);
		}

		@Override
		public BeanOverrideStrategy getBeanOverrideStrategy() {
			return BeanOverrideStrategy.REPLACE_DEFINITION;
		}

		@Override
		public String getBeanOverrideDescription() {
			return ConcreteOverrideMetadata.class.getSimpleName();
		}

		@Override
		protected Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition, @Nullable Object existingBeanInstance) {
			return BeanOverrideStrategy.REPLACE_DEFINITION;
		}
	}

	@NonNull
	public static String annotated() {
		return "exampleMethod";
	}

	@NonNull
	public String annotated = "exampleField";

	@FunctionalInterface
	public interface Annotated {
		String exampleClass();
	}

	static OverrideMetadata exampleOverride() {
		final Method annotated = ClassUtils.getStaticMethod(OverrideMetadataTests.class, "annotated");
		return new ConcreteOverrideMetadata(Objects.requireNonNull(annotated), annotated.getAnnotation(NonNull.class),
				ResolvableType.forClass(String.class), null);
	}

	@Test
	void implicitConfigurations() {
		final OverrideMetadata metadata = exampleOverride();
		assertThat(metadata.getExplicitBeanName()).as("explicitBeanName").isEmpty();
		assertThat(metadata.getOrCreateTracker(null)).as("no tracking").isSameAs(OverrideMetadata.NO_TRACKING);
	}

	@Test
	void methodElement() {
		final Method annotated = ClassUtils.getStaticMethod(OverrideMetadataTests.class, "annotated");
		final ConcreteOverrideMetadata metadata = new ConcreteOverrideMetadata(Objects.requireNonNull(annotated), annotated.getAnnotation(NonNull.class),
				ResolvableType.forClass(String.class), null);

		assertThat(metadata.methodElement()).as("methodElement").isSameAs(annotated).isSameAs(metadata.element());
		assertThat(metadata.fieldElement()).as("fieldElement").isNull();;
		assertThat(metadata.classElement()).as("classElement").isNull();;
	}

	@Test
	void fieldElement() throws NoSuchFieldException {
		final Field annotated = Objects.requireNonNull(OverrideMetadataTests.class.getField("annotated"));
		final ConcreteOverrideMetadata metadata = new ConcreteOverrideMetadata(annotated, annotated.getAnnotation(NonNull.class),
				ResolvableType.forClass(String.class), null);

		assertThat(metadata.methodElement()).as("methodElement").isNull();;
		assertThat(metadata.fieldElement()).as("fieldElement").isSameAs(annotated).isSameAs(metadata.element());
		assertThat(metadata.classElement()).as("classElement").isNull();;
	}

	@Test
	void classElement() {
		final Class<?> annotated = OverrideMetadataTests.Annotated.class;
		final ConcreteOverrideMetadata metadata = new ConcreteOverrideMetadata(annotated, annotated.getAnnotation(NonNull.class),
				ResolvableType.forClass(String.class), null);

		assertThat(metadata.methodElement()).as("methodElement").isNull();;
		assertThat(metadata.fieldElement()).as("fieldElement").isNull();;
		assertThat(metadata.classElement()).as("classElement").isSameAs(annotated).isSameAs(metadata.element());
	}

}

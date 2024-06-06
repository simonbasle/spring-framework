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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.example.CustomQualifier;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link OverrideMetadata}.
 *
 * @author Simon Baslé
 * @since 6.2
 */
public class OverrideMetadataTests {

	String annotated = "exampleField";

	@Test
	void forTestClassWithSingleField() {
		List<OverrideMetadata> overrideMetadata = OverrideMetadata.forTestClass(SingleAnnotation.class);
		assertThat(overrideMetadata).singleElement().satisfies(hasTestBeanMetadata(
				field(SingleAnnotation.class, "message"), String.class, null));
	}

	@Test
	void forTestClassWithSingleFieldWithBeanName() {
		List<OverrideMetadata> overrideMetadata = OverrideMetadata.forTestClass(SingleAnnotationWithName.class);
		assertThat(overrideMetadata).singleElement().satisfies(hasTestBeanMetadata(
				field(SingleAnnotationWithName.class, "message"), String.class, "messageBean"));
	}

	@Test
	void forTestClassWithMultipleFields() {
		List<OverrideMetadata> overrideMetadata = OverrideMetadata.forTestClass(MultipleAnnotations.class);
		assertThat(overrideMetadata).hasSize(2)
				.anySatisfy(hasTestBeanMetadata(
						field(MultipleAnnotations.class, "message"), String.class, null))
				.anySatisfy(hasTestBeanMetadata(
						field(MultipleAnnotations.class, "counter"), Integer.class, null));
	}

	@Test
	void forTestClassWithMultipleFieldsSameMetadata() {
		List<OverrideMetadata> overrideMetadata = OverrideMetadata.forTestClass(MultipleAnnotationsDuplicate.class);
		assertThat(overrideMetadata).hasSize(2)
				.anySatisfy(hasTestBeanMetadata(
						field(MultipleAnnotationsDuplicate.class, "message1"), String.class, null))
				.anySatisfy(hasTestBeanMetadata(
						field(MultipleAnnotationsDuplicate.class, "message2"), String.class, null));
		assertThat(new HashSet<>(overrideMetadata)).hasSize(1);
	}

	@Test
	void forTestClassWithDifferentOverrideMetadataOnSameField() {
		Field faultyField = field(MultipleAnnotationsOnSameField.class, "message");
		assertThatIllegalStateException()
				.isThrownBy(() -> OverrideMetadata.forTestClass(MultipleAnnotationsOnSameField.class))
				.withMessageStartingWith("Multiple @BeanOverride annotations found")
				.withMessageContaining(faultyField.toString());
	}

	@Test
	void getBeanNameIsNullByDefault() {
		OverrideMetadata metadata = new DummyOverrideMetadata(field(getClass(), "annotated"), String.class);
		assertThat(metadata.getBeanName()).isNull();
	}

	@Test
	void hashCodeAndEqualsShouldWorkOnDifferentClasses() {
		hashCodeAndEqualsShouldWorkOnDifferentClasses(f -> new DummyOverrideMetadata(f, ExampleService.class));
	}

	public static void hashCodeAndEqualsShouldWorkOnDifferentClasses(Function<Field, OverrideMetadata> metadataFunction) {
		OverrideMetadata directQualifier1 = metadataFunction.apply(ReflectionUtils.findField(ConfigA.class, "directQualifier"));
		OverrideMetadata directQualifier2 = metadataFunction.apply(ReflectionUtils.findField(ConfigB.class, "directQualifier"));
		OverrideMetadata differentDirectQualifier1 = metadataFunction.apply(ReflectionUtils.findField(ConfigA.class, "differentDirectQualifier"));
		OverrideMetadata differentDirectQualifier2 = metadataFunction.apply(ReflectionUtils.findField(ConfigB.class, "differentDirectQualifier"));
		OverrideMetadata customQualifier1 = metadataFunction.apply(ReflectionUtils.findField(ConfigA.class, "customQualifier"));
		OverrideMetadata customQualifier2 = metadataFunction.apply(ReflectionUtils.findField(ConfigB.class, "customQualifier"));

		assertThat(directQualifier1).hasSameHashCodeAs(directQualifier2);
		assertThat(differentDirectQualifier1).hasSameHashCodeAs(differentDirectQualifier2);
		assertThat(customQualifier1).hasSameHashCodeAs(customQualifier2);
		assertThat(differentDirectQualifier1).isEqualTo(differentDirectQualifier1)
				.isEqualTo(differentDirectQualifier2)
				.isNotEqualTo(directQualifier2);
		assertThat(directQualifier1).isEqualTo(directQualifier1)
				.isEqualTo(directQualifier2)
				.isNotEqualTo(differentDirectQualifier1);
		assertThat(customQualifier1).isEqualTo(customQualifier1)
				.isEqualTo(customQualifier2)
				.isNotEqualTo(differentDirectQualifier1);
	}

	private Field field(Class<?> target, String fieldName) {
		Field field = ReflectionUtils.findField(target, fieldName);
		assertThat(field).isNotNull();
		return field;
	}

	private Consumer<OverrideMetadata> hasTestBeanMetadata(Field field, Class<?> beanType, @Nullable String beanName) {
		return hasOverrideMetadata(field, beanType, BeanOverrideStrategy.REPLACE_DEFINITION, beanName);
	}

	private Consumer<OverrideMetadata> hasOverrideMetadata(Field field, Class<?> beanType, BeanOverrideStrategy strategy, @Nullable String beanName) {
		return metadata -> {
			assertThat(metadata.getField()).isEqualTo(field);
			assertThat(metadata.getBeanType().toClass()).isEqualTo(beanType);
			assertThat(metadata.getStrategy()).isEqualTo(strategy);
			assertThat(metadata.getBeanName()).isEqualTo(beanName);
		};
	}

	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@TestBean(methodName = "counter")
	public @interface CounterTestBean { }


	static class SingleAnnotation {

		@TestBean(methodName = "onField")
		String message;

		static String onField() {
			return "OK";
		}
	}

	static class SingleAnnotationWithName {

		@TestBean(name = "messageBean")
		String message;

		static String messageTestOverride() {
			return "OK";
		}
	}

	static class MultipleAnnotations {

		@TestBean(methodName = "message")
		String message;

		@TestBean(methodName = "counter")
		Integer counter;

		static String message() {
			return "OK";
		}

		static Integer counter() {
			return 42;
		}
	}

	static class MultipleAnnotationsDuplicate {

		@TestBean(methodName = "message")
		String message1;

		@TestBean(methodName = "message")
		String message2;

		static String message() {
			return "OK";
		}
	}

	static class MultipleAnnotationsOnSameField {

		@CounterTestBean()
		@TestBean(methodName = "foo")
		String message;

		static String foo() {
			return "foo";
		}
	}

	public static class ConfigA {

		private ExampleService noQualifier;

		@Qualifier("test")
		private ExampleService directQualifier;

		@Qualifier("different")
		private ExampleService differentDirectQualifier;

		@CustomQualifier
		private ExampleService customQualifier;

	}

	public static class ConfigB {

		@Qualifier("test")
		private ExampleService directQualifier;

		@Qualifier("different")
		private ExampleService differentDirectQualifier;

		@CustomQualifier
		private ExampleService customQualifier;

	}


	static class DummyOverrideMetadata extends OverrideMetadata {

		DummyOverrideMetadata(Field field, Class<?> typeToOverride) {
			super(field, ResolvableType.forClass(typeToOverride), BeanOverrideStrategy.REPLACE_DEFINITION);
		}

		@Override
		protected Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition,
				@Nullable Object existingBeanInstance) {

			return BeanOverrideStrategy.REPLACE_DEFINITION;
		}
	}

}

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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

class BeanOverrideParserTests {

	static class TestOverrideMetadata extends OverrideMetadata {

		public TestOverrideMetadata(Field field, TestBeanOverrideAnnotation overrideAnnotation,
				ResolvableType typeToOverride, @Nullable QualifierMetadata qualifier) {
			super(field, overrideAnnotation, typeToOverride, qualifier);
		}

		@Override
		public BeanOverrideStrategy getBeanOverrideStrategy() {
			return BeanOverrideStrategy.REPLACE_DEFINITION;
		}

		@Override
		public String getBeanOverrideDescription() {
			return "test";
		}

		@Override
		protected Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition,
				@Nullable Object existingBeanInstance) {
			return "TEST OVERRIDE";
		}
	}


	static class TestBeanOverrideProcessor implements BeanOverrideProcessor {

		public TestBeanOverrideProcessor() {
		}

		@Override
		public OverrideMetadata createMetadata(AnnotatedElement element, Annotation overrideAnnotation, ResolvableType typeToOverride, QualifierMetadata qualifier) {
			if (!(element instanceof Field f)) {
				throw new IllegalArgumentException("TestBeanOverrideProcessor only supports annotation on fields");
			}
			if (!(overrideAnnotation instanceof TestBeanOverrideAnnotation annotation)) {
				throw new IllegalStateException("synthetic annotation error");
			}
			return new TestOverrideMetadata(f, annotation, typeToOverride, qualifier);
		}
	}

	@BeanOverride(processor = TestBeanOverrideProcessor.class)
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface TestBeanOverrideAnnotation { }

	@Configuration
	static class TestOverrideConfiguration {

		@TestBeanOverrideAnnotation
		String message;

	}

	@Test
	void test() {
		BeanOverrideParser parser = new BeanOverrideParser();
		parser.parse(TestOverrideConfiguration.class);

		assertThat(parser.getOverrideMetadata()).hasSize(1)
				.first()
				.extracting(om -> om.createOverride("foo", null, null))
				.hasToString("TEST OVERRIDE");
	}

}
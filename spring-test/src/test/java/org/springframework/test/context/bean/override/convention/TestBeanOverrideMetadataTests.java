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

package org.springframework.test.context.bean.override.convention;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.bean.override.OverrideMetadata;
import org.springframework.test.context.bean.override.OverrideMetadataTests;
import org.springframework.test.context.bean.override.OverrideMetadataTests.ConfigA;
import org.springframework.test.context.bean.override.OverrideMetadataTests.ConfigB;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestBeanOverrideMetadata}.
 *
 * @author Stephane Nicoll
 */
class TestBeanOverrideMetadataTests {

	@Test
	void identitySameMetadata() {
		

	}


	@Test
	void testBeanMetadataHashCodeAndEqualsShouldWorkOnDifferentClassesAndSameMethods() {
		ResolvableType beanType = ResolvableType.forClass(ExampleService.class);
		Method method = ReflectionUtils.findMethod(FirstCase.class, "factoryMethod");
		TestBean annotation = AnnotationUtils.getAnnotation(field(FirstCase.class, "service"),
				TestBean.class);

		OverrideMetadataTests.hashCodeAndEqualsShouldWorkOnDifferentClasses(f -> new TestBeanOverrideMetadata(f, method, annotation, beanType));
	}

	@Test
	void testBeanMetadataHashCodeAndEqualsShouldDivergeForDifferentMethods() {
		ResolvableType beanType = ResolvableType.forClass(ExampleService.class);
		Method method1 = ReflectionUtils.findMethod(FirstCase.class, "factoryMethod");
		Method method2 = ReflectionUtils.findMethod(OtherCase.class, "factoryMethod");
		TestBean annotation = AnnotationUtils.getAnnotation(field(FirstCase.class, "service"),
				TestBean.class);

		assertThat(method1).isNotNull().isNotEqualTo(method2);
		assertThat(method2).isNotNull();

		OverrideMetadata directQualifier1 = new TestBeanOverrideMetadata(field(ConfigA.class, "directQualifier"),
				method1, annotation, beanType);
		OverrideMetadata directQualifier2 = new TestBeanOverrideMetadata(field(ConfigB.class, "directQualifier"),
				method2, annotation, beanType);
		OverrideMetadata differentDirectQualifier1 = new TestBeanOverrideMetadata(field(ConfigA.class, "differentDirectQualifier"),
				method1, annotation, beanType);
		OverrideMetadata differentDirectQualifier2 = new TestBeanOverrideMetadata(field(ConfigB.class, "differentDirectQualifier"),
				method2, annotation, beanType);
		OverrideMetadata customQualifier1 = new TestBeanOverrideMetadata(field(ConfigA.class, "customQualifier"),
				method1, annotation, beanType);
		OverrideMetadata customQualifier2 = new TestBeanOverrideMetadata(field(ConfigB.class, "customQualifier"),
				method2, annotation, beanType);

		assertThat(directQualifier1).doesNotHaveSameHashCodeAs(directQualifier2);
		assertThat(differentDirectQualifier1).doesNotHaveSameHashCodeAs(differentDirectQualifier2);
		assertThat(customQualifier1).doesNotHaveSameHashCodeAs(customQualifier2);
		assertThat(differentDirectQualifier1).isEqualTo(differentDirectQualifier1)
				.isNotEqualTo(differentDirectQualifier2)
				.isNotEqualTo(directQualifier2);
		assertThat(directQualifier1).isEqualTo(directQualifier1)
				.isNotEqualTo(directQualifier2)
				.isNotEqualTo(differentDirectQualifier1);
		assertThat(customQualifier1).isEqualTo(customQualifier1)
				.isNotEqualTo(customQualifier2)
				.isNotEqualTo(differentDirectQualifier1);
	}

	private Field field(Class<?> target, String name) {
		Field field = ReflectionUtils.findField(target, name);
		assertThat(field).isNotNull();
		return field;
	}

	static class FirstCase {

		@TestBean(methodName = "factoryMethod")
		private ExampleService service;

		private static ExampleService factoryMethod() {
			return new RealExampleService("first case");
		}
	}

	static class OtherCase {
		private static ExampleService factoryMethod() {
			return new RealExampleService("other case");
		}
	}

}

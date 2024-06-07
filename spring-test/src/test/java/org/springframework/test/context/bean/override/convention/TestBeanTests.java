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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.BeanOverrideStrategy;
import org.springframework.test.context.bean.override.OverrideMetadata;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestBean}.
 *
 * @author Stephane Nicoll
 */
public class TestBeanTests {

	@Test
	void testBeanCanBeUsedAsMetaAnnotation() {
		List<OverrideMetadata> metadata = OverrideMetadata.forTestClass(SampleMetaAnnotation.class);
		assertThat(metadata).singleElement().satisfies(
				hasTestBeanMetadata(field(SampleMetaAnnotation.class, "msg"), String.class, null));
	}

	private Field field(Class<?> target, String fieldName) {
		Field field = ReflectionUtils.findField(target, fieldName);
		assertThat(field).isNotNull();
		return field;
	}

	private Consumer<OverrideMetadata> hasTestBeanMetadata(Field field, Class<?> beanType, @Nullable String beanName) {
		return metadata -> {
			assertThat(metadata).isInstanceOf(TestBeanOverrideMetadata.class);
			assertThat(metadata.getField()).isEqualTo(field);
			assertThat(metadata.getBeanType().toClass()).isEqualTo(beanType);
			assertThat(metadata.getStrategy()).isEqualTo(BeanOverrideStrategy.REPLACE_DEFINITION);
			assertThat(metadata.getBeanName()).isEqualTo(beanName);
		};
	}


	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@TestBean(methodName = "message")
	public @interface MessageTestBean {}


	static class SampleMetaAnnotation {

		@MessageTestBean
		private String msg;

		static String message() {
			return "OK";
		}

	}

}

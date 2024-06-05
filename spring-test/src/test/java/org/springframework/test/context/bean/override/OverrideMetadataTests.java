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

import java.lang.reflect.Field;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.example.CustomQualifier;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OverrideMetadata}.
 *
 * @author Simon Baslé
 * @since 6.2
 */
public class OverrideMetadataTests {

	@Test
	void implicitConfigurations() throws Exception {
		OverrideMetadata metadata = exampleOverride();
		assertThat(metadata.getBeanName()).as("expectedBeanName").isNull();
	}

	@Test
	void hashCodeAndEqualsShouldWorkOnDifferentClasses() {
		ResolvableType beanType = ResolvableType.forClass(ExampleService.class);
		hashCodeAndEqualsShouldWorkOnDifferentClasses(f -> new ConcreteOverrideMetadata(f, beanType, BeanOverrideStrategy.REPLACE_DEFINITION));
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

	@NonNull
	String annotated = "exampleField";

	private static OverrideMetadata exampleOverride() throws Exception {
		Field field = OverrideMetadataTests.class.getDeclaredField("annotated");
		return new ConcreteOverrideMetadata(field, ResolvableType.forClass(String.class),
				BeanOverrideStrategy.REPLACE_DEFINITION);
	}

	static class ConcreteOverrideMetadata extends OverrideMetadata {

		ConcreteOverrideMetadata(Field field, ResolvableType typeToOverride,
				BeanOverrideStrategy strategy) {

			super(field, typeToOverride, strategy);
		}

		@Override
		protected Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition,
				@Nullable Object existingBeanInstance) {

			return BeanOverrideStrategy.REPLACE_DEFINITION;
		}
	}

}

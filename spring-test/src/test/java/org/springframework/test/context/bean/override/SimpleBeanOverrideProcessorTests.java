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

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.FailingExampleService;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class SimpleBeanOverrideProcessorTests {

	@Configuration(proxyBeanMethods = false)
	static class NoMatchingMethodConventionBeans {

		@TestBean
		private ExampleService mock;

		@Bean
		ExampleService example1() {
			return new FailingExampleService();
		}
	}

	@Test
	void byConventionLooksForBothFieldNameAndBeanName() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		BeanOverrideBeanPostProcessor.register(context);
		context.register(NoMatchingMethodConventionBeans.class);
		assertThatIllegalStateException().isThrownBy(context::refresh)
				.withMessageContaining("Expected one static method of [mockTestOverride, example1TestOverride] on class "
						+ NoMatchingMethodConventionBeans.class.getName() + " with return type "
						+ ExampleService.class.getName());
	}

	//FIXME add more tests around conventions, etc...
}

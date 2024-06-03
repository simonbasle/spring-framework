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

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.mock.MockCreationSettings;

import org.springframework.core.ResolvableType;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.ExampleServiceCaller;
import org.springframework.test.context.bean.override.example.RealExampleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MockitoSpyBeanMetadata}.
 *
 * @author Phillip Webb
 */
//FIXME
class MockitoSpyBeanMetadataTests {
//
//	private static final ResolvableType REAL_SERVICE_TYPE = ResolvableType.forClass(RealExampleService.class);
//
//	@Test
//	void classToSpyMustNotBeNull() {
//		assertThatIllegalArgumentException().isThrownBy(() -> new MockitoSpyBeanMetadata(null, null, null, true, null))
//			.withMessageContaining("TypeToSpy must not be null");
//	}
//
//	@Test
//	void createWithDefaults() {
//		MockitoSpyBeanMetadata definition = new MockitoSpyBeanMetadata(null, REAL_SERVICE_TYPE, null, true, null);
//		assertThat(definition.getName()).isNull();
//		assertThat(definition.getTypeToSpy()).isEqualTo(REAL_SERVICE_TYPE);
//		assertThat(definition.getReset()).isEqualTo(MockReset.AFTER);
//		assertThat(definition.isProxyTargetAware()).isTrue();
//		assertThat(definition.getQualifier()).isNull();
//	}
//
//	@Test
//	void createExplicit() {
//		QualifierMetadata qualifier = mock(QualifierMetadata.class);
//		MockitoSpyBeanMetadata definition = new MockitoSpyBeanMetadata("name", REAL_SERVICE_TYPE, MockReset.BEFORE, false, qualifier);
//		assertThat(definition.getName()).isEqualTo("name");
//		assertThat(definition.getTypeToSpy()).isEqualTo(REAL_SERVICE_TYPE);
//		assertThat(definition.getReset()).isEqualTo(MockReset.BEFORE);
//		assertThat(definition.isProxyTargetAware()).isFalse();
//		assertThat(definition.getQualifier()).isEqualTo(qualifier);
//	}
//
//	@Test
//	void createSpy() {
//		MockitoSpyBeanMetadata definition = new MockitoSpyBeanMetadata("name", REAL_SERVICE_TYPE, MockReset.BEFORE, true, null);
//		RealExampleService spy = definition.createSpy(new RealExampleService("hello"));
//		MockCreationSettings<?> settings = Mockito.mockingDetails(spy).getMockCreationSettings();
//		assertThat(spy).isInstanceOf(ExampleService.class);
//		assertThat(settings.getMockName()).hasToString("name");
//		assertThat(settings.getDefaultAnswer()).isEqualTo(Answers.CALLS_REAL_METHODS);
//		assertThat(MockReset.get(spy)).isEqualTo(MockReset.BEFORE);
//	}
//
//	@Test
//	void createSpyWhenNullInstanceShouldThrowException() {
//		MockitoSpyBeanMetadata definition = new MockitoSpyBeanMetadata("name", REAL_SERVICE_TYPE, MockReset.BEFORE, true, null);
//		assertThatIllegalArgumentException().isThrownBy(() -> definition.createSpy(null))
//			.withMessageContaining("Instance must not be null");
//	}
//
//	@Test
//	void createSpyWhenWrongInstanceShouldThrowException() {
//		MockitoSpyBeanMetadata definition = new MockitoSpyBeanMetadata("name", REAL_SERVICE_TYPE, MockReset.BEFORE, true, null);
//		assertThatIllegalArgumentException().isThrownBy(() -> definition.createSpy(new ExampleServiceCaller(null)))
//			.withMessageContaining("must be an instance of");
//	}
//
//	@Test
//	void createSpyTwice() {
//		MockitoSpyBeanMetadata definition = new MockitoSpyBeanMetadata("name", REAL_SERVICE_TYPE, MockReset.BEFORE, true, null);
//		Object instance = new RealExampleService("hello");
//		instance = definition.createSpy(instance);
//		definition.createSpy(instance);
//	}

}

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

import org.springframework.test.context.ContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@code org.springframework.test.context.bean.override.BeanOverrideContextCustomizerFactory}.
 *
 * @author Phillip Webb
 */
//FIXME
class MockitoContextCustomizerFactoryTests {
//
//	private final MockitoContextCustomizerFactory factory = new MockitoContextCustomizerFactory();
//
//	@Test
//	void getContextCustomizerWithoutAnnotationReturnsCustomizer() {
//		ContextCustomizer customizer = this.factory.createContextCustomizer(NoMockitoBeanAnnotation.class, null);
//		assertThat(customizer).isNotNull();
//	}
//
//	@Test
//	void getContextCustomizerWithAnnotationReturnsCustomizer() {
//		ContextCustomizer customizer = this.factory.createContextCustomizer(WithMockitoBeanAnnotation.class, null);
//		assertThat(customizer).isNotNull();
//	}
//
//	@Test
//	void getContextCustomizerUsesMocksAsCacheKey() {
//		ContextCustomizer customizer = this.factory.createContextCustomizer(WithMockitoBeanAnnotation.class, null);
//		assertThat(customizer).isNotNull();
//		ContextCustomizer same = this.factory.createContextCustomizer(WithSameMockitoBeanAnnotation.class, null);
//		assertThat(customizer).isNotNull();
//		ContextCustomizer different = this.factory.createContextCustomizer(WithDifferentMockitoBeanAnnotation.class, null);
//		assertThat(different).isNotNull();
//		assertThat(customizer).hasSameHashCodeAs(same);
//		assertThat(customizer.hashCode()).isNotEqualTo(different.hashCode());
//		assertThat(customizer).isEqualTo(customizer).isEqualTo(same).isNotEqualTo(different);
//	}
//
//	static class NoMockitoBeanAnnotation {
//
//	}
//
//	@MockitoBean({ Service1.class, Service2.class })
//	static class WithMockitoBeanAnnotation {
//
//	}
//
//	@MockitoBean({ Service2.class, Service1.class })
//	static class WithSameMockitoBeanAnnotation {
//
//	}
//
//	@MockitoBean({ Service1.class })
//	static class WithDifferentMockitoBeanAnnotation {
//
//	}

	interface Service1 {

	}

	interface Service2 {

	}

}

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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.ExampleServiceCaller;

/**
 * Tests for {@link MockitoContextCustomizer}.
 *
 * @author Phillip Webb
 */
//FIXME
class MockitoContextCustomizerTests {

	private static final Set<MockitoBeanMetadata> NO_DEFINITIONS = Collections.emptySet();
//
//	@Test
//	void hashCodeAndEquals() {
//		MockitoBeanMetadata d1 = createTestMockitoBeanMetadata(ExampleService.class);
//		MockitoBeanMetadata d2 = createTestMockitoBeanMetadata(ExampleServiceCaller.class);
//		MockitoContextCustomizer c1 = new MockitoContextCustomizer(NO_DEFINITIONS);
//		MockitoContextCustomizer c2 = new MockitoContextCustomizer(new LinkedHashSet<>(Arrays.asList(d1, d2)));
//		MockitoContextCustomizer c3 = new MockitoContextCustomizer(new LinkedHashSet<>(Arrays.asList(d2, d1)));
//		assertThat(c2).hasSameHashCodeAs(c3);
//		assertThat(c1).isEqualTo(c1).isNotEqualTo(c2);
//		assertThat(c2).isEqualTo(c2).isEqualTo(c3).isNotEqualTo(c1);
//	}
//
//	private MockitoBeanMetadata createTestMockitoBeanMetadata(Class<?> typeToMock) {
//		return new MockitoBeanMetadata(null, ResolvableType.forClass(typeToMock), null, null, false, null, null);
//	}

}

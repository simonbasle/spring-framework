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

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.BootstrapTestUtils;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import org.springframework.test.context.cache.DefaultContextCache;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for application context caching when using {@link MockitoBean @MockitoBean}.
 *
 * @author Andy Wilkinson
 */
class MockitoBeanContextCachingTests {

	private final DefaultContextCache contextCache = new DefaultContextCache(2);

	private final DefaultCacheAwareContextLoaderDelegate delegate = new DefaultCacheAwareContextLoaderDelegate(
			this.contextCache);

	@AfterEach
	@SuppressWarnings("unchecked")
	void clearCache() {
		Map<MergedContextConfiguration, ApplicationContext> contexts = (Map<MergedContextConfiguration, ApplicationContext>) ReflectionTestUtils
			.getField(this.contextCache, "contextMap");
		for (ApplicationContext context : contexts.values()) {
			if (context instanceof ConfigurableApplicationContext configurableContext) {
				configurableContext.close();
			}
		}
		this.contextCache.clear();
	}

	@Test
	void whenThereIsANormalBeanAndAMockitoBeanThenTwoContextsAreCreated() {
		bootstrapContext(TestClass.class);
		assertThat(this.contextCache.size()).isOne();
		bootstrapContext(MockedBeanTestClass.class);
		assertThat(this.contextCache.size()).isEqualTo(2);
	}

	@Test
	void whenThereIsTheSameMockedBeanInEachTestClassThenOneContextIsCreated() {
		bootstrapContext(MockedBeanTestClass.class);
		assertThat(this.contextCache.size()).isOne();
		bootstrapContext(AnotherMockedBeanTestClass.class);
		assertThat(this.contextCache.size()).isOne();
	}

	@SuppressWarnings("rawtypes")
	private void bootstrapContext(Class<?> testClass) {
		final BootstrapContext bootstrapper = BootstrapTestUtils.buildBootstrapContext(testClass, this.delegate);
		TestContext testContext = BootstrapTestUtils.resolveTestContextBootstrapper(bootstrapper)
				.buildTestContext();
		testContext.getApplicationContext();
	}

	@SpringJUnitConfig(TestConfiguration.class)
	static class TestClass {

	}

	@SpringJUnitConfig(TestConfiguration.class)
	static class MockedBeanTestClass {

		@MockitoBean
		private TestBean testBean;

	}

	@SpringJUnitConfig(TestConfiguration.class)
	static class AnotherMockedBeanTestClass {

		@MockitoBean
		private TestBean testBean;

	}

	@Configuration
	static class TestConfiguration {

		@Bean
		TestBean testBean() {
			return new TestBean();
		}

	}

	static class TestBean {

	}

}

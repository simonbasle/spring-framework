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

package org.springframework.test.bean.override;

import java.lang.reflect.Field;
import java.util.function.BiConsumer;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.ReflectionUtils;

/**
 * An abstract {@link TestExecutionListener} to enable Bean Override support in tests, injecting overridden
 * beans in appropriate fields.
 * <p>A single concrete {@link AbstractBeanOverrideTestExecutionListener} should be registered per application test,
 * defining the {@link #getOrder() order} and optionally adding behavior to {@link #prepareTestInstance(TestContext)}
 * and {@link #beforeTestMethod(TestContext)}.
 *
 * @author Simon Baslé
 * @since 6.2
 */
public abstract class AbstractBeanOverrideTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * Defaults to calling {@link #injectFields(TestContext)}.
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		injectFields(testContext);
	}

	/**
	 * Defaults to calling {@link #reinjectFieldsIfConfigured(TestContext)}.
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		reinjectFieldsIfConfigured(testContext);
	}

	/**
	 * Parse Bean Override fields in the test class and inject these using a
	 * registered {@link BeanOverrideBeanPostProcessor}.
	 */
	protected void injectFields(TestContext testContext) {
		postProcessFields(testContext, (testMetadata, postProcessor) -> postProcessor.inject(
				testMetadata.field(), testMetadata.testInstance(), testMetadata.overrideMetadata()));
	}

	/**
	 * Parse Bean Override fields in the test class, null them out and re-inject them
	 * using a registered {@link BeanOverrideBeanPostProcessor}, provided the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE}
	 * attribute is present in the {@code testContext}
	 * @see #reinjectFields(TestContext)
	 */
	protected void reinjectFieldsIfConfigured(final TestContext testContext) {
		if (Boolean.TRUE.equals(
				testContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE))) {
			reinjectFields(testContext);
		}
	}

	/**
	 * Parse Bean Override fields in the test class, null them out and re-inject them
	 * using a registered {@link BeanOverrideBeanPostProcessor}.
	 * @see #reinjectFieldsIfConfigured(TestContext)
	 */
	protected void reinjectFields(final TestContext testContext) {
		postProcessFields(testContext, (testMetadata, postProcessor) -> {
			Field f = testMetadata.field();
			ReflectionUtils.makeAccessible(f);
			ReflectionUtils.setField(f, testMetadata.testInstance(), null);
			postProcessor.inject(testMetadata.field(), testMetadata.testInstance(), testMetadata.overrideMetadata());
		});
	}

	private void postProcessFields(TestContext testContext, BiConsumer<TestContextOverrideMetadata, BeanOverrideBeanPostProcessor> consumer) {
		BeanOverrideParser parser = new BeanOverrideParser();
		parser.parse(testContext.getTestClass());
		if (!parser.getOverrideMetadata().isEmpty()) {
			BeanOverrideBeanPostProcessor postProcessor = testContext.getApplicationContext().getBean(BeanOverrideBeanPostProcessor.class);
			for (OverrideMetadata metadata: parser.getOverrideMetadata()) {
				if (metadata.element() instanceof Field field) {
					consumer.accept(new TestContextOverrideMetadata(testContext.getTestInstance(), field, metadata), postProcessor);
				}
			}
		}
	}

	private record TestContextOverrideMetadata(Object testInstance, Field field, OverrideMetadata overrideMetadata) {}

}

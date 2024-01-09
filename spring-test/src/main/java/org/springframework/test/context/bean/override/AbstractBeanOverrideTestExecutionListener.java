/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.Optional;
import java.util.function.BiConsumer;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.ReflectionUtils;

/**
 * A {@link TestExecutionListener} to enable Bean Override support in tests.
 */
public abstract class AbstractBeanOverrideTestExecutionListener extends AbstractTestExecutionListener {


	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		initForCleanup(testContext).ifPresent(o -> testContext.setAttribute(getCleanupAttributeName(), o));;
		injectFields(testContext);
	}

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		if (Boolean.TRUE.equals(
				testContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE))) {
			initForCleanup(testContext).ifPresent(o -> testContext.setAttribute(getCleanupAttributeName(), o));
			reinjectFields(testContext);
		}
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		Object cleanup = testContext.getAttribute(getCleanupAttributeName());
		if (cleanup instanceof AutoCloseable closeable) {
			closeable.close();
		}
	}

	//FIXME make it more pluggable... how to have a single listener yet have eg. mock specific inits/cleanup
	protected Optional<Object> initForCleanup(TestContext testContext) {
		return Optional.empty();
	}

	protected String getCleanupAttributeName() {
		return this.getClass().getSimpleName() + ".AFTER_TEST";
	}

	private void injectFields(TestContext testContext) {
		postProcessFields(testContext, (testMetadata, postProcessor) -> postProcessor.inject(
				testMetadata.field(), testMetadata.testInstance(), testMetadata.overrideMetadata()));
	}

	private void reinjectFields(final TestContext testContext) {
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

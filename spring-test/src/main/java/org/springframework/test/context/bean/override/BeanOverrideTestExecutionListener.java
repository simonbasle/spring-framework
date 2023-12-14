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
import java.util.function.BiConsumer;

import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.ReflectionUtils;

/**
 * Abstract {@link TestExecutionListener} to enable Bean Override support in tests.
 */
public abstract class BeanOverrideTestExecutionListener<D extends BeanOverrideDefinition>
		extends AbstractTestExecutionListener {

	protected abstract Class<? extends BeanOverridePostProcessor<D>> getPostProcessorClass();

	@Override
	public abstract int getOrder();

	protected abstract String getBeanOverrideAttributeName();

	@Nullable
	protected abstract Object doInitBeanOverride(TestContext testContext);

	protected abstract boolean hasRelevantAnnotations(TestContext testContext);

	protected abstract BeanOverrideDefinitionsParser<?, D> createParser();

	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		initOverrides(testContext);
		injectFields(testContext);
	}

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		if (Boolean.TRUE.equals(
				testContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE))) {
			initOverrides(testContext);
			reinjectFields(testContext);
		}
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		Object mocks = testContext.getAttribute(getBeanOverrideAttributeName());
		if (mocks instanceof AutoCloseable closeable) {
			closeable.close();
		}
	}

	private void initOverrides(TestContext testContext) {
		if (hasRelevantAnnotations(testContext)) {
			Object o = doInitBeanOverride(testContext);
			if (o != null) {
				testContext.setAttribute(getBeanOverrideAttributeName(), doInitBeanOverride(testContext));
			}
		}
	}

	private void injectFields(TestContext testContext) {
		postProcessFields(testContext, (beanOverrideField, postProcessor) -> postProcessor.inject(beanOverrideField.field,
				beanOverrideField.target, beanOverrideField.definition));
	}

	private void reinjectFields(final TestContext testContext) {
		postProcessFields(testContext, (beanOverrideField, postProcessor) -> {
			ReflectionUtils.makeAccessible(beanOverrideField.field);
			ReflectionUtils.setField(beanOverrideField.field, testContext.getTestInstance(), null);
			postProcessor.inject(beanOverrideField.field, beanOverrideField.target, beanOverrideField.definition);
		});
	}

	private void postProcessFields(TestContext testContext, BiConsumer<BeanOverrideField<D>, BeanOverridePostProcessor<D>> consumer) {
		BeanOverrideDefinitionsParser<?, D> parser = createParser();
		parser.parse(testContext.getTestClass());
		if (!parser.getDefinitions().isEmpty()) {
			BeanOverridePostProcessor<D> postProcessor = testContext.getApplicationContext().getBean(getPostProcessorClass());
			for (D definition : parser.getDefinitions()) {
				Field field = parser.getField(definition);
				if (field != null) {
					consumer.accept(new BeanOverrideField<>(field, testContext.getTestInstance(), definition), postProcessor);
				}
			}
		}
	}

	private record BeanOverrideField<D extends BeanOverrideDefinition>(Field field, Object target, D definition) {}

}

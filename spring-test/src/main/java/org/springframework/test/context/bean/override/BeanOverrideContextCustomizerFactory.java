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

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * A {@link ContextCustomizerFactory} to add support for Bean Overriding.
 */
public class BeanOverrideContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		BeanOverrideParser parser = new BeanOverrideParser();
		parseDefinitions(testClass, parser);
		return new BeanOverrideContextCustomizer(parser.getOverrideMetadata());
	}

	private void parseDefinitions(Class<?> testClass, BeanOverrideParser parser) {
		parser.parse(testClass);
		if (TestContextAnnotationUtils.searchEnclosingClass(testClass)) {
			parseDefinitions(testClass.getEnclosingClass(), parser);
		}
	}

	/**
	 * A {@link ContextCustomizer} for Bean Overriding in tests.
	 */
	static final class BeanOverrideContextCustomizer implements ContextCustomizer {

		private final Set<OverrideMetadata> metadata;

		/**
		 * @param metadata a set of concrete {@link OverrideMetadata} provided by
		 * the underlying {@link BeanOverrideParser}
		 */
		BeanOverrideContextCustomizer(Set<OverrideMetadata> metadata) {
			this.metadata = metadata;
		}

		@Override
		public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
			if (context instanceof BeanDefinitionRegistry registry) {
				BeanOverrideBeanPostProcessor.register(registry, this.metadata);
			}
		}
	}
}

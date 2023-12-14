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

package org.springframework.test.context.bean.override.concrete;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.bean.override.BeanOverrideContextCustomizerFactory;

public final class BeanoContextCustomizerFactory extends BeanOverrideContextCustomizerFactory<BeanoDefinition> {

	@Override
	protected BeanoParser doCreateParser(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
		return new BeanoParser();
	}

	@Override
	protected ContextCustomizer doCreateContextCustomizer(Set<BeanoDefinition> definitions) {
		return (context, mergedConfig) -> {
			if (context instanceof BeanDefinitionRegistry registry) {
				BeanoPostProcessor.register(registry, BeanoPostProcessor.class, BeanoPostProcessor.class.getName());
			}
		};
	}
}

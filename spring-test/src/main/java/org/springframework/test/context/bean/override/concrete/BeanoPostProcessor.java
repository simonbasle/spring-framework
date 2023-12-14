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

import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.test.context.bean.override.BeanOverrideDefinitionsParser;
import org.springframework.test.context.bean.override.BeanOverridePostProcessor;

public class BeanoPostProcessor extends BeanOverridePostProcessor<BeanoDefinition> {

	private final OverridenBeans overridenBeans;

	/**
	 * Create a new {@link BeanOverridePostProcessor} instance with the given initial
	 * definitions.
	 * @param definitions the initial definitions
	 */
	public BeanoPostProcessor(Set<BeanoDefinition> definitions) {
		super(definitions);
		this.overridenBeans = new OverridenBeans();
	}

	@Override
	protected void registerTrackerBean(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.registerSingleton(OverridenBeans.class.getName(), this.overridenBeans);
	}

	@Override
	protected Object createAndTrackOverride(String overriddenBeanName, BeanoDefinition definition) {
		Object override = definition.createOverrideInstance(overriddenBeanName);
		this.overridenBeans.add(override);
		return override;
	}

	@Override
	protected BeanOverrideDefinitionsParser<?, BeanoDefinition> createBeanDefinitionParser() {
		return new BeanoParser();
	}
}

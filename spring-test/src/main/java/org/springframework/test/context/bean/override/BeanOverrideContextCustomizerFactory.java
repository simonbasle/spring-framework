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

import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * An abstract {@link ContextCustomizerFactory} to add support for a concrete variant of Bean Override.
 */
public abstract class BeanOverrideContextCustomizerFactory<D extends BeanOverrideDefinition> implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		// We gather the explicit mock definitions here since they form part of the
		// MergedContextConfiguration key. Different mocks need to have a different key.
		BeanOverrideDefinitionsParser<?, D> parser = doCreateParser(testClass, configAttributes);
		parseDefinitions(testClass, parser);
		return doCreateContextCustomizer(parser.getDefinitions());
	}

	private void parseDefinitions(Class<?> testClass, BeanOverrideDefinitionsParser<?, D> parser) {
		parser.parse(testClass);
		if (TestContextAnnotationUtils.searchEnclosingClass(testClass)) {
			parseDefinitions(testClass.getEnclosingClass(), parser);
		}
	}

	protected abstract BeanOverrideDefinitionsParser<?,D> doCreateParser(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes);

	protected abstract ContextCustomizer doCreateContextCustomizer(Set<D> definitions);

}

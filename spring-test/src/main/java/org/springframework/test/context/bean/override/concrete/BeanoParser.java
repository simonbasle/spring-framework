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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;

import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.BeanOverrideDefinitionsParser;
import org.springframework.test.context.bean.override.QualifierDefinition;
import org.springframework.util.Assert;

final class BeanoParser extends BeanOverrideDefinitionsParser<BeanO, BeanoDefinition> {

	protected BeanoParser() {
		super();
	}

	@Override
	protected Class<BeanO> annotationClass() {
		return BeanO.class;
	}

	@Override
	protected void doParseBeanOverrideAnnotation(BeanO annotation, AnnotatedElement element, Class<?> source) {
		Assert.isInstanceOf(Method.class, element, () -> "Expected a method to override, got " + element);
		ResolvableType typeToOverride = getOrDeduceType(element, source);
		Assert.notNull(typeToOverride, () -> "Unable to deduce method return type to override from " + element);
		BeanoDefinition definition = new BeanoDefinition(annotation.name(), typeToOverride,
				QualifierDefinition.forElement(element, a -> !a.annotationType().equals(BeanO.class)),
				source, (Method) element);
		super.addDefinition(element, definition, "override");
	}


	@Nullable
	private ResolvableType getOrDeduceType(AnnotatedElement element, Class<?> source) {
		if (!(element instanceof Method method)) {
			return null;
		}
		return (method.getGenericReturnType() instanceof TypeVariable<?>)
				? ResolvableType.forMethodReturnType(method, source)
				: ResolvableType.forMethodReturnType(method);
	}
}

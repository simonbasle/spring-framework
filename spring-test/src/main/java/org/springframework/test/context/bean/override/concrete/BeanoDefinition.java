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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.test.context.bean.override.BeanOverrideDefinition;
import org.springframework.test.context.bean.override.QualifierDefinition;

public final class BeanoDefinition extends BeanOverrideDefinition {

	private final Method method;
	private final Object source;
	private final Object[] arguments;

	BeanoDefinition(String name, ResolvableType typeToOverride, @Nullable QualifierDefinition qualifier, Object source, Method method,
			Object... arguments) {
		super(name, false, typeToOverride, qualifier);
		this.source = source;
		this.method = method;
		this.arguments = arguments;
	}

	@Override
	protected Object createOverrideInstance(String beanName) {
		try {
			Object o;
			if (this.arguments == null || this.arguments.length == 0) {
				o = this.method.invoke(this.source);
			}
			else {
				o = this.method.invoke(this.source, this.arguments);
			}
			return o;
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}

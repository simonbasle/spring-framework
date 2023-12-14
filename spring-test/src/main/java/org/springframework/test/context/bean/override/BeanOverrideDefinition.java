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

import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

public abstract class BeanOverrideDefinition {

	private static final int MULTIPLIER = 31;

	private final String name;

	private final boolean proxyTargetAware;

	private final ResolvableType typeToOverride;

	@Nullable
	private final QualifierDefinition qualifier;

	protected BeanOverrideDefinition(String name, boolean proxyTargetAware, ResolvableType typeToOverride,
			@Nullable QualifierDefinition qualifier) {
		this.name = name;
		this.proxyTargetAware = proxyTargetAware;
		Assert.notNull(typeToOverride, "TypeToOverride must not be null");
		this.typeToOverride = typeToOverride;
		this.qualifier = qualifier;

	}

	/**
	 * Return the name for bean.
	 * @return the name or {@code null}
	 */
	String getName() {
		return this.name;
	}

	/**
	 * Return if AOP advised beans should be proxy target aware.
	 * @return if proxy target aware
	 */
	boolean isProxyTargetAware() {
		return this.proxyTargetAware;
	}

	/**
	 * Return the qualifier or {@code null}.
	 * @return the qualifier
	 */
	@Nullable
	QualifierDefinition getQualifier() {
		return this.qualifier;
	}

	/**
	 * Return the {@link ResolvableType} type to override.
	 * @return the resolvable type
	 */
	ResolvableType getTypeToOverride() {
		return this.typeToOverride;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || !getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		BeanOverrideDefinition other = (BeanOverrideDefinition) obj;
		boolean result = ObjectUtils.nullSafeEquals(this.name, other.name);
		result = result && ObjectUtils.nullSafeEquals(this.proxyTargetAware, other.proxyTargetAware);
		result = result && ObjectUtils.nullSafeEquals(this.qualifier, other.qualifier);
		result = result && ObjectUtils.nullSafeEquals(this.typeToOverride, other.typeToOverride);
		return result;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.name);
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.proxyTargetAware);
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.qualifier);
		result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.typeToOverride);
		return result;
	}

	protected abstract Object createOverrideInstance(String overriddenBeanName);
}

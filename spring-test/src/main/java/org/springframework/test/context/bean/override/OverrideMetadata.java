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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

public class OverrideMetadata {

	private final Field field;
	private final Annotation overrideAnnotation;
	private final ResolvableType typeToOverride;
	@Nullable
	private final QualifierMetadata qualifier;

	public OverrideMetadata(Field field, Annotation overrideAnnotation,
			ResolvableType typeToOverride, @Nullable QualifierMetadata qualifier) {
		this.field = field;
		this.overrideAnnotation = overrideAnnotation;
		this.typeToOverride = typeToOverride;
		this.qualifier = qualifier;
	}

	protected Optional<String> getBeanName() {
		return Optional.empty();
	}

	public Field field() {
		return this.field;
	}

	public Annotation overrideAnnotation() {
		return this.overrideAnnotation;
	}

	public ResolvableType typeToOverride() {
		return this.typeToOverride;
	}

	@Nullable
	public QualifierMetadata qualifier() {
		return this.qualifier;
	}

	public boolean enforceOriginalDefinition() {
		return false;
	}

	public boolean enforceOriginalSingleton() {
		return false;
	}

	protected void checkEnforcedOriginals(String transformedBeanName,
			@Nullable BeanDefinition existingBeanDefinition,
			@Nullable Object originalSingleton) {
		if (existingBeanDefinition == null && enforceOriginalDefinition()) {
			throw new BeanDefinitionValidationException("Cannot create override, mandatory original bean definition " + transformedBeanName + " not present");
		}
		if (originalSingleton == null && enforceOriginalSingleton()) {
			throw new BeanDefinitionValidationException("Cannot create override, mandatory original singleton instance " + transformedBeanName + " not present");
		}
	}

	protected Object createOverride(String transformedBeanName, @Nullable BeanDefinition existingBeanDefinition,
			@Nullable Object originalSingleton) {
		checkEnforcedOriginals(transformedBeanName, existingBeanDefinition, originalSingleton);

		if (originalSingleton != null) {
			return originalSingleton;
		}
		throw new BeanDefinitionValidationException("Cannot create default override, no originalSingleton present");
	}


	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (OverrideMetadata) obj;
		return Objects.equals(this.field, that.field) &&
				Objects.equals(this.overrideAnnotation, that.overrideAnnotation) &&
				Objects.equals(this.typeToOverride, that.typeToOverride) &&
				Objects.equals(this.qualifier, that.qualifier);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.field, this.overrideAnnotation, this.typeToOverride, this.qualifier);
	}

	@Override
	public String toString() {
		return "OverrideMetadata[" +
				"field=" + this.field + ", " +
				"overrideAnnotation=" + this.overrideAnnotation + ", " +
				"typeToOverride=" + this.typeToOverride + ", " +
				"qualifier=" + this.qualifier + ']';
	}
}

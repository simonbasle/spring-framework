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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

//FIXME harmonize getters names ? distinguish getters vs stateless methods ?
public abstract class OverrideMetadata {

	public static final OverrideMetadata IGNORE = new OverrideMetadata(BeanOverride.NoOpBeanOverrideProcessor.class.getDeclaredConstructors()[0],
			BeanOverride.class.getAnnotations()[0],
			ResolvableType.forClass(BeanOverride.class),
			null) {
		@Override
		public BeanOverrideStrategy getBeanOverrideStrategy() {
			return BeanOverrideStrategy.NO_OP;
		}

		@Override
		public String getBeanOverrideDescription() {
			return "IGNORE";
		}

		@Override
		protected Object createOverride(String transformedBeanName, @Nullable BeanDefinition existingBeanDefinition, Object originalSingleton) {
			throw new UnsupportedOperationException("OverrideMetadata.IGNORE should not be used to create overrides");
		}
	};

	private static final Consumer<Object> NO_OP = o -> {};

	private final AnnotatedElement element;
	private final Annotation overrideAnnotation;
	private final ResolvableType typeToOverride;
	@Nullable
	private final QualifierMetadata qualifier;

	public OverrideMetadata(AnnotatedElement element, Annotation overrideAnnotation,
			ResolvableType typeToOverride, @Nullable QualifierMetadata qualifier) {
		this.element = element;
		this.overrideAnnotation = overrideAnnotation;
		this.typeToOverride = typeToOverride;
		this.qualifier = qualifier;
	}

	public abstract BeanOverrideStrategy getBeanOverrideStrategy();

	public abstract String getBeanOverrideDescription();

	protected Optional<String> getBeanName() {
		return Optional.empty();
	}

	protected Consumer<Object> getOrCreateTracker(ConfigurableListableBeanFactory beanFactory) {
		return NO_OP;
	}

	public AnnotatedElement element() {
		return this.element;
	}

	@Nullable
	public Field fieldElement() {
		return this.element instanceof  Field f ? f : null;
	}

	@Nullable
	public Method methodElement() {
		return this.element instanceof Method m ? m : null;
	}

	@Nullable
	public Class<?> classElement() {
		return this.element instanceof Class<?> c ? c : null;
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
			throw new BeanDefinitionValidationException("Cannot create " + getBeanOverrideDescription() + " override, mandatory original bean definition " + transformedBeanName + " not present");
		}
		if (originalSingleton == null && enforceOriginalSingleton()) {
			throw new BeanDefinitionValidationException("Cannot create " + getBeanOverrideDescription() + " override, mandatory original singleton instance " + transformedBeanName + " not present");
		}
	}

	protected Object createOverride(String transformedBeanName, @Nullable BeanDefinition existingBeanDefinition,
			@Nullable Object originalSingleton) {
		checkEnforcedOriginals(transformedBeanName, existingBeanDefinition, originalSingleton);

		if (originalSingleton != null) {
			return originalSingleton;
		}
		throw new BeanDefinitionValidationException("Cannot create " + getBeanOverrideDescription() + " override, no originalSingleton present");
	}


	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || !getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		var that = (OverrideMetadata) obj;
		return Objects.equals(this.element, that.element) &&
				Objects.equals(this.overrideAnnotation, that.overrideAnnotation) &&
				Objects.equals(this.typeToOverride, that.typeToOverride) &&
				Objects.equals(this.qualifier, that.qualifier);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.element, this.overrideAnnotation, this.typeToOverride, this.qualifier);
	}

	@Override
	public String toString() {
		return "OverrideMetadata[" +
				"category=" + this.getBeanOverrideDescription() + ", " +
				"element=" + this.element + ", " +
				"overrideAnnotation=" + this.overrideAnnotation + ", " +
				"typeToOverride=" + this.typeToOverride + ", " +
				"qualifier=" + this.qualifier + ']';
	}
}

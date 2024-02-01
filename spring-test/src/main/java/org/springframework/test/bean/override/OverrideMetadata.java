/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.bean.override;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * Metadata for Bean Overrides.
 *
 * @author Simon Baslé
 * @since 6.2
 */
public abstract class OverrideMetadata {

	/**
	 * Default tracking {@link Consumer}, which is NO-OP.
	 * @see #getOrCreateTracker(ConfigurableListableBeanFactory)
	 */
	public static final Consumer<Object> NO_TRACKING = o -> {};

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

	/**
	 * Define the broad {@link BeanOverrideStrategy} for this {@link OverrideMetadata},
	 * as a hint on how and when the override instance should be created.
	 */
	public abstract BeanOverrideStrategy getBeanOverrideStrategy();

	/**
	 * Define a short human-readable description of the kind of override this
	 * OverrideMetadata is about. This is especially useful for {@link BeanOverrideProcessor}
	 * that produce several subtypes of metadata (e.g. "mock" vs "spy").
	 */
	public abstract String getBeanOverrideDescription();

	/**
	 * Provide an explicit bean name to override, if relevant. Typically, this is useful
	 * for concrete annotations that allow the user to provide the bean name.
	 * @return an {@link Optional} bean name, or {@link Optional#empty()} if it should be inferred
	 */
	protected Optional<String> getExplicitBeanName() {
		return Optional.empty();
	}

	/**
	 * Returns an optional tracking {@link Consumer} for objects created by this OverrideMetadata.
	 * Defaults to the {@link #NO_TRACKING} NO-OP implementation.
	 * @param beanFactory the bean factory in which trackers could optionally be registered
	 * @return the tracking {@link Consumer} to use, or {@link #NO_TRACKING} if irrelevant
	 */
	protected Consumer<Object> getOrCreateTracker(ConfigurableListableBeanFactory beanFactory) {
		return NO_TRACKING;
	}

	/**
	 * The element annotated with a {@link BeanOverride}-compatible annotation.
	 * <p>Typically a {@link Field}, but implementations could also accept {@link Method}
	 * or {@link Class}.
	 * @return the annotated element
	 * @see #fieldElement()
	 */
	public AnnotatedElement element() {
		return this.element;
	}

	/**
	 * Convenience method to get the {@link #element annotated element} as a {@link Field}.
	 * @return the annotated element as a {@link Field}, or {@code null} if not a Field
	 */
	@Nullable
	public Field fieldElement() {
		return this.element instanceof  Field f ? f : null;
	}

	/**
	 * Convenience method to get the {@link #element annotated element} as a {@link Method}.
	 * @return the annotated element as a {@link Method}, or {@code null} if not a Method
	 */
	@Nullable
	public Method methodElement() {
		return this.element instanceof Method m ? m : null;
	}

	/**
	 * Convenience method to get the {@link #element annotated element} as a {@link Class}.
	 * @return the annotated element as a {@link Class}, or {@code null} if not a Class
	 */
	@Nullable
	public Class<?> classElement() {
		return this.element instanceof Class<?> c ? c : null;
	}

	/**
	 * @return the concrete override annotation, i.e. the one meta-annotated with {@link BeanOverride}
	 */
	public Annotation overrideAnnotation() {
		return this.overrideAnnotation;
	}

	/**
	 * @return the type to override, as a {@link ResolvableType}
	 */
	public ResolvableType typeToOverride() {
		return this.typeToOverride;
	}

	/**
	 * @return an optional {@link QualifierMetadata}, or {@code null} if not relevant
	 */
	@Nullable
	public QualifierMetadata qualifier() {
		return this.qualifier;
	}

	/**
	 * Create an override instance from this {@link OverrideMetadata}, optionally provided
	 * with an existing {@link BeanDefinition} and/or an original instance (i.e. a singleton
	 * or an early wrapped instance).
	 * @param beanName the name of the bean being overridden
	 * @param existingBeanDefinition an existing bean definition for that bean name, or
	 * {@code null} if not relevant
	 * @param existingBeanInstance an existing instance for that bean name, for wrapping
	 * purpose, or {@code null} if irrelevant
	 * @return the instance with which to override the bean
	 */
	protected abstract Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition,
			@Nullable Object existingBeanInstance);

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

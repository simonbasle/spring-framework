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

package org.springframework.test.context.bean.override;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;

/**
 * Metadata for qualifying annotations on Bean Override injection points, as
 * represented by a parent {@link OverrideMetadata}. This represents relevant
 * annotations on the injection point and can be used to logically compare such
 * injection points independently of the declared class or actual field.
 *
 * <p><strong>WARNING</strong>: implementations are used as a context cache key
 * and must implement proper {@code equals()} and {@code hashCode()} methods.
 * They should generally not take the {@code OverrideMetadata} associated bean
 * override annotations into account, which is facilitated by the
 * {@link #forField(Field, Class[])} factory method.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Simon Baslé
 * @since 6.2
 * @see OverrideMetadata
 */
public class QualifierMetadata {

	private final DependencyDescriptor descriptor;

	private final Set<Annotation> annotations;


	protected QualifierMetadata(Field f, Set<Annotation> annotations) {
		// We can't use the field or descriptor as part of the context key
		// but we can assume that if two fields have the same qualifiers then
		// it's safe for Spring to use either for qualifier logic
		this.descriptor = new DependencyDescriptor(f, true);
		this.annotations = annotations;
	}

	/**
	 * Get the {@code DependencyDescriptor} for the {@code Field} this metadata
	 * was created from.
	 */
	protected DependencyDescriptor getDescriptor() {
		return this.descriptor;
	}

	/**
	 * Get the set of {@code Annotation} this QualifierMetadata considers as
	 * qualifying.
	 */
	protected Set<Annotation> getQualifyingAnnotations() {
		return this.annotations;
	}

	/**
	 * Modify the provided RootBeanDefinition according to this metadata.
	 * By default, sets the underlying {@link #getDescriptor() descriptor's}
	 * field as the {@link RootBeanDefinition#setQualifiedElement(AnnotatedElement)
	 * qualified element} of the bean definition, unless {@link #getQualifyingAnnotations()}
	 * is empty.
	 */
	protected void applyToDefinition(RootBeanDefinition rootBeanDefinition) {
		if (!this.annotations.isEmpty()) {
			rootBeanDefinition.setQualifiedElement(getDescriptor().getField());
		}
	}

	/**
	 * Check if this {@link QualifierMetadata} matches a given bean in the
	 * provided {@code beanFactory}, making the bean a candidate for overriding.
	 * <p>Defaults to checking {@link ConfigurableListableBeanFactory#isAutowireCandidate(String, DependencyDescriptor)}
	 * using the underlying {@link #getDescriptor()}.
	 */
	protected boolean matchesInFactory(String beanName, ConfigurableListableBeanFactory beanFactory) {
		return beanFactory.isAutowireCandidate(beanName, getDescriptor());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final QualifierMetadata that = (QualifierMetadata) obj;
		return Objects.equals(this.annotations, that.annotations);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.annotations);
	}

	@Override
	public String toString() {
		return this.annotations.toString();
	}


	/**
	 * Create a {@code QualifierMetadata} for the provided {@code Field},
	 * gathering information about annotations on the field for the purpose of
	 * bean overriding. Any {@code excludedAnnotations} either directly present
	 * or meta-present on the field is not taken into account.
	 * <p>Typically, excluded annotations would be the concrete
	 * {@link BeanOverride @BeanOverride} annotation(s) the parent
	 * {@link OverrideMetadata} is associated with.
	 */
	public static QualifierMetadata forField(Field field, Class<?>... excludedAnnotations) {
		Annotation[] candidates = field.getDeclaredAnnotations();
		Set<Class<?>> excludedSet = new HashSet<>(Arrays.asList(excludedAnnotations));

		Set<Annotation> annotations = new HashSet<>(candidates.length);
		for (Annotation candidate : candidates) {
			boolean isDirectOrMetaExcluded = excludedSet.contains(candidate.annotationType()) ||
					MergedAnnotations.from(candidate).stream().anyMatch(ma ->
							excludedSet.contains(ma.getType()));

			if (!isDirectOrMetaExcluded) {
				annotations.add(candidate);
			}
		}
		return new QualifierMetadata(field, annotations);
	}

	/**
	 * Create a {@code QualifierMetadata} for the provided {@code Field},
	 * gathering information about annotations on the field for the purpose
	 * of bean overriding.
	 * <p>The {@code isExcludedAnnotation} predicate can be used as an
	 * alternative to enumerating annotation classes to exclude.
	 * @see #forField(Field, Class[])
	 */
	public static QualifierMetadata forField(Field field, @Nullable Predicate<Class<? extends Annotation>>
			isExcludedAnnotation) {

		Annotation[] candidates = field.getDeclaredAnnotations();
		Set<Annotation> annotations = new HashSet<>(candidates.length);
		for (Annotation candidate : candidates) {
			if (isExcludedAnnotation == null || !isExcludedAnnotation.test(candidate.annotationType())) {
				annotations.add(candidate);
			}
		}
		return new QualifierMetadata(field, annotations);
	}

}

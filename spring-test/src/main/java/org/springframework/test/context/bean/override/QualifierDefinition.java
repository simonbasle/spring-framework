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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Definition of a Spring {@link Qualifier @Qualifier}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see BeanOverrideDefinition
 */
public class QualifierDefinition {

	private final Field field;

	private final DependencyDescriptor descriptor;

	private final Set<Annotation> annotations;

	QualifierDefinition(Field field, Set<Annotation> annotations) {
		// We can't use the field or descriptor as part of the context key
		// but we can assume that if two fields have the same qualifiers then
		// it's safe for Spring to use either for qualifier logic
		this.field = field;
		this.descriptor = new DependencyDescriptor(field, true);
		this.annotations = annotations;
	}

	boolean matches(ConfigurableListableBeanFactory beanFactory, String beanName) {
		return beanFactory.isAutowireCandidate(beanName, this.descriptor);
	}

	void applyTo(RootBeanDefinition definition) {
		definition.setQualifiedElement(this.field);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || !getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		QualifierDefinition other = (QualifierDefinition) obj;
		return this.annotations.equals(other.annotations);
	}

	@Override
	public int hashCode() {
		return this.annotations.hashCode();
	}

	public static QualifierDefinition forElement(AnnotatedElement element, Predicate<? super Annotation> isQualifierPredicate) {
		if (element != null && element instanceof Field field) {
			Set<Annotation> annotations = getQualifierAnnotations(field, isQualifierPredicate);
			if (!annotations.isEmpty()) {
				return new QualifierDefinition(field, annotations);
			}
		}
		return null;
	}

	private static Set<Annotation> getQualifierAnnotations(Field field, Predicate<? super Annotation> isQualifierPredicate) {
		// Assume that any annotations other than @MockBean/@SpyBean are qualifiers
		Annotation[] candidates = field.getDeclaredAnnotations();
		Set<Annotation> annotations = new HashSet<>(candidates.length);
		for (Annotation candidate : candidates) {
			if (isQualifierPredicate.test(candidate)) {
				annotations.add(candidate);
			}
		}
		return annotations;
	}

}

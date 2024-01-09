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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Simple {@link BeanOverrideProcessor} primarily made to work with the {@link TestBean} annotation
 * but can work with arbitrary override annotations provided the annotated class has
 * a static method which name follows the convention {@code <beanName> + } {@link TestBean#CONVENTION_SUFFIX}.
 */
public class SimpleBeanOverrideProcessor implements BeanOverrideProcessor {

	static final class MethodConventionOverrideMetadata extends OverrideMetadata {

		@Nullable
		private final Method overrideMethod;

		public MethodConventionOverrideMetadata(Field field, @Nullable Method overrideMethod,
				Annotation overrideAnnotation, ResolvableType typeToOverride, @Nullable QualifierMetadata qualifier) {
			super(field, overrideAnnotation, typeToOverride, qualifier);
			this.overrideMethod = overrideMethod;
		}

		@NonNull
		@Override
		public Field fieldElement() {
			return (Field) super.element();
		}

		@Override
		public BeanOverrideStrategy getBeanOverrideStrategy() {
			return BeanOverrideStrategy.REPLACE_DEFINITION;
		}

		@Override
		public String getBeanOverrideDescription() {
			return "method convention";
		}

		@Override
		protected Object createOverride(String transformedBeanName, @Nullable BeanDefinition existingBeanDefinition,
				@Nullable Object originalSingleton) {

			Method methodToInvoke = this.overrideMethod;
			if (methodToInvoke == null) {
				methodToInvoke = ensureMethod(fieldElement().getDeclaringClass(), transformedBeanName + TestBean.CONVENTION_SUFFIX,
						fieldElement().getType());
			}

			methodToInvoke.setAccessible(true);
			Object override;
			try {
				override = methodToInvoke.invoke(null);
			}
			catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e); //TODO better error handling, notably indicate that we expect a 0-param static method
			}

			return override;
		}
	}

	@Override
	public Set<ResolvableType> getOrDeduceTypes(AnnotatedElement element, Annotation annotation, Class<?> source) {
		return Set.of();
	}

	@Override
	public boolean isQualifierAnnotation(Annotation annotation) {
		return !(annotation instanceof TestBean) && additionalIsQualifierCheck(annotation);
	}

	/**
	 * Additional check to extend which annotations are considered as qualifier annotations.
	 * Not the {@link TestBean} annotation is never considered as a qualifier.
	 * Defaults to considering all annotations.
	 */
	protected boolean additionalIsQualifierCheck(Annotation annotation) {
		return true;
	}

	public static Method ensureMethod(Class<?> enclosingClass, String expectedMethodName, Class<?> expectedMethodReturnType) {
		Method overrideMethod = ClassUtils.getStaticMethod(enclosingClass, expectedMethodName);
		Assert.notNull(overrideMethod, "Expected static method " + expectedMethodName + " on class " + enclosingClass.getName());
		Assert.state(expectedMethodReturnType.isAssignableFrom(overrideMethod.getReturnType()),
				"Method " + expectedMethodName + " on class " + enclosingClass.getName() + " doesn't return a "
			+ expectedMethodReturnType.getName());
		return overrideMethod;
	}

	@Override
	public OverrideMetadata createMetadata(AnnotatedElement element, BeanOverride syntheticAnnotation, ResolvableType typeToOverride,
			@Nullable QualifierMetadata qualifier) {
		if (!(element instanceof Field field)) {
			throw new UnsupportedOperationException("SimpleBeanOverrideProcessor can only process annotated Fields, got: " + element);
		}

		final Class<?> enclosingClass = field.getDeclaringClass();

		// if we can get an explicit method name right away, fail false if it doesn't match
		if (syntheticAnnotation instanceof TestBean testBeanAnnotation) {
			String annotationMethodName = testBeanAnnotation.methodName();
			if (!annotationMethodName.isBlank()) {
				Method overrideMethod = ensureMethod(enclosingClass, annotationMethodName, field.getType());
				return new MethodConventionOverrideMetadata(field, overrideMethod, syntheticAnnotation, typeToOverride, qualifier);
			}
		}
		// otherwise defer the resolution of the static method until OverrideMetadata#createOverride
		return new MethodConventionOverrideMetadata(field, null, syntheticAnnotation, typeToOverride, qualifier);
	}
}

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
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

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
		protected Object createOverride(String beanName, @Nullable BeanDefinition existingBeanDefinition,
				@Nullable Object existingBeanInstance) {
			Method methodToInvoke = this.overrideMethod;
			if (methodToInvoke == null) {
				methodToInvoke = ensureMethod(fieldElement().getDeclaringClass(), fieldElement().getType(),
						beanName + TestBean.CONVENTION_SUFFIX, fieldElement().getName() + TestBean.CONVENTION_SUFFIX);
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

	public static Method ensureMethod(Class<?> enclosingClass, Class<?> expectedMethodReturnType, String... expectedMethodNames) {
		Set<String> expectedNames = new HashSet<>(Arrays.asList(expectedMethodNames));
		return Arrays.stream(enclosingClass.getDeclaredMethods())
				.filter(m -> Modifier.isStatic(m.getModifiers()))
				.filter(m -> expectedNames.contains(m.getName()) && expectedMethodReturnType.isAssignableFrom(m.getReturnType()))
				.findFirst().orElseThrow(() -> new IllegalStateException("Expected one static method of " + expectedNames + " on class " + enclosingClass.getName()
				+ " with return type "+ expectedMethodReturnType.getName()));
	}

	@Override
	public OverrideMetadata createMetadata(AnnotatedElement element, Annotation overrideAnnotation, ResolvableType typeToOverride,
			@Nullable QualifierMetadata qualifier) {
		if (!(element instanceof Field field)) {
			throw new UnsupportedOperationException("SimpleBeanOverrideProcessor can only process annotated Fields, got: " + element);
		}

		final Class<?> enclosingClass = field.getDeclaringClass();

		// if we can get an explicit method name right away, fail false if it doesn't match
		if (overrideAnnotation instanceof TestBean testBeanAnnotation) {
			String annotationMethodName = testBeanAnnotation.methodName();
			if (!annotationMethodName.isBlank()) {
				Method overrideMethod = ensureMethod(enclosingClass, field.getType(), annotationMethodName);
				return new MethodConventionOverrideMetadata(field, overrideMethod, overrideAnnotation, typeToOverride, qualifier);
			}
		}
		// otherwise defer the resolution of the static method until OverrideMetadata#createOverride
		return new MethodConventionOverrideMetadata(field, null, overrideAnnotation, typeToOverride, qualifier);
	}
}

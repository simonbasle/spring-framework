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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Simple {@link BeanOverrideProcessor} primarily made to work with the {@link TestBean} annotation
 * but can work with arbitrary override annotations provided the annotated class has
 * a relevant method according to the convention documented in {@link TestBean}.
 *
 * @author Simon Baslé
 * @since 6.2
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

	/**
	 * Ensures the {@code enclosingClass} has a static, no-arguments method with the
	 * provided {@code expectedMethodReturnType} and exactly one of the {@code expectedMethodNames}.
	 */
	public static Method ensureMethod(Class<?> enclosingClass, Class<?> expectedMethodReturnType, String... expectedMethodNames) {
		Assert.isTrue(expectedMethodNames.length > 0, "At least one expectedMethodName is required");
		Set<String> expectedNames = new LinkedHashSet<>(Arrays.asList(expectedMethodNames));
		final List<Method> found = Arrays.stream(enclosingClass.getDeclaredMethods())
				.filter(m -> Modifier.isStatic(m.getModifiers()))
				.filter(m -> expectedNames.contains(m.getName()) && expectedMethodReturnType.isAssignableFrom(m.getReturnType()))
				.collect(Collectors.toList());

		Assert.state(found.size() == 1, () -> "Found " + found.size() + " static methods instead of exactly one, " +
						"matching a name in " + expectedNames + " with return type " + expectedMethodReturnType.getName() +
						" on class " + enclosingClass.getName());

		return found.get(0);
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

	@Override
	public List<OverrideMetadata> createMetadata(AnnotatedElement element, Annotation overrideAnnotation,
			Set<ResolvableType> typesToOverride, @Nullable QualifierMetadata qualifier) {
		if (!(element instanceof Field field)) {
			throw new IllegalArgumentException("SimpleBeanOverrideProcessor can only process annotated Fields, got a " + element.getClass().getSimpleName());
		}
		List<OverrideMetadata> result = new ArrayList<>(typesToOverride.size());

		final Class<?> enclosingClass = field.getDeclaringClass();
		// if we can get an explicit method name right away, fail fast if it doesn't match
		if (overrideAnnotation instanceof TestBean testBeanAnnotation) {
			String annotationMethodName = testBeanAnnotation.methodName();
			if (!annotationMethodName.isBlank()) {
				Method overrideMethod = ensureMethod(enclosingClass, field.getType(), annotationMethodName);
				for (ResolvableType typeToOverride : typesToOverride) {
					result.add(new MethodConventionOverrideMetadata(field, overrideMethod, overrideAnnotation, typeToOverride, qualifier));
				}
				return result;
			}
		}
		// otherwise defer the resolution of the static method until OverrideMetadata#createOverride
		for (ResolvableType typeToOverride : typesToOverride) {
			result.add(new MethodConventionOverrideMetadata(field, null, overrideAnnotation, typeToOverride, qualifier));
		}
		return result;
	}
}

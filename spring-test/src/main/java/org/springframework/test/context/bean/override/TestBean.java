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

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Mark a field to represent a "method" bean override of the bean of the same name and
 * inject the field with the overriding instance.
 * <p>The instance is created from a static method in the declaring class which return
 * type is compatible with the annotated field and which name follows the convention
 * {@code fieldName + }{@link #CONVENTION_SUFFIX}, unless the annotation's {@link #methodName()}
 * is specified.
 * <p>The annotated field's name is interpreted to be the name of the original bean to
 * override, unless the annotation's {@link #beanName()} is specified.
 * @see SimpleBeanOverrideProcessor
 */
@Target(ElementType.FIELD)
@BeanOverride(processor = SimpleBeanOverrideProcessor.class)
public @interface TestBean {

	String CONVENTION_SUFFIX = "TestOverride";

	/**
	 * The name of a static method to look for in the Configuration, which will be
	 * used to instantiate the override bean and inject the annotated field.
	 * <p> Default is {@code ""} (the empty String), which is translated into
	 * the annotated field's name concatenated with the {@link #CONVENTION_SUFFIX}.
	 */
	String methodName() default "";

	/**
	 * The name of the original bean to override, or {@code ""} (the empty String)
	 * to deduce the name from the annotated field.
	 */
	String beanName() default "";
}

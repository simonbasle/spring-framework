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

package org.springframework.test.bean.override.mockito;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.mockito.Answers;

import org.springframework.test.bean.override.BeanOverride;

/**
 * Mark a field to trigger the override of the bean of the same name with a
 * Mockito mock. In order to ensure mocks are set up and reset correctly,
 * the test class must itself be annotated with
 * {@link MockitoBeanOverrideTestListeners}.
 *
 * @author Simon Baslé
 * @since 6.2
 */
@Retention(RetentionPolicy.RUNTIME)
@BeanOverride(processor = MockitoBeanOverrideProcessor.class)
public @interface MockitoBean {
	String name() default "";
	boolean serializable() default false;
	MockReset reset() default MockReset.AFTER;
	Class<?>[] extraInterfaces() default {};
	Answers answers() default Answers.RETURNS_DEFAULTS;
}

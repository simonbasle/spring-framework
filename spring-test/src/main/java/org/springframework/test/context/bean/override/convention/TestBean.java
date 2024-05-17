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

package org.springframework.test.context.bean.override.convention;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.bean.override.BeanOverride;

/**
 * Mark a field to override a bean definition in the {@code BeanFactory}.
 *
 * <p>By default, the bean to override is inferred from the type of the
 * annotated field. This requires that exactly one matching definition is
 * present in the application context. A {@code @Qualifier} annotation can be
 * used to help disambiguate. Alternatively, you can explicitly specify a bean
 * name to replace by setting the {@link #value()} or {@link #name()} attribute.
 *
 * <p>The instance is created from a zero-argument static factory method in the
 * test class whose return type is compatible with the annotated field. In the
 * case of a nested test, any enclosing class it might have is also considered.
 * Similarly, in case the test class inherits from a base class the whole class
 * hierarchy is considered. The method is deduced as follows.
 * <ul>
 * <li>If the {@link #methodName()} is specified, look for a static method with
 * that name.</li>
 * <li>If a method name is not specified, look for exactly one static method named
 * with a suffix equal to {@value #CONVENTION_SUFFIX} and starting with either the
 * name of the annotated field or the name of the bean (if specified).</li>
 * </ul>
 *
 * <p>Consider the following example.
 *
 * <pre><code>
 * class CustomerServiceTests {
 *
 *     &#064;TestBean
 *     private CustomerRepository repository;
 *
 *     // Tests
 *
 *     private static CustomerRepository repositoryTestOverride() {
 *         return new TestCustomerRepository();
 *     }
 * }</code></pre>
 *
 * <p>In the example above, the {@code repository} bean is replaced by the
 * instance generated by the {@code repositoryTestOverride()} method. Not only
 * is the overridden instance injected into the {@code repository} field, but it
 * is also replaced in the {@code BeanFactory} so that other injection points
 * for that bean use the overridden bean instance.
 *
 * <p>To make things more explicit, the bean and method names can be set,
 * as shown in the following example.
 *
 * <pre><code>
 * class CustomerServiceTests {
 *
 *     &#064;TestBean(name = "repository", methodName = "createTestCustomerRepository")
 *     private CustomerRepository repository;
 *
 *     // Tests
 *
 *     private static CustomerRepository createTestCustomerRepository() {
 *         return new TestCustomerRepository();
 *     }
 * }</code></pre>
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 * @see TestBeanOverrideProcessor
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@BeanOverride(TestBeanOverrideProcessor.class)
public @interface TestBean {

	/**
	 * Required suffix for the name of a factory method that overrides a bean
	 * instance when the factory method is detected by convention.
	 * @see #methodName()
	 */
	String CONVENTION_SUFFIX = "TestOverride";


	/**
	 * Alias for {@link #name()}.
	 * <p>Intended to be used when no other attributes are needed &mdash; for
	 * example, {@code @TestBean("customBeanName")}.
	 * @see #name()
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * Name of the bean to override.
	 * <p>If left unspecified, the bean to override is selected according to
	 * the annotated field's type.
	 * @see #value()
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * Name of a static factory method to look for in the test class, which will
	 * be used to instantiate the bean to override.
	 * <p>In the case of a nested test, any enclosing class it might have is
	 * also considered. Similarly, in case the test class inherits from a base
	 * class the whole class hierarchy is considered.
	 * <p>If left unspecified, the name of the factory method will be detected
	 * based on convention.
	 * @see #CONVENTION_SUFFIX
	 */
	String methodName() default "";

}

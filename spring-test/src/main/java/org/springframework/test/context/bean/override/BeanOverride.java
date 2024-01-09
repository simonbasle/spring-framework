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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AliasFor;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface BeanOverride {

	@AliasFor("processors")
	Class<? extends BeanOverrideProcessor> value() default NoOpBeanOverrideProcessor.class;

	@AliasFor("value")
	Class<? extends BeanOverrideProcessor> processor() default NoOpBeanOverrideProcessor.class;


	final class NoOpBeanOverrideProcessor implements BeanOverrideProcessor {

		@Override
		public OverrideMetadata createMetadata(AnnotatedElement element, BeanOverride syntheticAnnotation, ResolvableType typeToOverride, QualifierMetadata qualifier) {
			return OverrideMetadata.IGNORE;
		}
	}
}

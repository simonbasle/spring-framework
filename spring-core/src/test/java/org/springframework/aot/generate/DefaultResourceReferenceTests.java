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

package org.springframework.aot.generate;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.javapoet.CodeBlock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultResourceReference}.
 *
 * @author Stephane Nicoll
 */
class DefaultResourceReferenceTests {

	@Test
	void toResourceInstantiation() {
		String path = "com/example/my-file.txt";
		DefaultResourceReference resourceReference = new DefaultResourceReference(path);
		assertThat(resourceReference.toVariableInitialization())
				.isEqualTo(CodeBlock.of("new $L($S)", ClassPathResource.class.getName(), path));
	}


}

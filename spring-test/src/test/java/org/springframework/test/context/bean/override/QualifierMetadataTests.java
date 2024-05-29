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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link QualifierMetadata}.
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
class QualifierMetadataTests {

	@Mock
	private ConfigurableListableBeanFactory beanFactory;

	@Test
	void forElementWhenElementIsFieldWithNoQualifiersShouldReturnEmpty() {
		QualifierMetadata definition = QualifierMetadata
			.forField(ReflectionUtils.findField(ConfigA.class, "noQualifier"),
			MockitoBean.class);
		assertThat(definition.getQualifyingAnnotations()).isEmpty();
	}

	@Test
	void forElementWhenElementIsFieldWithQualifierShouldReturnDefinition() {
		QualifierMetadata definition = QualifierMetadata
			.forField(ReflectionUtils.findField(ConfigA.class, "directQualifier"),
					MockitoBean.class);
		assertThat(definition).isNotNull();
		assertThat(definition.getQualifyingAnnotations()).hasSize(1);
	}

	@Test
	void matchesShouldCallBeanFactory() {
		Field field = ReflectionUtils.findField(ConfigA.class, "directQualifier");
		QualifierMetadata qualifierMetadata = QualifierMetadata.forField(field,
				MockitoBean.class);
		qualifierMetadata.matchesInFactory("bean", this.beanFactory);
		then(this.beanFactory).should()
			.isAutowireCandidate(eq("bean"), assertArg(
					(dependencyDescriptor) -> assertThat(dependencyDescriptor.getAnnotatedElement()).isEqualTo(field)));
	}

	@Test
	void applyToShouldSetQualifierElement() {
		Field field = ReflectionUtils.findField(ConfigA.class, "directQualifier");
		QualifierMetadata qualifierMetadata = QualifierMetadata.forField(field,
				MockitoBean.class);
		RootBeanDefinition definition = new RootBeanDefinition();
		qualifierMetadata.applyToDefinition(definition);
		assertThat(definition.getQualifiedElement()).isEqualTo(field);
	}

	@Test
	void hashCodeAndEqualsShouldWorkOnDifferentClasses() {
		QualifierMetadata directQualifier1 = QualifierMetadata
			.forField(ReflectionUtils.findField(ConfigA.class, "directQualifier"),
					MockitoBean.class);
		QualifierMetadata directQualifier2 = QualifierMetadata
			.forField(ReflectionUtils.findField(ConfigB.class, "directQualifier"),
					MockitoBean.class);
		QualifierMetadata differentDirectQualifier1 = QualifierMetadata
			.forField(ReflectionUtils.findField(ConfigA.class, "differentDirectQualifier"),
					MockitoBean.class);
		QualifierMetadata differentDirectQualifier2 = QualifierMetadata
			.forField(ReflectionUtils.findField(ConfigB.class, "differentDirectQualifier"),
					MockitoBean.class);
		QualifierMetadata customQualifier1 = QualifierMetadata
			.forField(ReflectionUtils.findField(ConfigA.class, "customQualifier"),
					MockitoBean.class);
		QualifierMetadata customQualifier2 = QualifierMetadata
			.forField(ReflectionUtils.findField(ConfigB.class, "customQualifier"),
					MockitoBean.class);
		assertThat(directQualifier1).hasSameHashCodeAs(directQualifier2);
		assertThat(differentDirectQualifier1).hasSameHashCodeAs(differentDirectQualifier2);
		assertThat(customQualifier1).hasSameHashCodeAs(customQualifier2);
		assertThat(differentDirectQualifier1).isEqualTo(differentDirectQualifier1)
			.isEqualTo(differentDirectQualifier2)
			.isNotEqualTo(directQualifier2);
		assertThat(directQualifier1).isEqualTo(directQualifier1)
			.isEqualTo(directQualifier2)
			.isNotEqualTo(differentDirectQualifier1);
		assertThat(customQualifier1).isEqualTo(customQualifier1)
			.isEqualTo(customQualifier2)
			.isNotEqualTo(differentDirectQualifier1);
	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigA {

		@MockitoBean
		private Object noQualifier;

		@MockitoBean
		@Qualifier("test")
		private Object directQualifier;

		@MockitoBean
		@Qualifier("different")
		private Object differentDirectQualifier;

		@MockitoBean
		@CustomQualifier
		private Object customQualifier;

	}

	static class ConfigB {

		@MockitoBean
		@Qualifier("test")
		private Object directQualifier;

		@MockitoBean
		@Qualifier("different")
		private Object differentDirectQualifier;

		@MockitoBean
		@CustomQualifier
		private Object customQualifier;

	}

	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	public @interface CustomQualifier {

	}

}

/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.FailingExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.bean.override.example.TestBeanOverrideAnnotation;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test for {@link BeanOverrideBeanPostProcessor}.
 *
 * @author Simon Baslé
 */
class BeanOverrideBeanPostProcessorTests {

	@Test
	void cannotOverrideMultipleBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		BeanOverrideBeanPostProcessor.register(context);
		context.register(MultipleBeans.class);
		assertThatIllegalStateException().isThrownBy(context::refresh)
			.withMessageContaining("Unable to register test bean " + ExampleService.class.getName()
					+ " expected a single matching bean to replace but found [example1, example2]");
	}

	@Test
	void cannotOverrideMultipleQualifiedBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		BeanOverrideBeanPostProcessor.register(context);
		context.register(MultipleQualifiedBeans.class);
		assertThatIllegalStateException().isThrownBy(context::refresh)
			.withMessageContaining("Unable to register test bean " + ExampleService.class.getName()
					+ " expected a single matching bean to replace but found [example1, example3]");
	}

	@Test
	void canOverrideBeanProducedByFactoryBeanWithClassObjectTypeAttribute() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		BeanOverrideBeanPostProcessor.register(context);
		RootBeanDefinition factoryBeanDefinition = new RootBeanDefinition(TestFactoryBean.class);
		factoryBeanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, SomeInterface.class);
		context.registerBeanDefinition("beanToBeOverridden", factoryBeanDefinition);
		context.register(OverriddenFactoryBean.class);
		context.refresh();
		assertThat(context.getBean("beanToBeOverridden")).isSameAs(OVERRIDE);
	}

	@Test
	void canOverrideBeanProducedByFactoryBeanWithResolvableTypeObjectTypeAttribute() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		BeanOverrideBeanPostProcessor.register(context);
		RootBeanDefinition factoryBeanDefinition = new RootBeanDefinition(TestFactoryBean.class);
		ResolvableType objectType = ResolvableType.forClass(SomeInterface.class);
		factoryBeanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, objectType);
		context.registerBeanDefinition("beanToBeOverridden", factoryBeanDefinition);
		context.register(OverriddenFactoryBean.class);
		context.refresh();
		assertThat(context.getBean("beanToBeOverridden")).isSameAs(OVERRIDE);
	}

	@Test
	void canOverridePrimaryBean() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		BeanOverrideBeanPostProcessor.register(context);
		context.register(OverridePrimaryBean.class);
		context.refresh();
		assertThat(context.getBean(OverridePrimaryBean.class).field).as("field").isSameAs(OVERRIDE_SERVICE);
		assertThat(context.getBean(ExampleService.class)).as("bean by class").isSameAs(OVERRIDE_SERVICE);
		assertThat(context.getBean("examplePrimary", ExampleService.class)).as("primary").isSameAs(OVERRIDE_SERVICE);
		assertThat(context.getBean("exampleQualified", ExampleService.class)).as("qualified").isNotSameAs(OVERRIDE_SERVICE);
	}

	@Test
	void canOverrideQualifiedBeanWithPrimaryBeanPresent() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		BeanOverrideBeanPostProcessor.register(context);
		context.register(OverrideQualifiedBean.class);
		context.refresh();
		assertThat(context.getBean(OverrideQualifiedBean.class).field).as("field").isSameAs(OVERRIDE_SERVICE);
		assertThat(context.getBean(ExampleService.class)).as("bean by class").isNotSameAs(OVERRIDE_SERVICE);
		assertThat(context.getBean("examplePrimary", ExampleService.class)).as("primary").isNotSameAs(OVERRIDE_SERVICE);
		assertThat(context.getBean("exampleQualified", ExampleService.class)).as("qualified").isSameAs(OVERRIDE_SERVICE);
	}

	@Test
	void postProcessorShouldNotTriggerEarlyInitialization() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(FactoryBeanRegisteringPostProcessor.class);
		BeanOverrideBeanPostProcessor.register(context);
		context.register(TestBeanFactoryPostProcessor.class);
		context.register(EagerInitBean.class);
		context.refresh();
	}

	static final SomeInterface OVERRIDE = new SomeImplementation();
	static final ExampleService OVERRIDE_SERVICE = new FailingExampleService();


	@Configuration(proxyBeanMethods = false)
	static class OverriddenFactoryBean {

		@TestBeanOverrideAnnotation("fOverride")
		SomeInterface f;

		static SomeInterface fOverride() {
			return OVERRIDE;
		}

		@Bean
		TestFactoryBean testFactoryBean() {
			return new TestFactoryBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleBeans {

		@TestBeanOverrideAnnotation
		ExampleService service;

		@Bean
		ExampleService example1() {
			return new FailingExampleService();
		}

		@Bean
		ExampleService example2() {
			return new FailingExampleService();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleQualifiedBeans {

		@TestBeanOverrideAnnotation
		@Qualifier("test")
		private ExampleService field;

		@Bean
		@Qualifier("test")
		ExampleService example1() {
			return new FailingExampleService();
		}

		@Bean
		ExampleService example2() {
			return new FailingExampleService();
		}

		@Bean
		@Qualifier("test")
		ExampleService example3() {
			return new FailingExampleService();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OverridePrimaryBean {

		@TestBeanOverrideAnnotation("useThis")
		private ExampleService field;

		@Bean
		@Qualifier("test")
		ExampleService exampleQualified() {
			return new RealExampleService("qualified");
		}

		@Bean
		@Primary
		ExampleService examplePrimary() {
			return new RealExampleService("primary");
		}

		static ExampleService useThis() {
			return OVERRIDE_SERVICE;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OverrideQualifiedBean {

		@TestBeanOverrideAnnotation("useThis")
		@Qualifier("test")
		private ExampleService field;

		@Bean
		@Qualifier("test")
		ExampleService exampleQualified() {
			return new RealExampleService("qualified");
		}

		@Bean
		@Primary
		ExampleService examplePrimary() {
			return new RealExampleService("primary");
		}

		static ExampleService useThis() {
			return OVERRIDE_SERVICE;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class EagerInitBean {

		@TestBeanOverrideAnnotation("useThis")
		private ExampleService service;

		static ExampleService useThis() {
			return OVERRIDE_SERVICE;
		}

	}

	static class TestFactoryBean implements FactoryBean<Object> {

		@Override
		public Object getObject() {
			return new SomeImplementation();
		}

		@Override
		public Class<?> getObjectType() {
			return null;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

	}

	static class FactoryBeanRegisteringPostProcessor implements BeanFactoryPostProcessor, Ordered {

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
			RootBeanDefinition beanDefinition = new RootBeanDefinition(TestFactoryBean.class);
			((BeanDefinitionRegistry) beanFactory).registerBeanDefinition("test", beanDefinition);
		}

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

	}

	static class TestBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

		@Override
		@SuppressWarnings("unchecked")
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
			Map<String, BeanWrapper> cache = (Map<String, BeanWrapper>) ReflectionTestUtils.getField(beanFactory,
					"factoryBeanInstanceCache");
			Assert.isTrue(cache.isEmpty(), "Early initialization of factory bean triggered.");
		}

	}

	interface SomeInterface {

	}

	static class SomeImplementation implements SomeInterface {

	}

}

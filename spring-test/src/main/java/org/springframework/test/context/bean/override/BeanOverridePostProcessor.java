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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.Conventions;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * An abstract {@link BeanFactoryPostProcessor} used to register and inject
 * {@link BeanOverrideDefinition} with the {@link ApplicationContext}. An initial set of
 * definitions can be passed to the processor with additional definitions being
 * automatically created from {@code @Configuration} classes that use a concrete
 * annotation to mark override sites. The ultimate instances of overridden beans
 * can be created externally (e.g. delegating to a mocking framework) and injected
 * in associated {@link Field fields}, or can be computed from user code (e.g. when
 * annotating a {@link Method method}).
 * class supports field injection in the case the parser triggers
 * <p>
 * Concrete implementations of this class will need to be associated to concrete
 * implementations of {@link BeanOverrideDefinition}, annotation and
 * {@link BeanOverrideDefinitionsParser}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Andreas Neiser
 * @author Simon Baslé
 * @since 6.2.0
 */
public abstract class BeanOverridePostProcessor<D extends BeanOverrideDefinition> implements InstantiationAwareBeanPostProcessor, BeanClassLoaderAware,
		BeanFactoryAware, BeanFactoryPostProcessor, Ordered {

	private static final String CONFIGURATION_CLASS_ATTRIBUTE = Conventions
		.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "configurationClass");

	private static final BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();

	private final Set<D> definitions;

	private ClassLoader classLoader;

	private BeanFactory beanFactory;

	private final Map<D, String> beanNameRegistry = new HashMap<>();

	private final Map<Field, String> fieldRegistry = new HashMap<>();

	/**
	 * Create a new {@link BeanOverridePostProcessor} instance with the given initial
	 * definitions.
	 * @param definitions the initial definitions
	 */
	public BeanOverridePostProcessor(Set<D> definitions) {
		this.definitions = definitions;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
				"Beans overriding can only be used with a ConfigurableListableBeanFactory");
		this.beanFactory = beanFactory;
	}

	protected Set<D> getDefinitions() {
		return this.definitions;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(BeanDefinitionRegistry.class, beanFactory,
				"Bean overriding annotations can only be used on bean factories that implement BeanDefinitionRegistry");
		postProcessBeanFactory(beanFactory, (BeanDefinitionRegistry) beanFactory);
	}

	protected abstract void registerTrackerBean(ConfigurableListableBeanFactory beanFactory);
//			beanFactory.registerSingleton(MockitoBeans.class.getName(), this.mockitoBeans);

	protected abstract BeanOverrideDefinitionsParser<?, D> createBeanDefinitionParser();

	protected abstract Object createAndTrackOverride(String overriddenBeanName, D definition);
		//		this.mockitoBeans.add(override);
//		return definition.createOverride(overriddenBeanName);

	protected void copyBeanDefinitionDetails(BeanDefinition from, RootBeanDefinition to) {
		to.setPrimary(from.isPrimary());
	}

	private void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry) {
		registerTrackerBean(beanFactory);
		BeanOverrideDefinitionsParser<?, D> parser = createBeanDefinitionParser();
		for (Class<?> configurationClass : getConfigurationClasses(beanFactory)) {
			parser.parse(configurationClass);
		}
		Set<D> definitions = parser.getDefinitions();
		for (D definition : definitions) {
			//field can be null, which means we don't need to track the associated Field e.g. for injection
			Field field = parser.getField(definition);
			register(beanFactory, registry, definition, field);
		}
	}

	private Set<Class<?>> getConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {
		Set<Class<?>> configurationClasses = new LinkedHashSet<>();
		for (BeanDefinition beanDefinition : getConfigurationBeanDefinitions(beanFactory).values()) {
			configurationClasses.add(ClassUtils.resolveClassName(beanDefinition.getBeanClassName(), this.classLoader));
		}
		return configurationClasses;
	}

	private Map<String, BeanDefinition> getConfigurationBeanDefinitions(ConfigurableListableBeanFactory beanFactory) {
		Map<String, BeanDefinition> definitions = new LinkedHashMap<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
			if (definition.getAttribute(CONFIGURATION_CLASS_ATTRIBUTE) != null) {
				definitions.put(beanName, definition);
			}
		}
		return definitions;
	}

	private void register(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
			D definition, @Nullable Field field) {
		RootBeanDefinition beanDefinition = createBeanDefinition(definition);
		String beanName = getBeanName(beanFactory, registry, definition, beanDefinition);
		String transformedBeanName = BeanFactoryUtils.transformedBeanName(beanName);
		if (registry.containsBeanDefinition(transformedBeanName)) {
			BeanDefinition existing = registry.getBeanDefinition(transformedBeanName);
			copyBeanDefinitionDetails(existing, beanDefinition);
			registry.removeBeanDefinition(transformedBeanName);
		}
		registry.registerBeanDefinition(transformedBeanName, beanDefinition);
		Object override = createAndTrackOverride(beanName, definition);
		beanFactory.registerSingleton(transformedBeanName, override);

		this.beanNameRegistry.put(definition, beanName);
		if (field != null) {
			this.fieldRegistry.put(field, beanName);
		}
	}

	private RootBeanDefinition createBeanDefinition(D overrideDefinition) {
		RootBeanDefinition definition = new RootBeanDefinition(overrideDefinition.getTypeToOverride().resolve());
		definition.setTargetType(overrideDefinition.getTypeToOverride());
		if (overrideDefinition.getQualifier() != null) {
			overrideDefinition.getQualifier().applyTo(definition);
		}
		return definition;
	}

	private String getBeanName(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
			D overrideDefinition, RootBeanDefinition beanDefinition) {
		if (StringUtils.hasLength(overrideDefinition.getName())) {
			return overrideDefinition.getName();
		}
		Set<String> existingBeans = getExistingBeans(beanFactory, overrideDefinition.getTypeToOverride(),
				overrideDefinition.getQualifier());
		if (existingBeans.isEmpty()) {
			return BeanOverridePostProcessor.beanNameGenerator.generateBeanName(beanDefinition, registry);
		}
		if (existingBeans.size() == 1) {
			return existingBeans.iterator().next();
		}
		String primaryCandidate = determinePrimaryCandidate(registry, existingBeans, overrideDefinition.getTypeToOverride());
		if (primaryCandidate != null) {
			return primaryCandidate;
		}
		throw new IllegalStateException("Unable to register mock bean " + overrideDefinition.getTypeToOverride()
				+ " expected a single matching bean to replace but found " + existingBeans);
	}


	private Set<String> getExistingBeans(ConfigurableListableBeanFactory beanFactory, ResolvableType type,
			@Nullable QualifierDefinition qualifier) {
		Set<String> candidates = new TreeSet<>();
		for (String candidate : getExistingBeans(beanFactory, type)) {
			if (qualifier == null || qualifier.matches(beanFactory, candidate)) {
				candidates.add(candidate);
			}
		}
		return candidates;
	}

	private Set<String> getExistingBeans(ConfigurableListableBeanFactory beanFactory, ResolvableType resolvableType) {
		Set<String> beans = new LinkedHashSet<>(
				Arrays.asList(beanFactory.getBeanNamesForType(resolvableType, true, false)));
		Class<?> type = resolvableType.resolve(Object.class);
		for (String beanName : beanFactory.getBeanNamesForType(FactoryBean.class, true, false)) {
			beanName = BeanFactoryUtils.transformedBeanName(beanName);
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			Object attribute = beanDefinition.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
			if (resolvableType.equals(attribute) || type.equals(attribute)) {
				beans.add(beanName);
			}
		}
		beans.removeIf(this::isScopedTarget);
		return beans;
	}

	private boolean isScopedTarget(String beanName) {
		try {
			return ScopedProxyUtils.isScopedTarget(beanName);
		}
		catch (Throwable ex) {
			return false;
		}
	}

	@Nullable
	private String determinePrimaryCandidate(BeanDefinitionRegistry registry, Collection<String> candidateBeanNames,
			ResolvableType type) {
		String primaryBeanName = null;
		for (String candidateBeanName : candidateBeanNames) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(candidateBeanName);
			if (beanDefinition.isPrimary()) {
				if (primaryBeanName != null) {
					throw new NoUniqueBeanDefinitionException(type.resolve(), candidateBeanNames.size(),
							"more than one 'primary' bean found among candidates: "
									+ Collections.singletonList(candidateBeanNames));
				}
				primaryBeanName = candidateBeanName;
			}
		}
		return primaryBeanName;
	}

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
			throws BeansException {
		ReflectionUtils.doWithFields(bean.getClass(), (field) -> postProcessField(bean, field));
		return pvs;
	}

	private void postProcessField(Object bean, Field field) {
		String beanName = this.fieldRegistry.get(field);
		if (StringUtils.hasText(beanName)) {
			inject(field, bean, beanName);
		}
	}

	void inject(Field field, Object target, D definition) {
		String beanName = this.beanNameRegistry.get(definition);
		Assert.state(StringUtils.hasLength(beanName), () -> "No bean found for definition " + definition);
		inject(field, target, beanName);
	}

	private void inject(Field field, Object target, String beanName) {
		try {
			field.setAccessible(true);
			Object existingValue = ReflectionUtils.getField(field, target);
			Object bean = this.beanFactory.getBean(beanName, field.getType());
			if (existingValue == bean) {
				return;
			}
			Assert.state(existingValue == null, () -> "The existing value '" + existingValue + "' of field '" + field
					+ "' is not the same as the new value '" + bean + "'");
			ReflectionUtils.setField(field, target, bean);
		}
		catch (Throwable ex) {
			throw new BeanCreationException("Could not inject field: " + field, ex);
		}
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 10;
	}

	/**
	 * Register the processor with a {@link BeanDefinitionRegistry}. Not required when
	 * using the {@link SpringRunner} as registration is automatic.
	 * @param registry the bean definition registry
	 * @param postProcessorClass the class of {@link BeanOverridePostProcessor} to register
	 * @param postProcessorBeanName the name of the infrastructure bean to register
	 * (typically, the processor class {@link Class#getName() getName()})
	 */
	public static <D extends BeanOverrideDefinition> void register(BeanDefinitionRegistry registry,
			Class<? extends BeanOverridePostProcessor<D>> postProcessorClass, String postProcessorBeanName) {
		register(registry, postProcessorClass, postProcessorBeanName, null);
	}

	/**
	 * Register the processor with a {@link BeanDefinitionRegistry}. Not required when
	 * using the {@link SpringRunner} as registration is automatic.
	 * @param registry the bean definition registry
	 * @param postProcessorClass the class of {@link BeanOverridePostProcessor} to register
	 * @param postProcessorBeanName the name of the infrastructure bean to register
	 * @param definitions the initial override definitions
	 */
	public static <D extends BeanOverrideDefinition> void register(BeanDefinitionRegistry registry,
			Class<? extends BeanOverridePostProcessor<D>> postProcessorClass,
			String postProcessorBeanName, @Nullable Set<D> definitions) {
		BeanDefinition definition = getOrAddBeanDefinition(registry, postProcessorClass, postProcessorBeanName);
		ValueHolder constructorArg = definition.getConstructorArgumentValues().getIndexedArgumentValue(0, Set.class);
		@SuppressWarnings("unchecked")
		Set<D> existing = (Set<D>) constructorArg.getValue();
		if (definitions != null) {
			existing.addAll(definitions);
		}
	}

	private static <D1 extends BeanOverrideDefinition> BeanDefinition getOrAddBeanDefinition(BeanDefinitionRegistry registry,
			Class<? extends BeanOverridePostProcessor<D1>> postProcessorClass,
			String postProcessorBeanName) {
		if (!registry.containsBeanDefinition(postProcessorBeanName)) {
			RootBeanDefinition definition = new RootBeanDefinition(postProcessorClass);
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			ConstructorArgumentValues constructorArguments = definition.getConstructorArgumentValues();
			constructorArguments.addIndexedArgumentValue(0, new LinkedHashSet<D1>());
			registry.registerBeanDefinition(postProcessorBeanName, definition);
			return definition;
		}
		return registry.getBeanDefinition(postProcessorBeanName);
	}

}

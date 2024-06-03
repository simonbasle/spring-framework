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

package org.springframework.test.context.bean.override.mockito;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.bean.override.example.ExampleExtraInterface;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.ExampleServiceCaller;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@code org.springframework.test.context.bean.override.BeanOverrideParsingUtils}
 * in the context of Mockito.
 *
 * @author Phillip Webb
 */
//FIXME
class DefinitionsParserTests {
//
//	private final DefinitionsParser parser = new DefinitionsParser();
//
//	@Test
//	void parseSingleMockitoBean() {
//		this.parser.parse(SingleMockitoBean.class);
//		assertThat(getDefinitions()).hasSize(1);
//		assertThat(getMockitoBeanMetadata(0).getTypeToMock().resolve()).isEqualTo(ExampleService.class);
//	}
//
//	@Test
//	void parseRepeatMockitoBean() {
//		this.parser.parse(RepeatMockitoBean.class);
//		assertThat(getDefinitions()).hasSize(2);
//		assertThat(getMockitoBeanMetadata(0).getTypeToMock().resolve()).isEqualTo(ExampleService.class);
//		assertThat(getMockitoBeanMetadata(1).getTypeToMock().resolve()).isEqualTo(ExampleServiceCaller.class);
//	}
//
//	@Test
//	void parseMockitoBeanAttributes() {
//		this.parser.parse(MockitoBeanAttributes.class);
//		assertThat(getDefinitions()).hasSize(1);
//		MockitoBeanMetadata definition = getMockitoBeanMetadata(0);
//		assertThat(definition.getName()).isEqualTo("Name");
//		assertThat(definition.getTypeToMock().resolve()).isEqualTo(ExampleService.class);
//		assertThat(definition.getExtraInterfaces()).containsExactly(ExampleExtraInterface.class);
//		assertThat(definition.getAnswer()).isEqualTo(Answers.RETURNS_SMART_NULLS);
//		assertThat(definition.isSerializable()).isTrue();
//		assertThat(definition.getReset()).isEqualTo(MockReset.NONE);
//		assertThat(definition.getQualifier()).isNull();
//	}
//
//	@Test
//	void parseMockitoBeanOnClassAndField() {
//		this.parser.parse(MockitoBeanOnClassAndField.class);
//		assertThat(getDefinitions()).hasSize(2);
//		MockitoBeanMetadata classDefinition = getMockitoBeanMetadata(0);
//		assertThat(classDefinition.getTypeToMock().resolve()).isEqualTo(ExampleService.class);
//		assertThat(classDefinition.getQualifier()).isNull();
//		MockitoBeanMetadata fieldDefinition = getMockitoBeanMetadata(1);
//		assertThat(fieldDefinition.getTypeToMock().resolve()).isEqualTo(ExampleServiceCaller.class);
//		QualifierMetadata qualifier = QualifierMetadata
//			.forElement(ReflectionUtils.findField(MockitoBeanOnClassAndField.class, "caller"));
//		assertThat(fieldDefinition.getQualifier()).isNotNull().isEqualTo(qualifier);
//	}
//
//	@Test
//	void parseMockitoBeanInferClassToMock() {
//		this.parser.parse(MockitoBeanInferClassToMock.class);
//		assertThat(getDefinitions()).hasSize(1);
//		assertThat(getMockitoBeanMetadata(0).getTypeToMock().resolve()).isEqualTo(ExampleService.class);
//	}
//
//	@Test
//	void parseMockitoBeanMissingClassToMock() {
//		assertThatIllegalStateException().isThrownBy(() -> this.parser.parse(MockitoBeanMissingClassToMock.class))
//			.withMessageContaining("Unable to deduce type to mock");
//	}
//
//	@Test
//	void parseMockitoBeanMultipleClasses() {
//		this.parser.parse(MockitoBeanMultipleClasses.class);
//		assertThat(getDefinitions()).hasSize(2);
//		assertThat(getMockitoBeanMetadata(0).getTypeToMock().resolve()).isEqualTo(ExampleService.class);
//		assertThat(getMockitoBeanMetadata(1).getTypeToMock().resolve()).isEqualTo(ExampleServiceCaller.class);
//	}
//
//	@Test
//	void parseMockitoBeanMultipleClassesWithName() {
//		assertThatIllegalStateException().isThrownBy(() -> this.parser.parse(MockitoBeanMultipleClassesWithName.class))
//			.withMessageContaining("The name attribute can only be used when mocking a single class");
//	}
//
//	@Test
//	void parseSingleSpyBean() {
//		this.parser.parse(SingleSpyBean.class);
//		assertThat(getDefinitions()).hasSize(1);
//		assertThat(getMockitoSpyBeanMetadata(0).getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
//	}
//
//	@Test
//	void parseRepeatSpyBean() {
//		this.parser.parse(RepeatSpyBean.class);
//		assertThat(getDefinitions()).hasSize(2);
//		assertThat(getMockitoSpyBeanMetadata(0).getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
//		assertThat(getMockitoSpyBeanMetadata(1).getTypeToSpy().resolve()).isEqualTo(ExampleServiceCaller.class);
//	}
//
//	@Test
//	void parseSpyBeanAttributes() {
//		this.parser.parse(SpyBeanAttributes.class);
//		assertThat(getDefinitions()).hasSize(1);
//		MockitoSpyBeanMetadata definition = getMockitoSpyBeanMetadata(0);
//		assertThat(definition.getName()).isEqualTo("Name");
//		assertThat(definition.getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
//		assertThat(definition.getReset()).isEqualTo(MockReset.NONE);
//		assertThat(definition.getQualifier()).isNull();
//	}
//
//	@Test
//	void parseSpyBeanOnClassAndField() {
//		this.parser.parse(SpyBeanOnClassAndField.class);
//		assertThat(getDefinitions()).hasSize(2);
//		MockitoSpyBeanMetadata classDefinition = getMockitoSpyBeanMetadata(0);
//		assertThat(classDefinition.getQualifier()).isNull();
//		assertThat(classDefinition.getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
//		MockitoSpyBeanMetadata fieldDefinition = getMockitoSpyBeanMetadata(1);
//		QualifierMetadata qualifier = QualifierMetadata
//			.forElement(ReflectionUtils.findField(SpyBeanOnClassAndField.class, "caller"));
//		assertThat(fieldDefinition.getQualifier()).isNotNull().isEqualTo(qualifier);
//		assertThat(fieldDefinition.getTypeToSpy().resolve()).isEqualTo(ExampleServiceCaller.class);
//	}
//
//	@Test
//	void parseSpyBeanInferClassToMock() {
//		this.parser.parse(SpyBeanInferClassToMock.class);
//		assertThat(getDefinitions()).hasSize(1);
//		assertThat(getMockitoSpyBeanMetadata(0).getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
//	}
//
//	@Test
//	void parseSpyBeanMissingClassToMock() {
//		assertThatIllegalStateException().isThrownBy(() -> this.parser.parse(SpyBeanMissingClassToMock.class))
//			.withMessageContaining("Unable to deduce type to spy");
//	}
//
//	@Test
//	void parseSpyBeanMultipleClasses() {
//		this.parser.parse(SpyBeanMultipleClasses.class);
//		assertThat(getDefinitions()).hasSize(2);
//		assertThat(getMockitoSpyBeanMetadata(0).getTypeToSpy().resolve()).isEqualTo(RealExampleService.class);
//		assertThat(getMockitoSpyBeanMetadata(1).getTypeToSpy().resolve()).isEqualTo(ExampleServiceCaller.class);
//	}
//
//	@Test
//	void parseSpyBeanMultipleClassesWithName() {
//		assertThatIllegalStateException().isThrownBy(() -> this.parser.parse(SpyBeanMultipleClassesWithName.class))
//			.withMessageContaining("The name attribute can only be used when spying a single class");
//	}
//
//	private MockitoBeanMetadata getMockitoBeanMetadata(int index) {
//		return (MockitoBeanMetadata) getDefinitions().get(index);
//	}
//
//	private MockitoSpyBeanMetadata getMockitoSpyBeanMetadata(int index) {
//		return (MockitoSpyBeanMetadata) getDefinitions().get(index);
//	}
//
//	private List<Definition> getDefinitions() {
//		return new ArrayList<>(this.parser.getDefinitions());
//	}
//
//	@MockitoBean(ExampleService.class)
//	static class SingleMockitoBean {
//
//	}
//
//	@MockitoBeans({ @MockitoBean(ExampleService.class), @MockitoBean(ExampleServiceCaller.class) })
//	static class RepeatMockitoBean {
//
//	}
//
//	@MockitoBean(name = "Name", classes = ExampleService.class, extraInterfaces = ExampleExtraInterface.class,
//			answer = Answers.RETURNS_SMART_NULLS, serializable = true, reset = MockReset.NONE)
//	static class MockitoBeanAttributes {
//
//	}
//
//	@MockitoBean(ExampleService.class)
//	static class MockitoBeanOnClassAndField {
//
//		@MockitoBean(ExampleServiceCaller.class)
//		@Qualifier("test")
//		private Object caller;
//
//	}
//
//	@MockitoBean({ ExampleService.class, ExampleServiceCaller.class })
//	static class MockitoBeanMultipleClasses {
//
//	}
//
//	@MockitoBean(name = "name", classes = { ExampleService.class, ExampleServiceCaller.class })
//	static class MockitoBeanMultipleClassesWithName {
//
//	}
//
//	static class MockitoBeanInferClassToMock {
//
//		@MockitoBean
//		private ExampleService exampleService;
//
//	}
//
//	@MockitoBean
//	static class MockitoBeanMissingClassToMock {
//
//	}
//
//	@MockitoSpyBean(RealExampleService.class)
//	static class SingleSpyBean {
//
//	}
//
//	@MockitoSpyBeans({ @MockitoSpyBean(RealExampleService.class), @MockitoSpyBean(ExampleServiceCaller.class) })
//	static class RepeatSpyBean {
//
//	}
//
//	@MockitoSpyBean(name = "Name", classes = RealExampleService.class, reset = MockReset.NONE)
//	static class MockitoSpyBeanAttributes {
//
//	}
//
//	@MockitoSpyBean(RealExampleService.class)
//	static class MockitoSpyBeanOnClassAndField {
//
//		@MockitoSpyBean(ExampleServiceCaller.class)
//		@Qualifier("test")
//		private Object caller;
//
//	}
//
//	@MockitoSpyBean({ RealExampleService.class, ExampleServiceCaller.class })
//	static class MockitoSpyBeanMultipleClasses {
//
//	}
//
//	@MockitoSpyBean(name = "name", classes = { RealExampleService.class, ExampleServiceCaller.class })
//	static class MockitoSpyBeanMultipleClassesWithName {
//
//	}
//
//	static class MockitoSpyBeanInferClassToMock {
//
//		@MockitoSpyBean
//		private RealExampleService exampleService;
//
//	}
//
//	@MockitoSpyBean
//	static class MockitoSpyBeanMissingClassToMock {
//
//	}

}

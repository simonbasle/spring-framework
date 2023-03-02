/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

import org.springframework.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link YamlProcessor}.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Brian Clozel
 */
class YamlProcessorTests {

	private final YamlProcessor processor = new YamlProcessor() {
	};


	@Test
	void addFullListsDefaultsToFalse() {
		assertThat(this.processor.isIncludeSimpleLists()).isFalse();
	}

	@Test
	void arrayConvertedToIndexedBeanReference() {
		setYaml("foo: bar\nbar: [1,2,3]");
		this.processor.process((properties, map) -> {
			assertThat(properties).hasSize(4);
			assertThat(properties.get("foo")).isEqualTo("bar");
			assertThat(properties.getProperty("foo")).isEqualTo("bar");
			assertThat(properties.get("bar[0]")).isEqualTo(1);
			assertThat(properties.getProperty("bar[0]")).isEqualTo("1");
			assertThat(properties.get("bar[1]")).isEqualTo(2);
			assertThat(properties.getProperty("bar[1]")).isEqualTo("2");
			assertThat(properties.get("bar[2]")).isEqualTo(3);
			assertThat(properties.getProperty("bar[2]")).isEqualTo("3");
			assertThat(properties).doesNotContainKey("bar");
		});
	}

	//gh-16381
	@Test
	void arrayConvertedToIndexedKeysAndFullList() {
		setYaml("""
			animals:
			  mammals:
			   - cat
			   - dog
			   - horse
			  unicorns: []
			""");
		this.processor.setIncludeSimpleLists(true);

		this.processor.process((properties, map) -> {
			assertThat(properties)
					.doesNotContainKey("animals")
					.containsOnlyKeys("animals.mammals",
							"animals.mammals[0]",
							"animals.mammals[1]",
							"animals.mammals[2]",
							"animals.unicorns");
			assertThat(properties.getProperty("animals.mammals[0]")).isEqualTo("cat");
			assertThat(properties.getProperty("animals.mammals[1]")).isEqualTo("dog");
			assertThat(properties.getProperty("animals.mammals[2]")).isEqualTo("horse");

			@SuppressWarnings("unchecked") List<Object> allMammals = (List<Object>) properties.get("animals.mammals");
			assertThat(allMammals).isNotNull().containsExactly("cat", "dog", "horse");
			assertThat(properties.getProperty("animals.mammals")).as("full list String form")
					.isEqualTo("[cat, dog, horse]");

			@SuppressWarnings("unchecked") List<Object> unicorns = (List<Object>) properties.get("animals.unicorns");
			assertThat(unicorns).isNotNull().isEmpty();
			assertThat(properties.getProperty("animals.unicorns")).as("empty List in String form")
					.isEqualTo("[]");
		});
	}

	@Test
	void arrayWithNestedMapsNotIncludingFullLists() {
		//here we test that fullList isn't effected if the list contains a Map
		setYaml("""
			all:
			  animals:
			   - name: cat
			     type: mammal
			   - name: dragon
			     type: imaginary
			""");
		this.processor.setIncludeSimpleLists(true);

		this.processor.process((properties, map) -> {
			assertThat(properties)
					.doesNotContainKeys("all", "all.animals")
					.hasSize(4)
					.containsEntry("all.animals[0].name", "cat")
					.containsEntry("all.animals[0].type", "mammal")
					.containsEntry("all.animals[1].name", "dragon")
					.containsEntry("all.animals[1].type", "imaginary");
		});
	}

	@Test
	void arrayWithNestedListsNotIncludingFullLists() {
		//here we test that fullList isn't effected if the list contains a Collection
		setYaml("""
			foo:
			  bar:
			    - 1
			    - [2,3,4]
			    - [cat,dog]
			""");
		this.processor.setIncludeSimpleLists(true);

		this.processor.process((properties, map) -> {
			assertThat(properties).hasSize(8);
			assertThat(properties)
					.doesNotContainKeys("foo", "foo.bar")
					.containsOnlyKeys(
							"foo.bar[0]",
							"foo.bar[1][0]",
							"foo.bar[1][1]",
							"foo.bar[1][2]",
							"foo.bar[2][0]",
							"foo.bar[2][1]",
							//full lists only for the last level of nesting
							"foo.bar[1]",
							"foo.bar[2]");
			assertThat(properties.get("foo.bar[0]")).isEqualTo(1);
			assertThat(properties.getProperty("foo.bar[0]")).isEqualTo("1");
			assertThat(properties.get("foo.bar[1][0]")).isEqualTo(2);
			assertThat(properties.getProperty("foo.bar[1][0]")).isEqualTo("2");
			assertThat(properties.get("foo.bar[1][2]")).isEqualTo(4);
			assertThat(properties.getProperty("foo.bar[1][2]")).isEqualTo("4");
			assertThat(properties.get("foo.bar[2][0]")).isEqualTo("cat");
			assertThat(properties.getProperty("foo.bar[2][0]")).isEqualTo("cat");

			@SuppressWarnings("unchecked") List<Object> numbers = (List<Object>) properties.get("foo.bar[1]");
			assertThat(numbers).isNotNull().containsExactly(2, 3, 4);
			assertThat(properties.getProperty("foo.bar[1]")).isEqualTo("[2, 3, 4]");

			@SuppressWarnings("unchecked") List<Object> animals = (List<Object>) properties.get("foo.bar[2]");
			assertThat(animals).isNotNull().containsExactly("cat", "dog");
			assertThat(properties.getProperty("foo.bar[2]")).isEqualTo("[cat, dog]");
		});
	}

	@Test
	void stringResource() {
		setYaml("foo # a document that is a literal");
		this.processor.process((properties, map) -> assertThat(map.get("document")).isEqualTo("foo"));
	}

	@Test
	void badDocumentStart() {
		setYaml("foo # a document\nbar: baz");
		assertThatExceptionOfType(ParserException.class)
			.isThrownBy(() -> this.processor.process((properties, map) -> {}))
			.withMessageContaining("line 2, column 1");
	}

	@Test
	void badResource() {
		setYaml("foo: bar\ncd\nspam:\n  foo: baz");
		assertThatExceptionOfType(ScannerException.class)
				.isThrownBy(() -> this.processor.process((properties, map) -> {}))
				.withMessageContaining("line 3, column 1");
	}

	@Test
	void mapConvertedToIndexedBeanReference() {
		setYaml("foo: bar\nbar:\n spam: bucket");
		this.processor.process((properties, map) -> {
			assertThat(properties.get("bar.spam")).isEqualTo("bucket");
			assertThat(properties).hasSize(2);
		});
	}

	@Test
	void integerKeyBehaves() {
		setYaml("foo: bar\n1: bar");
		this.processor.process((properties, map) -> {
			assertThat(properties.get("[1]")).isEqualTo("bar");
			assertThat(properties).hasSize(2);
		});
	}

	@Test
	void integerDeepKeyBehaves() {
		setYaml("foo:\n  1: bar");
		this.processor.process((properties, map) -> {
			assertThat(properties.get("foo[1]")).isEqualTo("bar");
			assertThat(properties).hasSize(1);
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void flattenedMapIsSameAsPropertiesButOrdered() {
		setYaml("cat: dog\nfoo: bar\nbar:\n spam: bucket");
		this.processor.process((properties, map) -> {
			Map<String, Object> flattenedMap = processor.getFlattenedMap(map);
			assertThat(flattenedMap).isInstanceOf(LinkedHashMap.class);

			assertThat(properties).hasSize(3);
			assertThat(flattenedMap).hasSize(3);

			assertThat(properties.get("bar.spam")).isEqualTo("bucket");
			assertThat(flattenedMap.get("bar.spam")).isEqualTo("bucket");

			Map<String, Object> bar = (Map<String, Object>) map.get("bar");
			assertThat(bar.get("spam")).isEqualTo("bucket");

			List<Object> keysFromProperties = new ArrayList<>(properties.keySet());
			List<String> keysFromFlattenedMap = new ArrayList<>(flattenedMap.keySet());
			assertThat(keysFromProperties).containsExactlyInAnyOrderElementsOf(keysFromFlattenedMap);
			// Keys in the Properties object are sorted.
			assertThat(keysFromProperties).containsExactly("bar.spam", "cat", "foo");
			// But the flattened map retains the order from the input.
			assertThat(keysFromFlattenedMap).containsExactly("cat", "foo", "bar.spam");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void standardTypesSupportedByDefault() throws Exception {
		setYaml("value: !!set\n  ? first\n  ? second");
		this.processor.process((properties, map) -> {
			assertThat(properties).containsExactly(entry("value[0]", "first"), entry("value[1]", "second"));
			assertThat(map.get("value")).isInstanceOf(Set.class);
			Set<String> set = (Set<String>) map.get("value");
			assertThat(set).containsExactly("first", "second");
		});
	}

	@Test
	void customTypeNotSupportedByDefault() throws Exception {
		URL url = new URL("https://localhost:9000/");
		setYaml("value: !!java.net.URL [\"" + url + "\"]");
		assertThatExceptionOfType(ConstructorException.class)
				.isThrownBy(() -> this.processor.process((properties, map) -> {}))
				.withMessageContaining("Unsupported type encountered in YAML document: java.net.URL");
	}

	@Test
	void customTypesSupportedDueToExplicitConfiguration() throws Exception {
		this.processor.setSupportedTypes(URL.class, String.class);

		URL url = new URL("https://localhost:9000/");
		setYaml("value: !!java.net.URL [!!java.lang.String [\"" + url + "\"]]");

		this.processor.process((properties, map) -> {
			assertThat(properties).containsExactly(entry("value", url));
			assertThat(map).containsExactly(entry("value", url));
		});
	}

	@Test
	void customTypeNotSupportedDueToExplicitConfiguration() {
		this.processor.setSupportedTypes(List.class);

		setYaml("value: !!java.net.URL [\"https://localhost:9000/\"]");

		assertThatExceptionOfType(ConstructorException.class)
				.isThrownBy(() -> this.processor.process((properties, map) -> {}))
				.withMessageContaining("Unsupported type encountered in YAML document: java.net.URL");
	}

	private void setYaml(String yaml) {
		this.processor.setResources(new ByteArrayResource(yaml.getBytes()));
	}

}

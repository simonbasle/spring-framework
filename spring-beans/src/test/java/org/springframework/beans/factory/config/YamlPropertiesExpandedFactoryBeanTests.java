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

package org.springframework.beans.factory.config;

import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class YamlPropertiesExpandedFactoryBeanTests {

	@SuppressWarnings("unchecked")
	@Test
	void loadFlattenedPropertiesAndSimpleLists() {
		YamlPropertiesExpandedFactoryBean factoryBean = new YamlPropertiesExpandedFactoryBean();
		final ClassPathResource yaml = new ClassPathResource(this.getClass().getSimpleName() + ".yaml",
				this.getClass());
		factoryBean.setResources(yaml);
		Properties prop = factoryBean.getObject();

		assertThat(prop).containsOnlyKeys(
				"animals.birds[0]",
				"animals.birds[1]",
				"animals.birds[2]",
				"animals.mammals[0]",
				"animals.mammals[1]",
				"animals.mammals[2]",
				"deeperList[0]",
				"deeperList[1].submap.firstKey",
				"deeperList[1].submap.secondKey",
				"deeperList[2][0]",
				"deeperList[2][1]",
				"deeperList[2][2]",
				"deeperList[3][0]",
				"deeperList[3][1].complexSubListTwo",
				"deeperList[3][2].complexSubListThree",
				"humans[0]",
				"humans[1]",
				"humans[2]",
				"listOfArrays[0][0]",
				"listOfArrays[0][1]",
				"listOfArrays[1][0]",
				"listOfArrays[1][1]",
				"animals.dragons", //not really a List (as the content is null and thus ambiguous)
				// below are the simple lists in addition to scalar/flattened entries
				"animals.birds",
				"animals.mammals",
				"animals.unicorns",
				"deeperList[2]",
				"humans",
				"listOfArrays[0]",
				"listOfArrays[1]"
		);

		assertThat(prop)
				.as("intermediate keys not added because they're not a simple list")
				.doesNotContainKeys(
						"deeperList", //a list that includes sub lists as well as a map
						"listOfArrays", //an array of arrays
						"deeperList[1]", //not a list but a map
						"deeperList[3]" //a list inside a list like deeperList[2], but it contains maps
				);

		assertThat(prop.get("animals.birds")).isInstanceOfSatisfying(List.class, l -> assertThat((List<String>) l)
				.as("animals.birds")
				.containsExactly("chicken", "eagle", "chicken"));

		assertThat(prop.get("animals.dragons")).isInstanceOfSatisfying(String.class, s -> assertThat(s)
				.as("animals.dragons is ambiguous/null").isEmpty());

		assertThat(prop.get("animals.mammals")).isInstanceOfSatisfying(List.class, l -> assertThat((List<String>) l)
				.as("animals.mammals")
				.containsExactly("cat", "dog", "horse"));

		assertThat(prop.get("animals.unicorns")).isInstanceOfSatisfying(List.class, l -> assertThat(l)
				.as("animals.unicorns is unambiguous empty list")
				.isEmpty());

		assertThat(prop.get("deeperList[2]")).isInstanceOfSatisfying(List.class, l -> assertThat((List<String>) l)
				.as("deeperList[2]")
				.containsExactly("subListOne", "two", "three"));

		assertThat(prop.get("humans")).isInstanceOfSatisfying(List.class, l -> assertThat((List<String>) l)
				.as("humans")
				.containsExactly("Bill", "Bob", "Joe"));

		assertThat(prop.get("listOfArrays[0]")).isInstanceOfSatisfying(List.class, l -> assertThat((List<String>) l)
				.as("listOfArrays[0]")
				.containsExactly("c00", "c01"));

		assertThat(prop.get("listOfArrays[1]")).isInstanceOfSatisfying(List.class, l -> assertThat((List<String>) l)
				.as("listOfArrays[1]")
				.containsExactly("c10", "c11"));
	}

	@Test
	void loadedKeysCanBeReadAsAStringProperty() {
		YamlPropertiesExpandedFactoryBean factoryBean = new YamlPropertiesExpandedFactoryBean();
		final ClassPathResource yaml = new ClassPathResource(this.getClass().getSimpleName() + ".yaml",
				this.getClass());
		factoryBean.setResources(yaml);
		Properties prop = factoryBean.getObject();

		assertThat(prop).isNotNull().isNotEmpty();
		assertThatNoException().isThrownBy(() -> prop.keySet().forEach(k -> prop.getProperty(k.toString())));
	}

	@Test
	void simpleListsHaveBracketEnclosedRepresentation() {
		YamlPropertiesExpandedFactoryBean factoryBean = new YamlPropertiesExpandedFactoryBean();
		final ClassPathResource yaml = new ClassPathResource(this.getClass().getSimpleName() + ".yaml",
				this.getClass());
		factoryBean.setResources(yaml);
		Properties prop = factoryBean.getObject();

		assertThat(prop).isNotNull().isNotEmpty();
		assertThat(prop.getProperty("animals.dragons")).as("animals.dragons")
				.isEmpty();
		assertThat(prop.getProperty("animals.birds")).as("animals.birds")
				.isEqualTo("[chicken, eagle, chicken]");
		assertThat(prop.getProperty("animals.mammals")).as("animals.mammals")
				.isEqualTo("[cat, dog, horse]");
		assertThat(prop.getProperty("animals.unicorns")).as("animals.unicorns")
				.isEqualTo("[]");
		assertThat(prop.getProperty("deeperList[2]")).as("deeperList[2]")
				.isEqualTo("[subListOne, two, three]");
		assertThat(prop.getProperty("humans")).as("humans")
				.isEqualTo("[Bill, Bob, Joe]");
		assertThat(prop.getProperty("listOfArrays[0]")).as("listOfArrays[0]")
				.isEqualTo("[c00, c01]");
		assertThat(prop.getProperty("listOfArrays[1]")).as("listOfArrays[1]")
				.isEqualTo("[c10, c11]");
	}

}
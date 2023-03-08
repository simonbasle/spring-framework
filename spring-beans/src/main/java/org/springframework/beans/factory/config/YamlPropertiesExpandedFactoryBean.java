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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.core.CollectionFactory;

/**
 * Factory for {@link java.util.Properties} that reads from a YAML source,
 * exposing a semi-flat structure of String property values which includes
 * a representation of simple Lists.
 *
 * <p>Expanding on the flat structure exposed by its parent
 * {@link YamlPropertiesFactoryBean}, this factory additionally finds simple Lists nested inside the structure and
 * adds these with a String representation enclosed in square brackets.
 * A List is considered simple if it only contains scalar values, no inner lists
 * and no maps.
 *
 * <p>This factory does recursively inspect any inner list or map it finds nested
 * inside a list though. So for instance this YAML
 *
 * <pre class="code">
 * servers:
 * - [ dev.bar.com, foo.bar.com ]
 * - foo.com
 * - [ dev.example.org, { host: example.org, subdomain: foo } ]
 * </pre>
 *
 * becomes properties like this:
 *
 * <pre class="code">
 * servers[0][0]=dev.bar.com
 * servers[0][1]=foo.bar.com
 * servers[1]=foo.com
 * servers[2][0]=dev.example.org
 * servers[2][1].host=example.org
 * servers[2][1].subdomain=foo
 * servers[0]=[dev.bar.com, foo.bar.com]
 * </pre>
 *
 * Notice there is no entry for {@code servers[2]}, because it contains an
 * inlined map at index {@code 1}.
 *
 * <p>Requires SnakeYAML 1.18 or higher, as of Spring Framework 6.1.0.
 *
 * @author Simon Baslé
 * @since 6.1.0
 */
public class YamlPropertiesExpandedFactoryBean extends YamlPropertiesFactoryBean {

	@Override
	protected Properties createProperties() {
		final Properties result = CollectionFactory.createStringAdaptingProperties();
		process((yamlFlattenedContent, yamlMapContent) -> {
			result.putAll(yamlFlattenedContent);
			deepAddLists(yamlMapContent, "", result);
		});
		return result;
	}

	static void deepAddLists(Map<String, Object> currentLevel, String keyPart, Properties destination) {
		currentLevel.forEach((k, v) -> {
			String key = keyPart.isBlank() ? String.valueOf(k) : keyPart + "." + k;
			if (v instanceof List<?> l) {
				List<Object> stringList = new ArrayList<>(l.size());
				boolean isSimpleList = true;
				for (int i = 0; i < l.size(); i++) {
					Object o = l.get(i);
					if (o instanceof List<?> innerList) {
						//we do check for a simple sub-list
						String innerKey = key + "[" + i + "]";
						deepAddLists(Collections.singletonMap(innerKey, innerList), "", destination);
						//we don't add the current list (not simple).
						//we still want to inspect other list elements in case a sublist is present...
						isSimpleList = false;
					}
					else if (o instanceof  Map<?, ?> innerMap) {
						//the individual entries will be present in the flattened properties.
						//we DON'T add the inner Map, nor the current list.
						//we still want to inspect other list elements in case a sublist is present...
						isSimpleList = false;
					}
					else if (o != null) {
						stringList.add(o.toString());
					}
				}
				if (isSimpleList) {
					//only add full list if it had no sub-lists and no sub-maps
					destination.put(key, stringList);
				}
			}
			else if (v instanceof Map<?, ?> m) {
				@SuppressWarnings("unchecked") Map<String, Object> mm = (Map<String, Object>) m;
				deepAddLists(mm, key, destination);
			}
		});
	}
}

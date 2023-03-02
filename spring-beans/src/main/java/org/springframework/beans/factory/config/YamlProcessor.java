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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;

import org.springframework.core.CollectionFactory;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for YAML factories.
 *
 * <p>Requires SnakeYAML 1.18 or higher, as of Spring Framework 5.0.6.
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 4.1
 */
public abstract class YamlProcessor {

	private final Log logger = LogFactory.getLog(getClass());

	private ResolutionMethod resolutionMethod = ResolutionMethod.OVERRIDE;

	private Resource[] resources = new Resource[0];

	private List<DocumentMatcher> documentMatchers = Collections.emptyList();

	private boolean matchDefault = true;

	private Set<String> supportedTypes = Collections.emptySet();

	private boolean isIncludeSimpleLists = false;


	/**
	 * A map of document matchers allowing callers to selectively use only
	 * some of the documents in a YAML resource. In YAML documents are
	 * separated by {@code ---} lines, and each document is converted
	 * to properties before the match is made. E.g.
	 * <pre class="code">
	 * environment: dev
	 * url: https://dev.bar.com
	 * name: Developer Setup
	 * ---
	 * environment: prod
	 * url:https://foo.bar.com
	 * name: My Cool App
	 * </pre>
	 * when mapped with
	 * <pre class="code">
	 * setDocumentMatchers(properties -&gt;
	 *     ("prod".equals(properties.getProperty("environment")) ? MatchStatus.FOUND : MatchStatus.NOT_FOUND));
	 * </pre>
	 * would end up as
	 * <pre class="code">
	 * environment=prod
	 * url=https://foo.bar.com
	 * name=My Cool App
	 * </pre>
	 */
	public void setDocumentMatchers(DocumentMatcher... matchers) {
		this.documentMatchers = List.of(matchers);
	}

	/**
	 * Flag indicating that a document for which all the
	 * {@link #setDocumentMatchers(DocumentMatcher...) document matchers} abstain will
	 * nevertheless match. Default is {@code true}.
	 */
	public void setMatchDefault(boolean matchDefault) {
		this.matchDefault = matchDefault;
	}

	/**
	 * Method to use for resolving resources. Each resource will be converted to a Map,
	 * so this property is used to decide which map entries to keep in the final output
	 * from this factory. Default is {@link ResolutionMethod#OVERRIDE}.
	 */
	public void setResolutionMethod(ResolutionMethod resolutionMethod) {
		Assert.notNull(resolutionMethod, "ResolutionMethod must not be null");
		this.resolutionMethod = resolutionMethod;
	}

	/**
	 * Set locations of YAML {@link Resource resources} to be loaded.
	 * @see ResolutionMethod
	 */
	public void setResources(Resource... resources) {
		this.resources = resources;
	}

	/**
	 * Set the supported types that can be loaded from YAML documents.
	 * <p>If no supported types are configured, only Java standard classes
	 * (as defined in {@link org.yaml.snakeyaml.constructor.SafeConstructor})
	 * encountered in YAML documents will be supported.
	 * If an unsupported type is encountered, an {@link IllegalStateException}
	 * will be thrown when the corresponding YAML node is processed.
	 * @param supportedTypes the supported types, or an empty array to clear the
	 * supported types
	 * @since 5.1.16
	 * @see #createYaml()
	 */
	public void setSupportedTypes(Class<?>... supportedTypes) {
		if (ObjectUtils.isEmpty(supportedTypes)) {
			this.supportedTypes = Collections.emptySet();
		}
		else {
			Assert.noNullElements(supportedTypes, "'supportedTypes' must not contain null elements");
			this.supportedTypes = Arrays.stream(supportedTypes).map(Class::getName)
					.collect(Collectors.toUnmodifiableSet());
		}
	}

	/**
	 * Set the {@code isIncludeSimpleLists} flag, which enables adding the string
	 * representation of simple lists/arrays to the {@code Properties} in addition
	 * to the flattened indexed keys. When set to {@code false}, the default behavior
	 * of just adding flattened keys is restored.
	 * <p> For example, considering the following YAML snippet:
	 * <pre><code>
	 * animals:
	 *   mammals:
	 *    - cat
	 *    - dog
	 *    - horse
	 *   unicorns: []
	 * </code></pre>
	 * By default ({@code isAddFulllists == false}) the following entries are
	 * added to the properties:
	 * <pre><code>
	 * animals.mammals[0]="cat"
	 * animals.mammals[1]="dog"
	 * animals.mammals[2]="horse"
	 * animals.unicorns="" //an empty String
	 * </code></pre>
	 * With the flag set to {@code true}, an additional {@code animals.mammals}
	 * key is added with the {@code List} representation of all elements.
	 * The properties become:
	 * <pre><code>
	 * animals.mammals[0]="cat"
	 * animals.mammals[1]="dog"
	 * animals.mammals[2]="horse"
	 * animals.mammals="[cat,dog,horse]" // can be parsed as a List
	 * animals.unicorns="[]" // String representation of an empty List
	 * </code></pre>
	 * <p>This flag is ignored at levels where a list is detected to
	 * contain a nested list, array or map:
	 * <code><pre>
	 * all:
	 *   animals:
	 *    - name: cat
	 *      type: mammal
	 *    - name: dragon
	 *      type: imaginary
	 * </pre></code>
	 * The above YAML results in the following properties even if this flag is
	 * set to {@code true}:
	 * <code><pre>
	 * all.animals[0].name=cat
	 * all.animals[0].type=mammal
	 * all.animals[1].name=dragon
	 * all.animals[1].type=imaginary
	 * </pre></code>
	 *
	 * @param isIncludeSimpleLists {@code true} to enabling adding full lists during
	 * flattening, {@code false} to disable it (the default)
	 * @since 6.0.7
	 */
	public void setIncludeSimpleLists(boolean isIncludeSimpleLists) {
		this.isIncludeSimpleLists = isIncludeSimpleLists;
	}

	/**
	 * @return the value of the {{@link #setIncludeSimpleLists(boolean) isAddFullLists flag}
	 * @since 6.0.7
	 */
	protected boolean isIncludeSimpleLists() {
		return this.isIncludeSimpleLists;
	}

	/**
	 * Provide an opportunity for subclasses to process the Yaml parsed from the supplied
	 * resources. Each resource is parsed in turn and the documents inside checked against
	 * the {@link #setDocumentMatchers(DocumentMatcher...) matchers}. If a document
	 * matches it is passed into the callback, along with its representation as Properties.
	 * Depending on the {@link #setResolutionMethod(ResolutionMethod)} not all the
	 * documents will be parsed.
	 * @param callback a callback to delegate to once matching documents are found
	 * @see #createYaml()
	 */
	protected void process(MatchCallback callback) {
		Yaml yaml = createYaml();
		for (Resource resource : this.resources) {
			boolean found = process(callback, yaml, resource);
			if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND && found) {
				return;
			}
		}
	}

	/**
	 * Create the {@link Yaml} instance to use.
	 * <p>The default implementation sets the "allowDuplicateKeys" flag to {@code false},
	 * enabling built-in duplicate key handling in SnakeYAML 1.18+.
	 * <p>As of Spring Framework 5.1.16, if custom {@linkplain #setSupportedTypes
	 * supported types} have been configured, the default implementation creates
	 * a {@code Yaml} instance that filters out unsupported types encountered in
	 * YAML documents. If an unsupported type is encountered, an
	 * {@link IllegalStateException} will be thrown when the node is processed.
	 * @see LoaderOptions#setAllowDuplicateKeys(boolean)
	 */
	protected Yaml createYaml() {
		LoaderOptions loaderOptions = new LoaderOptions();
		loaderOptions.setAllowDuplicateKeys(false);
		DumperOptions dumperOptions = new DumperOptions();
		return new Yaml(new FilteringConstructor(loaderOptions), new Representer(dumperOptions),
				dumperOptions, loaderOptions);
	}

	private boolean process(MatchCallback callback, Yaml yaml, Resource resource) {
		int count = 0;
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Loading from YAML: " + resource);
			}
			try (Reader reader = new UnicodeReader(resource.getInputStream())) {
				for (Object object : yaml.loadAll(reader)) {
					if (object != null && process(asMap(object), callback)) {
						count++;
						if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND) {
							break;
						}
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Loaded " + count + " document" + (count > 1 ? "s" : "") +
							" from YAML resource: " + resource);
				}
			}
		}
		catch (IOException ex) {
			handleProcessError(resource, ex);
		}
		return (count > 0);
	}

	private void handleProcessError(Resource resource, IOException ex) {
		if (this.resolutionMethod != ResolutionMethod.FIRST_FOUND &&
				this.resolutionMethod != ResolutionMethod.OVERRIDE_AND_IGNORE) {
			throw new IllegalStateException(ex);
		}
		if (logger.isWarnEnabled()) {
			logger.warn("Could not load map from " + resource + ": " + ex.getMessage());
		}
	}

	/**
	 * Customize how the {@code Object} keys in the raw Map from the YAML parser
	 * are turned into {@code String} keys in a {@code Map<String, Object>},
	 * as a preparation step to the flattening and conversion to a
	 * {@code Properties} instance.
	 * <p>The default implementation performs the following sanitization:
	 * <ul>
	 *     <li>{@code String} keys escaped with brackets in the YAML have brackets
	 *     doubled. These will be used as full keys, dot-separated, in the Properties.</li>
	 *     <li>non-string keys (eg. integer keys) are escaped with single brackets.
	 *     These will be used as indexes (no dot separator) in the Properties.</li>
	 * </ul>
	 *
	 * @param rawKey the raw key as parsed by the YAML parser
	 * @return the sanitized key in {@code String} form
	 */
	protected String sanitizeKey(Object rawKey) {
		if (rawKey instanceof CharSequence csKey) {
			String key = csKey.toString();
			//detecting keys escaped in brackets, turning to double brackets in properties
			if (csKey.length() > 0 && csKey.charAt(0) == '['
					&& csKey.charAt(csKey.length()-1) == ']') {
				key = key.replace("[", "[[").replace("]", "]]");
			}
			return key;
		}
		else {
			// It has to be a map key in this case
			return "[" + rawKey + "]";
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<String, Object> asMap(Object object) {
		// YAML can have numbers as keys
		Map<String, Object> result = new LinkedHashMap<>();
		if (!(object instanceof Map map)) {
			// A document can be a text literal
			result.put("document", object);
			return result;
		}

		map.forEach((key, value) -> {
			if (value instanceof Map) {
				value = asMap(value);
			}
			String sanitizedKey = sanitizeKey(key);
			result.put(sanitizedKey, value);
		});
		return result;
	}

	private boolean process(Map<String, Object> map, MatchCallback callback) {
		Properties properties = CollectionFactory.createStringAdaptingProperties();
		properties.putAll(getFlattenedMap(map));

		if (this.documentMatchers.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Merging document (no matchers set): " + map);
			}
			callback.process(properties, map);
			return true;
		}

		MatchStatus result = MatchStatus.ABSTAIN;
		for (DocumentMatcher matcher : this.documentMatchers) {
			MatchStatus match = matcher.matches(properties);
			result = MatchStatus.getMostSpecific(match, result);
			if (match == MatchStatus.FOUND) {
				if (logger.isDebugEnabled()) {
					logger.debug("Matched document with document matcher: " + properties);
				}
				callback.process(properties, map);
				return true;
			}
		}

		if (result == MatchStatus.ABSTAIN && this.matchDefault) {
			if (logger.isDebugEnabled()) {
				logger.debug("Matched document with default matcher: " + map);
			}
			callback.process(properties, map);
			return true;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Unmatched document: " + map);
		}
		return false;
	}

	/**
	 * Return a flattened version of the given map, recursively following any nested Map
	 * or Collection values. Entries from the resulting map retain the same order as the
	 * source. When called with the Map from a {@link MatchCallback} the result will
	 * contain the same values as the {@link MatchCallback} Properties.
	 * @param source the source map
	 * @return a flattened map
	 * @since 4.1.3
	 */
	protected final Map<String, Object> getFlattenedMap(Map<String, Object> source) {
		Map<String, Object> result = new LinkedHashMap<>();
		buildFlattenedMap(result, source, null);
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, @Nullable String path) {
		source.forEach((key, value) -> {
			if (StringUtils.hasText(path)) {
				if (key.startsWith("[") && !key.startsWith("[[")) {
					key = path + key;
				}
				else {
					key = path + '.' + key;
				}
			}
			if (value instanceof String) {
				result.put(key, value);
			}
			else if (value instanceof Map map) {
				// Need a compound key
				buildFlattenedMap(result, map, key);
			}
			else if (value instanceof Collection collection) {
				// Need a compound key
				if (collection.isEmpty()) {
					result.put(key, isIncludeSimpleLists() ? Collections.emptyList() : "");
				}
				else {
					int count = 0;
					List<String> subCollectionKeys = new ArrayList<>();
					for (Object object : collection) {
						String indexKeyPart = "[" + (count++) + "]";
						if (object instanceof Collection || object instanceof Map) {
							subCollectionKeys.add(indexKeyPart);
						}
						buildFlattenedMap(result, Collections.singletonMap(indexKeyPart, object), key);
					}
					if (isIncludeSimpleLists()) {
						if (!subCollectionKeys.isEmpty()) {
							if (this.logger.isDebugEnabled()) {
								this.logger.debug(key + " not added as a full list because it contains "
										+ "nested lists/maps at indexes " + subCollectionKeys.stream().collect(Collectors.joining(",")));
							}
							else {
								this.logger.warn(key + " not added as a full list because it contains "
										+ subCollectionKeys.size() + " nested lists/maps");
							}
						}
						else {
							result.put(key, collection);
						}
					}
				}
			}
			else {
				result.put(key, (value != null ? value : ""));
			}
		});
	}


	/**
	 * Callback interface used to process the YAML parsing results.
	 */
	@FunctionalInterface
	public interface MatchCallback {

		/**
		 * Process the given representation of the parsing results.
		 * @param properties the properties to process (as a flattened
		 * representation with indexed keys in case of a collection or map)
		 * @param map the result map (preserving the original value structure
		 * in the YAML document)
		 */
		void process(Properties properties, Map<String, Object> map);
	}


	/**
	 * Strategy interface used to test if properties match.
	 */
	@FunctionalInterface
	public interface DocumentMatcher {

		/**
		 * Test if the given properties match.
		 * @param properties the properties to test
		 * @return the status of the match
		 */
		MatchStatus matches(Properties properties);
	}


	/**
	 * Status returned from {@link DocumentMatcher#matches(java.util.Properties)}.
	 */
	public enum MatchStatus {

		/**
		 * A match was found.
		 */
		FOUND,

		/**
		 * No match was found.
		 */
		NOT_FOUND,

		/**
		 * The matcher should not be considered.
		 */
		ABSTAIN;

		/**
		 * Compare two {@link MatchStatus} items, returning the most specific status.
		 */
		public static MatchStatus getMostSpecific(MatchStatus a, MatchStatus b) {
			return (a.ordinal() < b.ordinal() ? a : b);
		}
	}


	/**
	 * Method to use for resolving resources.
	 */
	public enum ResolutionMethod {

		/**
		 * Replace values from earlier in the list.
		 */
		OVERRIDE,

		/**
		 * Replace values from earlier in the list, ignoring any failures.
		 */
		OVERRIDE_AND_IGNORE,

		/**
		 * Take the first resource in the list that exists and use just that.
		 */
		FIRST_FOUND
	}


	/**
	 * {@link Constructor} that supports filtering of unsupported types.
	 * <p>If an unsupported type is encountered in a YAML document, an
	 * {@link IllegalStateException} will be thrown from {@link #getClassForName}.
	 */
	private class FilteringConstructor extends Constructor {

		FilteringConstructor(LoaderOptions loaderOptions) {
			super(loaderOptions);
		}

		@Override
		protected Class<?> getClassForName(String name) throws ClassNotFoundException {
			Assert.state(YamlProcessor.this.supportedTypes.contains(name),
					() -> "Unsupported type encountered in YAML document: " + name);
			return super.getClassForName(name);
		}
	}

}

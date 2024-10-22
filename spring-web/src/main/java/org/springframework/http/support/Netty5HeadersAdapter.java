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

package org.springframework.http.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.netty5.handler.codec.http.headers.HttpHeaders;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;

/**
 * {@code MultiValueMap} implementation for wrapping Netty HTTP headers.
 *
 * @author Violeta Georgieva
 * @since 6.1
 */
public final class Netty5HeadersAdapter extends AbstractHeadersAdapter {

	private final HttpHeaders headers;


	/**
	 * Create a new {@code Netty5HeadersAdapter} based on the given
	 * {@code HttpHeaders}.
	 */
	public Netty5HeadersAdapter(HttpHeaders headers) {
		Assert.notNull(headers, "Headers must not be null");
		this.headers = headers;
	}


	@Override
	@Nullable
	public String getFirst(String key) {
		CharSequence value = this.headers.get(key);
		return (value != null ? value.toString() : null);
	}

	@Override
	public void add(String key, @Nullable String value) {
		if (value != null) {
			this.headers.add(key, value);
		}
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		this.headers.add(key, values);
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		values.forEach(this.headers::add);
	}

	@Override
	public void set(String key, @Nullable String value) {
		if (value != null) {
			this.headers.set(key, value);
		}
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach(this.headers::set);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		Map<String, String> singleValueMap = new LinkedCaseInsensitiveMap<>(
				this.headers.size(), Locale.ROOT);
		this.headers.forEach(entry -> singleValueMap.putIfAbsent(
				entry.getKey().toString(), entry.getValue().toString()));
		return singleValueMap;
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return (key instanceof String headerName && this.headers.contains(headerName));
	}

	@Override
	public boolean containsValue(Object value) {
		return (value instanceof String &&
				StreamSupport.stream(this.headers.spliterator(), false)
						.anyMatch(entry -> value.equals(entry.getValue())));
	}

	@Override
	@Nullable
	public List<String> get(Object key) {
		Iterator<CharSequence> iterator = this.headers.valuesIterator((CharSequence) key);
		if (iterator.hasNext()) {
			List<String> result = new ArrayList<>();
			iterator.forEachRemaining(value -> result.add(value.toString()));
			return result;
		}
		return null;
	}

	@Nullable
	@Override
	public List<String> put(String key, @Nullable List<String> value) {
		List<String> previousValues = get(key);
		this.headers.set(key, value);
		return previousValues;
	}

	@Nullable
	@Override
	public List<String> remove(Object key) {
		if (key instanceof String headerName) {
			List<String> previousValues = get(headerName);
			this.headers.remove(headerName);
			return previousValues;
		}
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		map.forEach(this.headers::set);
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public String toString() {
		return org.springframework.http.HttpHeaders.formatHeaders(this);
	}

	@Override
	protected Stream<String> getOriginalHeaderNames() {
		return this.headers.names().stream().map(CharSequence::toString);
	}

	@Override
	protected Entry<String, List<String>> listAdaptingEntry(String key) {
		return new HeaderEntry(key);
	}

	@Override
	protected boolean originalContains(String key) {
		return this.headers.contains(key);
	}

	@Override
	protected void originalRemove(String key) {
		this.headers.remove(key);
	}


	private class HeaderEntry implements Entry<String, List<String>> {

		private final CharSequence key;

		HeaderEntry(CharSequence key) {
			this.key = key;
		}

		@Override
		public String getKey() {
			return this.key.toString();
		}

		@Override
		public List<String> getValue() {
			List<String> values = get(this.key);
			return (values != null ? values : Collections.emptyList());
		}

		@Override
		public List<String> setValue(List<String> value) {
			List<String> previousValues = getValue();
			headers.set(this.key, value);
			return previousValues;
		}
	}
}

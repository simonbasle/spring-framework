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

package org.springframework.http.support;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

public abstract class AbstractHeadersAdapter implements MultiValueMap<String, String> {

	protected abstract Stream<String> getOriginalHeaderNames();

	protected abstract void originalRemove(String key);

	protected abstract boolean originalContains(String key);

	protected abstract Entry<String, List<String>> listAdaptingEntry(String key);

	@NonNull
	@Override
	public final Set<String> keySet() {
		return new CaseInsensitiveHeaderSet();
	}

	@Override
	public final int size() {
		return keySet().size();
	}

	@Override
	public final Collection<List<String>> values() {
		return keySet().stream().map(this::get).toList();
	}

	@Override
	public final Set<Entry<String, List<String>>> entrySet() {
		return keySet().stream()
				.map(this::listAdaptingEntry)
				.collect(Collectors.toSet());
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		Set<String> keys = keySet();

		Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(keys.size());
		this.forEach((key, value) -> singleValueMap.put(key, value.get(0)));
		return singleValueMap;
	}


	private class CaseInsensitiveHeaderSet extends AbstractSet<String> {

		protected Set<String> deduplicate() {
			Set<String> deduplicatedKeys = new LinkedHashSet<>();
			Set<String> seen = new LinkedHashSet<>();
			getOriginalHeaderNames().forEach(cs -> {
				String compareKey = cs.toLowerCase(Locale.ROOT);
				if (!seen.contains(compareKey)) {
					// we store the original case of the first encountered occurrence
					deduplicatedKeys.add(cs);
					seen.add(compareKey);
				}
			});
			return deduplicatedKeys;
		}

		@Override
		public Iterator<String> iterator() {
			return new DeduplicatingHeaderNamesIterator();
		}

		@Override
		public int size() {
			return deduplicate().size();
		}

		private final class DeduplicatingHeaderNamesIterator implements Iterator<String> {

			private Iterator<String> iterator;

			@Nullable
			private String currentName;

			private DeduplicatingHeaderNamesIterator() {
				this.iterator = CaseInsensitiveHeaderSet.this.deduplicate().iterator();
			}

			@Override
			public boolean hasNext() {
				return this.iterator.hasNext();
			}

			@Override
			public String next() {
				this.currentName = this.iterator.next();
				return this.currentName;
			}

			@Override
			public void remove() {
				if (this.currentName == null) {
					throw new IllegalStateException("No current Header in iterator");
				}
				if (!AbstractHeadersAdapter.this.originalContains(this.currentName)) {
					throw new IllegalStateException("Header not present: " + this.currentName);
				}
				AbstractHeadersAdapter.this.originalRemove(this.currentName);
			}
		}
	}
}

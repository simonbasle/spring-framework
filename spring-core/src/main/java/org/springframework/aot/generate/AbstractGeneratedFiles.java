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

package org.springframework.aot.generate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.core.io.InputStreamSource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Common base class for implementations of {@link GeneratedFiles} that tracks
 * files which have been added.
 *
 * @author Simon Baslé
 * @since 6.2
 */
abstract class AbstractGeneratedFiles implements GeneratedFiles {

	protected final Map<Kind, Map<String, Entry>> entries = new HashMap<>();

	@Override
	public void addFile(Kind kind, String path, InputStreamSource content) {
		Entry e = getEntry(kind, path);
		Assert.state(!e.alreadyExists(), () -> "Path '" + path + "' already in use");
		persistAndTrackEntry(e, content);
	}

	@Override
	public void handleFile(Kind kind, String path, Function<Entry, InputStreamSource> computeFunction) {
		Entry e = getEntry(kind, path);
		final InputStreamSource replacement = computeFunction.apply(e);
		if (replacement != null) {
			persistAndTrackEntry(e, replacement);
		}
	}

	private void persistAndTrackEntry(Entry e, InputStreamSource replacement) {
		Kind kind = e.kind();
		String path = e.path();
		persistContent(kind, path, replacement);
		this.entries.computeIfAbsent(kind, k -> new LinkedHashMap<>())
				.put(path, new LazyEntry(kind, path, () -> replacement));
	}

	private Entry getEntry(Kind kind, String path) {
		return this.entries.computeIfAbsent(kind, k -> new LinkedHashMap<>()).getOrDefault(path,
				new EmptyEntry(kind, path));
	}

	/**
	 * Persist the {@code replacement} content. The replacement will also be
	 * stored in this {@code entries} map, unless this method throws to indicate
	 * a persistence error.
	 */
	protected abstract void persistContent(Kind kind, String path, InputStreamSource replacement);


	static class EmptyEntry implements Entry {
		private final Kind kind;
		private final String path;

		EmptyEntry(Kind kind, String path) {
			this.kind = kind;
			this.path = path;
		}

		@Override
		public Kind kind() {
			return this.kind;
		}

		@Override
		public String path() {
			return this.path;
		}

		@Override
		public boolean alreadyExists() {
			return false;
		}

		@Nullable
		@Override
		public InputStreamSource existingContent() {
			return null;
		}
	}

	static final class LazyEntry extends EmptyEntry {

		private final Supplier<InputStreamSource> existingContentSupplier;

		LazyEntry(Kind kind, String path, Supplier<InputStreamSource> existingContentSupplier) {

			super(kind, path);
			this.existingContentSupplier = existingContentSupplier;
		}

		@Override
		public boolean alreadyExists() {
			return true;
		}

		@Nullable
		@Override
		public InputStreamSource existingContent() {
			return this.existingContentSupplier.get();
		}
	}

}

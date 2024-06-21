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

package org.springframework.aot.generate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.util.Assert;

/**
 * {@link GeneratedFiles} implementation that stores generated files using a
 * {@link FileSystem}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
public class FileSystemGeneratedFiles implements GeneratedFiles {

	private final Function<Kind, Path> roots;

	private final Set<Path> processedPaths;


	/**
	 * Create a new {@link FileSystemGeneratedFiles} instance with all files
	 * stored under the specific {@code root}. The following subdirectories are
	 * created for the different file {@link Kind kinds}:
	 * <ul>
	 * <li>{@code sources}</li>
	 * <li>{@code resources}</li>
	 * <li>{@code classes}</li>
	 * </ul>
	 * @param root the root path
	 * @see #FileSystemGeneratedFiles(Function)
	 */
	public FileSystemGeneratedFiles(Path root) {
		this(conventionRoots(root));
	}

	/**
	 * Create a new {@link FileSystemGeneratedFiles} instance with all files
	 * stored under the root provided by the given {@link Function}.
	 * @param roots a function that returns the root to use for the given
	 * {@link Kind}
	 */
	public FileSystemGeneratedFiles(Function<Kind, Path> roots) {
		Assert.notNull(roots, "'roots' must not be null");
		Assert.isTrue(Arrays.stream(Kind.values()).map(roots).noneMatch(Objects::isNull),
				"'roots' must return a value for all file kinds");
		this.roots = roots;
		this.processedPaths = new HashSet<>();
	}


	private static Function<Kind, Path> conventionRoots(Path root) {
		Assert.notNull(root, "'root' must not be null");
		return kind -> switch (kind) {
			case SOURCE -> root.resolve("sources");
			case RESOURCE -> root.resolve("resources");
			case CLASS -> root.resolve("classes");
		};
	}

	@Override
	public void addFile(Kind kind, String path, InputStreamSource content) {
		handleFile(kind, path, f -> {
			Assert.state(!f.alreadyExists(), () -> "Path '" + path + "' already in use");
			return content;
		});
	}

	@Override
	public void handleFile(Kind kind, String path,
			Function<GeneratedFile, InputStreamSource> computeFunction) {

		Assert.notNull(kind, "'kind' must not be null");
		Assert.hasLength(path, "'path' must not be empty");
		Assert.notNull(computeFunction, "'computeFunction' must not be null");
		Path root = this.roots.apply(kind).toAbsolutePath().normalize();
		Path relativePath = root.resolve(path).toAbsolutePath().normalize();
		Assert.isTrue(relativePath.startsWith(root), "'path' must be relative");

		boolean exists = this.processedPaths.contains(relativePath);
		InputStreamSource existing = exists ? new FileSystemResource(relativePath) : null;
		final GeneratedFile generatedFile = new GeneratedFile(kind, path, exists, existing);
		try {
			InputStreamSource replacement = computeFunction.apply(generatedFile);
			if (replacement == null || replacement.equals(existing)) {
				//cancel the operation
				return;
			}
			try (InputStream inputStream = replacement.getInputStream()) {
				Files.createDirectories(relativePath.getParent());
				Files.copy(inputStream, relativePath, StandardCopyOption.REPLACE_EXISTING);
				this.processedPaths.add(relativePath);
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}

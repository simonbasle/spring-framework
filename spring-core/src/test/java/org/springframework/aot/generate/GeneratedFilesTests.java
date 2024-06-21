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

package org.springframework.aot.generate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import javax.lang.model.element.Modifier;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link GeneratedFiles}.
 *
 * @author Phillip Webb
 */
class GeneratedFilesTests {

	private final TestGeneratedFiles generatedFiles = new TestGeneratedFiles();

	@Test
	void addSourceFileWithJavaFileAddsFile() throws Exception {
		MethodSpec main = MethodSpec.methodBuilder("main")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC).returns(void.class)
				.addParameter(String[].class, "args")
				.addStatement("$T.out.println($S)", System.class, "Hello, World!")
				.build();
		TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld")
				.addModifiers(Modifier.PUBLIC, Modifier.FINAL).addMethod(main).build();
		JavaFile javaFile = JavaFile.builder("com.example", helloWorld).build();
		this.generatedFiles.addSourceFile(javaFile);
		assertThatFileAdded(Kind.SOURCE, "com/example/HelloWorld.java")
				.contains("Hello, World!");
	}

	@Test
	void addSourceFileWithJavaFileInTheDefaultPackageThrowsException() {
		TypeSpec helloWorld = TypeSpec.classBuilder("HelloWorld").build();
		JavaFile javaFile = JavaFile.builder("", helloWorld).build();
		assertThatIllegalArgumentException().isThrownBy(() -> this.generatedFiles.addSourceFile(javaFile))
				.withMessage("Could not add 'HelloWorld', processing classes in the "
						+ "default package is not supported. Did you forget to add a package statement?");
	}

	@Test
	void addSourceFileWithCharSequenceAddsFile() throws Exception {
		this.generatedFiles.addSourceFile("com.example.HelloWorld", "{}");
		assertThatFileAdded(Kind.SOURCE, "com/example/HelloWorld.java").isEqualTo("{}");
	}

	@Test
	void addSourceFileWithCharSequenceWhenClassNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedFiles.addSourceFile("", "{}"))
				.withMessage("'className' must not be empty");
	}

	@Test
	void addSourceFileWithCharSequenceWhenClassNameIsInTheDefaultPackageThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedFiles.addSourceFile("HelloWorld", "{}"))
				.withMessage("Could not add 'HelloWorld', processing classes in the "
						+ "default package is not supported. Did you forget to add a package statement?");
	}

	@Test
	void addSourceFileWithCharSequenceWhenClassNameIsInvalidThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedFiles
						.addSourceFile("com/example/HelloWorld.java", "{}"))
				.withMessage("'className' must be a valid identifier, got 'com/example/HelloWorld.java'");
	}

	@Test
	void addSourceFileWithConsumedAppendableAddsFile() throws Exception {
		this.generatedFiles.addSourceFile("com.example.HelloWorld",
				appendable -> appendable.append("{}"));
		assertThatFileAdded(Kind.SOURCE, "com/example/HelloWorld.java").isEqualTo("{}");
	}

	@Test
	void addSourceFileWithInputStreamSourceAddsFile() throws Exception {
		Resource resource = new ByteArrayResource("{}".getBytes(StandardCharsets.UTF_8));
		this.generatedFiles.addSourceFile("com.example.HelloWorld", resource);
		assertThatFileAdded(Kind.SOURCE, "com/example/HelloWorld.java").isEqualTo("{}");
	}

	@Test
	void addResourceFileWithCharSequenceAddsFile() throws Exception {
		this.generatedFiles.addResourceFile("META-INF/file", "test");
		assertThatFileAdded(Kind.RESOURCE, "META-INF/file").isEqualTo("test");
	}

	@Test
	void addResourceFileWithConsumedAppendableAddsFile() throws Exception {
		this.generatedFiles.addResourceFile("META-INF/file",
				appendable -> appendable.append("test"));
		assertThatFileAdded(Kind.RESOURCE, "META-INF/file").isEqualTo("test");
	}

	@Test
	void addResourceFileWithInputStreamSourceAddsFile() throws IOException {
		Resource resource = new ByteArrayResource(
				"test".getBytes(StandardCharsets.UTF_8));
		this.generatedFiles.addResourceFile("META-INF/file", resource);
		assertThatFileAdded(Kind.RESOURCE, "META-INF/file").isEqualTo("test");
	}

	@Test
	void addClassFileWithInputStreamSourceAddsFile() throws IOException {
		Resource resource = new ByteArrayResource(
				"test".getBytes(StandardCharsets.UTF_8));
		this.generatedFiles.addClassFile("com/example/HelloWorld.class", resource);
		assertThatFileAdded(Kind.CLASS, "com/example/HelloWorld.class").isEqualTo("test");
	}

	@Test
	void addFileWithCharSequenceAddsFile() throws Exception {
		this.generatedFiles.addFile(Kind.RESOURCE, "META-INF/file", "test");
		assertThatFileAdded(Kind.RESOURCE, "META-INF/file").isEqualTo("test");
	}

	@Test
	void addFileWithConsumedAppendableAddsFile() throws IOException {
		this.generatedFiles.addFile(Kind.SOURCE, "com/example/HelloWorld.java",
				appendable -> appendable.append("{}"));
		assertThatFileAdded(Kind.SOURCE, "com/example/HelloWorld.java").isEqualTo("{}");
	}

	@Test
	void handleFileAddsFile() throws IOException {
		this.generatedFiles.handleFile(Kind.RESOURCE, "META-INF/test/file.txt",
				f -> new AppendableConsumerInputStreamSource(appendable -> appendable.append("{}")));
		assertThatFileAdded(Kind.RESOURCE, "META-INF/test/file.txt").isEqualTo("{}");
	}

	@Test
	void handleFileThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> this.generatedFiles.handleFile(Kind.RESOURCE,
				"META-INF/test/file.txt", f -> { throw new IllegalStateException("exception in computeFunction"); }
		));
		assertThatFileNotAdded();
	}

	@Test
	void handleFileReplacesFile() throws IOException {
		this.generatedFiles.kind = Kind.RESOURCE;
		this.generatedFiles.path = "file.txt";
		this.generatedFiles.content = new AppendableConsumerInputStreamSource(a -> a.append("{}"));
		this.generatedFiles.handleFile(Kind.RESOURCE, "file.txt", f ->
				new AppendableConsumerInputStreamSource(a -> a.append("replacement")));
		assertThatFileAdded(Kind.RESOURCE, "file.txt").isEqualTo("replacement");
	}

	@Test
	void handleFileBacksOffWhenReturningOriginalInstance() throws IOException {
		this.generatedFiles.kind = Kind.RESOURCE;
		this.generatedFiles.path = "file.txt";
		this.generatedFiles.content = new AppendableConsumerInputStreamSource(a -> a.append("{}"));
		this.generatedFiles.handleFile(Kind.RESOURCE, "file.txt", GeneratedFiles.Entry::existingContent);
		assertThatFileAdded(Kind.RESOURCE, "file.txt").isEqualTo("{}");
	}

	@Test
	void handleFileBacksOffWhenReturningNull() throws IOException {
		this.generatedFiles.kind = Kind.RESOURCE;
		this.generatedFiles.path = "file.txt";
		this.generatedFiles.content = new AppendableConsumerInputStreamSource(a -> a.append("{}"));
		this.generatedFiles.handleFile(Kind.RESOURCE, "file.txt", f -> null);
		assertThatFileAdded(Kind.RESOURCE, "file.txt").isEqualTo("{}");
	}

	private AbstractStringAssert<?> assertThatFileAdded(Kind kind, String path)
			throws IOException {
		return this.generatedFiles.assertThatFileAdded(kind, path);
	}

	private void assertThatFileNotAdded() {
		assertThat(this.generatedFiles.content).isNull();
	}

	static class TestGeneratedFiles implements GeneratedFiles {

		private Kind kind;

		private String path;

		private InputStreamSource content;

		@Override
		public void addFile(Kind kind, String path, InputStreamSource content) {
			this.kind = kind;
			this.path = path;
			this.content = content;
		}

		@Override
		public void handleFile(Kind kind, String path, Function<Entry, InputStreamSource> computeFunction) {
			final Entry entry = new TestEntry(kind, path, kind.equals(this.kind) &&
					path.equals(this.path), this.content);
			final InputStreamSource newContent = computeFunction.apply(entry);
			if (newContent != null && !newContent.equals(this.content)) {
				addFile(kind, path, newContent);
			}
		}

		AbstractStringAssert<?> assertThatFileAdded(Kind kind, String path)
				throws IOException {
			assertThat(this.kind).as("kind").isEqualTo(kind);
			assertThat(this.path).as("path").isEqualTo(path);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			this.content.getInputStream().transferTo(out);
			return assertThat(out.toString(StandardCharsets.UTF_8));
		}

	}

	private static final class TestEntry extends AbstractGeneratedFiles.EmptyEntry {

		private final boolean alreadyExists;
		private final InputStreamSource content;

		TestEntry(Kind kind, String path, boolean exists, InputStreamSource content) {
			super(kind, path);
			this.alreadyExists = exists;
			this.content = content;
		}

		@Override
		public boolean alreadyExists() {
			return this.alreadyExists;
		}

		@Override
		public InputStreamSource existingContent() {
			return this.content;
		}
	}

}

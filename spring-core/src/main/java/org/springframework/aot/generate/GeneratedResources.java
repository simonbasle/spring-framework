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

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.core.io.InputStreamSource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.function.ThrowingConsumer;

/**
 * A managed collection of generated resources.
 *
 * <p>This class is stateful, so the same instance should be used for all class
 * generation.
 *
 * @author Simon Baslé
 * @since 6.2
 * @see GeneratedClasses
 */
public class GeneratedResources {

	private final String featureRootPath;

	private final List<GeneratedResource> resources;


	/**
	 * Create a new instance using no feature root path.
	 */
	GeneratedResources() {
		this("");
	}

	/**
	 * Create a new instance using the specified feature name as a root
	 * path for resources.
	 * @param featureRootPath the feature name to use
	 */
	GeneratedResources(String featureRootPath) {
		this(featureRootPath, new ArrayList<>());
	}

	private GeneratedResources(String featureRootPath, List<GeneratedResource> resources) {
		Assert.notNull(featureRootPath, "'featureRootPath' must not be null");
		this.featureRootPath = featureRootPath;
		this.resources = resources;
	}

	/**
	 * Add a new generated resource for the specified {@code featureName}
	 * targeting the specified resource {@code path}.
	 * @param featureName the name of the feature to associate with the
	 * generated resource
	 * @param targetPath the target path
	 * @param content a {@link CharSequence} of the resource content
	 * @return the newly generated resource reference
	 */
	public ResourceReference addForFeature(String featureName,
			String targetPath, CharSequence content) {

		Assert.notNull(content, "'content' must not be null");
		return addForFeature(featureName, targetPath, appendable -> appendable.append(content));
	}

	/**
	 * Add a new generated resource for the specified {@code featureName}
	 * targeting the specified {@code component}.
	 * @param featureName the name of the feature to associate with the
	 * generated resource
	 * @param targetPath the target path
	 * @param content a {@link Consumer} used to build the resource content
	 * @return the newly generated resource reference
	 */
	public ResourceReference addForFeature(String featureName,
			String targetPath, ThrowingConsumer<Appendable> content) {

		Assert.notNull(content, "'content' must not be null");
		return addForFeature(featureName, targetPath, new AppendableConsumerInputStreamSource(content));
	}

	/**
	 * Add a new generated resource for the specified {@code featureName}
	 * targeting the specified {@code component}.
	 * @param featureName the name of the feature to associate with the
	 * generated resource
	 * @param targetPath the target path
	 * @param content an {@link InputStreamSource} used to build the resource
	 * content
	 * @return the newly generated resource reference
	 */
	public ResourceReference addForFeature(String featureName,
			String targetPath, InputStreamSource content) {

		Assert.hasLength(featureName, "'featureName' must not be empty");
		Assert.notNull(targetPath, "'targetPath' must not be null");
		Assert.notNull(content, "'content' must not be null");
		return createAndAddGeneratedResource(featureName, targetPath, content);
	}

	private ResourceReference createAndAddGeneratedResource(String featureName,
			String path, InputStreamSource inputStreamSource) {

		String fullPath = generateFullPath(featureName, path);
		GeneratedResource generatedResource = new GeneratedResource(fullPath, inputStreamSource);
		this.resources.add(generatedResource);
		return new DefaultResourceReference(generatedResource.path());
	}

	private String generateFullPath(String featureName, String path) {
		return this.featureRootPath + File.separator +
				featureName + File.separator + path;
	}


	/**
	 * Write the {@link GeneratedResource generated resources} using the given
	 * {@link GeneratedFiles} instance.
	 * @param generatedFiles where to write the generated resources
	 */
	void writeTo(GeneratedFiles generatedFiles) {
		Assert.notNull(generatedFiles, "'generatedFiles' must not be null");
		List<GeneratedResource> generatedResources = new ArrayList<>(this.resources);
		generatedResources.sort(Comparator.comparing(GeneratedResource::path));
		for (GeneratedResource generatedResource : generatedResources) {
			generatedFiles.addResourceFile(generatedResource.path(), generatedResource.content());
		}
	}

	/**
	 * Create a new {@link GeneratedResources} instance using the specified name
	 * to qualify generated resource names for a dedicated round of code
	 * generation. The provided name is {@link StringUtils#uncapitalize(String)
	 * uncapitalized} and used as the root directory for subsequently created
	 * resources.
	 * @param resourceRootName the name to use as a root for resource path
	 * @return a new instance for the specified feature name prefix
	 */
	GeneratedResources withResourceRootName(String resourceRootName) {
		return new GeneratedResources(StringUtils.uncapitalize(resourceRootName), this.resources);
	}

	private record GeneratedResource(String path, InputStreamSource content) {
	}

}

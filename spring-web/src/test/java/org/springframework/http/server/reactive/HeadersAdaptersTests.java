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

package org.springframework.http.server.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.undertow.util.HeaderMap;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.tomcat.util.http.MimeHeaders;
import org.eclipse.jetty.http.HttpFields;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.support.HttpComponentsHeadersAdapter;
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.http.support.Netty4HeadersAdapter;
import org.springframework.http.support.Netty5HeadersAdapter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

/**
 * Tests for {@code HeadersAdapters} {@code MultiValueMap} implementations.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @author Simon Baslé
 */
class HeadersAdaptersTests {

	private void addMultipleCaseHeaders(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("SecondHeader", "value");
		headers.add("TestHEADER", "second");
		headers.add("TestHeader", "third");
	}

	@ParameterizedHeadersTest
	void sizeIsCaseInsensitive(MultiValueMap<String, String> headers) {
		addMultipleCaseHeaders(headers);
		assertThat(headers.get("testheader")).as("get(testheader)").containsExactly("first", "second", "third");
		assertThat(headers.size()).isEqualTo(2);
	}

	@ParameterizedHeadersTest
	void asSingleValueMapIsCaseInsensitive(MultiValueMap<String, String> headers) {
		addMultipleCaseHeaders(headers);
		assertThat(headers.asSingleValueMap()).as("asSingleValueMap")
				.hasSize(2)
				.containsEntry("TestHeader", "first")
				.containsEntry("SecondHeader", "value");
	}

	@ParameterizedHeadersTest
	void toSingleValueMapIsCaseInsensitive(MultiValueMap<String, String> headers) {
		addMultipleCaseHeaders(headers);
		assertThat(headers.toSingleValueMap()).as("toSingleValueMap")
				.containsEntry("TestHeader", "first")
				.containsEntry("SecondHeader", "value")
				.hasSize(2);
	}

	@ParameterizedHeadersTest
	void keySetIsCaseInsensitive(MultiValueMap<String, String> headers) {
		addMultipleCaseHeaders(headers);
		assertThat(headers.keySet()).as("keySet")
				.containsExactlyInAnyOrder("TestHeader", "SecondHeader")
				.hasSize(2);
	}

	@ParameterizedHeadersTest
	void shouldRemoveCaseInsensitiveFromKeySet(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("TestHEADER", "second");
		headers.add("TestHeader", "third");

		Iterator<String> iterator = headers.keySet().iterator();
		iterator.next();
		iterator.remove();

		assertThat(headers)
				.doesNotContainKey("TestHeader")
				.doesNotContainKey("TestHEADER")
				.doesNotContainKey("testheader")
				.hasSize(0);
	}

	@ParameterizedHeadersTest
	void valuesIsCaseInsensitive(MultiValueMap<String, String> headers) {
		addMultipleCaseHeaders(headers);
		assertThat(headers.values()).as("values")
				.anySatisfy(values -> assertThat(values).containsExactly("first", "second", "third"))
				.anySatisfy(values -> assertThat(values).containsExactly("value"))
				.hasSize(2);
	}

	@ParameterizedHeadersTest
	void entrySetIsCaseInsensitive(MultiValueMap<String, String> headers) {
		addMultipleCaseHeaders(headers);
		assertThat(headers.entrySet()).as("entrySet")
				.anySatisfy(e -> {
					assertThat(e.getKey()).isEqualToIgnoringCase("TestHeader");
					assertThat(e.getValue()).containsExactly("first", "second", "third");
				})
				.anySatisfy(e -> {
					assertThat(e.getKey()).isEqualTo("SecondHeader");
					assertThat(e.getValue()).containsExactly("value");
				})
				.hasSize(2);
	}

	@ParameterizedHeadersTest // gh-33730
	void copyAllFromEntrySetShouldNotCreateDuplicateValues(MultiValueMap<String, String> headers) {
		addMultipleCaseHeaders(headers);

		HttpHeaders headers2 = new HttpHeaders();
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			headers2.addAll(entry.getKey(), entry.getValue());
		}

		assertThat(headers2.get("TestHeader")).as("TestHeader")
				.containsExactly("first", "second", "third");
	}

	@ParameterizedHeadersTest
	void getWithUnknownHeaderShouldReturnNull(MultiValueMap<String, String> headers) {
		assertThat(headers.get("Unknown")).isNull();
	}

	@ParameterizedHeadersTest
	void getFirstWithUnknownHeaderShouldReturnNull(MultiValueMap<String, String> headers) {
		assertThat(headers.getFirst("Unknown")).isNull();
	}

	@ParameterizedHeadersTest
	void sizeWithMultipleValuesForHeaderShouldCountHeaders(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("TestHeader", "second");
		assertThat(headers).hasSize(1);
	}

	@ParameterizedHeadersTest
	void keySetShouldNotDuplicateHeaderNames(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("OtherHeader", "test");
		headers.add("TestHeader", "second");
		assertThat(headers.keySet()).hasSize(2);
	}

	@ParameterizedHeadersTest
	void containsKeyShouldBeCaseInsensitive(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		assertThat(headers.containsKey("testheader")).isTrue();
	}

	@ParameterizedHeadersTest
	void addShouldKeepOrdering(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.add("TestHeader", "second");
		assertThat(headers.getFirst("TestHeader")).isEqualTo("first");
		assertThat(headers.get("TestHeader")).first().isEqualTo("first");
	}

	@ParameterizedHeadersTest
	void putShouldOverrideExisting(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		headers.put("TestHeader", List.of("override"));
		assertThat(headers.getFirst("TestHeader")).isEqualTo("override");
		assertThat(headers.get("TestHeader")).hasSize(1);
	}

	@ParameterizedHeadersTest
	void nullValuesShouldNotFail(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", null);
		assertThat(headers.getFirst("TestHeader")).isNull();
		headers.set("TestHeader", null);
		assertThat(headers.getFirst("TestHeader")).isNull();
	}

	@ParameterizedHeadersTest
	void shouldReflectChangesOnKeyset(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		assertThat(headers.keySet()).hasSize(1);
		headers.keySet().removeIf("TestHeader"::equals);
		assertThat(headers.keySet()).isEmpty();
	}

	@ParameterizedHeadersTest
	void shouldFailIfHeaderRemovedFromKeyset(MultiValueMap<String, String> headers) {
		headers.add("TestHeader", "first");
		assertThat(headers.keySet()).hasSize(1);
		Iterator<String> names = headers.keySet().iterator();
		assertThat(names.hasNext()).isTrue();
		assertThat(names.next()).isEqualTo("TestHeader");
		names.remove();
		assertThatThrownBy(names::remove).isInstanceOf(IllegalStateException.class);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest
	@MethodSource("headers")
	@interface ParameterizedHeadersTest {
	}

	static Stream<Arguments> headers() {
		return Stream.of(
				argumentSet("Map", CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH))),
				argumentSet("Netty", new Netty4HeadersAdapter(new DefaultHttpHeaders())),
				argumentSet("Netty5", new Netty5HeadersAdapter(io.netty5.handler.codec.http.headers.HttpHeaders.newHeaders())),
				argumentSet("Tomcat", new TomcatHeadersAdapter(new MimeHeaders())),
				argumentSet("Undertow", new UndertowHeadersAdapter(new HeaderMap())),
				argumentSet("Jetty", new JettyHeadersAdapter(HttpFields.build())),
				argumentSet("HttpComponents", new HttpComponentsHeadersAdapter(new HttpGet("https://example.com")))
		);
	}

}

package org.araqnid.stuff.test.integration;

import static org.araqnid.stuff.test.integration.HttpClientMatchers.ok;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithContent;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithJsonContent;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonAny;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonObject;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.araqnid.stuff.test.integration.HttpClientMatchers.HttpContentMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.TreeNode;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.io.CharSource;

public class InfoResourcesIntegrationTest extends IntegrationTest {
	@Test
	public void version_resource_default_is_json() throws Exception {
		try (CloseableHttpResponse response = doGet("/_api/info/version")) {
			assertThat(response, is(allOf(ok(), responseWithJsonContent(jsonAny()))));
		}
	}

	@Test
	public void version_resource_as_json() throws Exception {
		try (CloseableHttpResponse response = doGetWithHeaders("/_api/info/version",
				ImmutableMultimap.of("Accept", "application/json"))) {
			assertThat(
					response,
					is(allOf(
							ok(),
							responseWithJsonContent(jsonObject()
									.withProperty("title", Matchers.notNullValue(TreeNode.class))
									.withProperty("vendor", Matchers.notNullValue(TreeNode.class))
									.withProperty("version", Matchers.notNullValue(TreeNode.class))))));
		}
	}

	@Test
	public void version_resource_as_plain_text() throws Exception {
		try (CloseableHttpResponse response = doGetWithHeaders("/_api/info/version",
				ImmutableMultimap.of("Accept", "text/plain"))) {
			assertThat(response,
					is(allOf(ok(), either(responseWithTextContent(any(String.class))).or(responseWithNullEntity()))));
		}
	}

	@Test
	@Ignore
	public void health_resource() throws Exception {
		try (CloseableHttpResponse response = doGet("/_api/info/health")) {
			assertThat(response, is(allOf(ok(), responseWithJsonContent(jsonString(any(String.class))))));
		}
	}

	@Test
	public void state_resource_default_is_json() throws Exception {
		try (CloseableHttpResponse response = doGet("/_api/info/state")) {
			assertThat(response, is(allOf(ok(), responseWithJsonContent(jsonAny()))));
		}
	}

	@Test
	public void state_resource_as_json() throws Exception {
		try (CloseableHttpResponse response = doGetWithHeaders("/_api/info/state",
				ImmutableMultimap.of("Accept", "application/json"))) {
			assertThat(response, is(allOf(ok(), responseWithJsonContent(jsonString(any(String.class))))));
		}
	}

	@Test
	public void state_resource_as_plain_text() throws Exception {
		try (CloseableHttpResponse response = doGetWithHeaders("/_api/info/state",
				ImmutableMultimap.of("Accept", "text/plain"))) {
			assertThat(response, is(allOf(ok(), responseWithTextContent(any(String.class)))));
		}
	}

	@Test
	public void routing_resource_default_is_json() throws Exception {
		try (CloseableHttpResponse response = doGet("/_api/info/routing")) {
			assertThat(response, is(allOf(ok(), responseWithJsonContent(jsonAny()))));
		}
	}

	@Test
	public void routing_resource_as_json() throws Exception {
		try (CloseableHttpResponse response = doGetWithHeaders("/_api/info/routing",
				ImmutableMultimap.of("Accept", "application/json"))) {
			assertThat(response, is(allOf(ok(), responseWithJsonContent(jsonAny()))));
		}
	}

	@Test
	public void routing_resource_as_plain_text() throws Exception {
		try (CloseableHttpResponse response = doGetWithHeaders("/_api/info/routing",
				ImmutableMultimap.of("Accept", "text/plain"))) {
			assertThat(response, is(allOf(ok(), responseWithTextContent(any(String.class)))));
		}
	}

	private static Matcher<HttpResponse> responseWithNullEntity() {
		return new TypeSafeDiagnosingMatcher<HttpResponse>() {
			@Override
			protected boolean matchesSafely(HttpResponse item, Description mismatchDescription) {
				if (item.getEntity() != null) {
					mismatchDescription.appendText("entity was ").appendValue(item.getEntity());
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("response with null entity");
			}
		};
	}

	public static Matcher<HttpResponse> responseWithTextContent(final Matcher<String> contentMatcher) {
		return responseWithContent(new HttpContentMatcher<String>(equalTo("text/plain"), contentMatcher) {
			@Override
			protected String doParse(final HttpEntity item) throws IOException {
				return new CharSource() {
					@Override
					public Reader openStream() throws IOException {
						return new InputStreamReader(item.getContent(), Charset.forName("UTF-8"));
					}
				}.read();
			}
		});
	}
}

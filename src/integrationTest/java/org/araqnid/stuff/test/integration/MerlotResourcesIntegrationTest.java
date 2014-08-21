package org.araqnid.stuff.test.integration;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import static org.araqnid.stuff.test.integration.ServerIntegrationTest.headerWithValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class MerlotResourcesIntegrationTest {
	private CloseableHttpClient httpClient;
	private ServerRunner server = new ServerRunner();

	@Before
	public void startServer() throws Exception {
		server.start();
	}

	@After
	public void stopServer() throws Exception {
		server.stop();
	}

	@Before
	public void setUp() throws Exception {
		httpClient = HttpClientBuilder.create().build();
	}

	@After
	public void tearDown() throws Exception {
		if (httpClient != null) httpClient.close();
	}

	@Test
	public void status_resource_with_no_cookie() throws Exception {
		CloseableHttpResponse response = doGet("/_api/merlot/");
		assertThat(response.getStatusLine(), is(ok()));
		assertThat(
				response,
				is(responseWithJsonContent(jsonObject()
						.withProperty("userInfo", jsonNull())
						.withProperty("version", jsonNull()))));
		response.close();
	}

	@Ignore
	@Test
	public void status_resource_with_auth_cookie() throws Exception {
		String userId = UUID.randomUUID().toString();
		String userCN = randomString("User");
		String username = randomEmailAddress();
		// TODO init db with user data
		CloseableHttpResponse response = doGet("/_api/merlot/", ImmutableMap.of("Cookie", "ATKT=" + authTicket(userId)));
		assertThat(response.getStatusLine(), is(ok()));
		assertThat(
				response,
				is(responseWithJsonContent(jsonObject().withProperty("userInfo",
						jsonObject().withProperty("commonName", jsonString(userCN)).withProperty("username", jsonString(username))).withProperty(
						"version", jsonNull()))));
		response.close();
	}

	@Ignore
	@Test
	public void status_resource_with_invalid_auth_cookie() throws Exception {
		CloseableHttpResponse response = doGet("/_api/merlot/", ImmutableMap.of("Cookie", "ATKT=xyzzy"));
		assertThat(response.getStatusLine(), is(forbidden()));
		response.close();
	}

	private CloseableHttpResponse doGet(String path, Map<String, String> headers) throws IOException {
		HttpUriRequest request = new HttpGet(server.appUri(path));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}
		return httpClient.execute(request);
	}

	private CloseableHttpResponse doGet(String path) throws IOException {
		return doGet(path, Collections.<String, String> emptyMap());
	}

	public static Matcher<StatusLine> ok() {
		return new TypeSafeDiagnosingMatcher<StatusLine>() {
			@Override
			protected boolean matchesSafely(StatusLine item, Description mismatchDescription) {
				if (item.getStatusCode() != HttpServletResponse.SC_OK) {
					mismatchDescription.appendText("status is ").appendValue(item);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("status is ").appendValue(HttpServletResponse.SC_OK);
			}
		};
	}

	public static Matcher<StatusLine> forbidden() {
		return new TypeSafeDiagnosingMatcher<StatusLine>() {
			@Override
			protected boolean matchesSafely(StatusLine item, Description mismatchDescription) {
				if (item.getStatusCode() != HttpServletResponse.SC_FORBIDDEN) {
					mismatchDescription.appendText("status is ").appendValue(item);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("status is ").appendValue(HttpServletResponse.SC_OK);
			}
		};
	}

	public static Matcher<HttpResponse> responseWithJsonContent(final Matcher<? extends TreeNode> contentMatcher) {
		return new TypeSafeDiagnosingMatcher<HttpResponse>() {
			private TreeNode document;
			private IOException parseException;

			@Override
			protected boolean matchesSafely(HttpResponse response, Description mismatchDescription) {
				assertThat(response.getEntity().getContentType(), is(headerWithValue(equalTo("application/json"))));
				try {
					parse(response);
				} catch (IOException e) {
					mismatchDescription.appendText("response contains invalid JSON: ").appendValue(e);
					return false;
				}
				if (!contentMatcher.matches(document)) {
					mismatchDescription.appendText("in response document, ");
					contentMatcher.describeMismatch(document, mismatchDescription);
					return false;
				}
				return true;
			}

			private void parse(HttpResponse response) throws IOException {
				if (document != null) return;
				if (parseException != null) throw parseException;
				try {
					document = new MappingJsonFactory().createParser(response.getEntity().getContent())
							.readValueAsTree();
				} catch (IOException e) {
					parseException = e;
					throw e;
				}
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("response with JSON content: ").appendDescriptionOf(contentMatcher);
			}
		};
	}

	public static ObjectNodeMatcher jsonObject() {
		return new ObjectNodeMatcher();
	}

	public static class ObjectNodeMatcher extends TypeSafeDiagnosingMatcher<ObjectNode> {
		private final Map<String, Matcher<? extends TreeNode>> propertyMatchers = new LinkedHashMap<>();

		@Override
		protected boolean matchesSafely(ObjectNode item, Description mismatchDescription) {
			Set<String> remainingFieldNames = Sets.newHashSet(item.fieldNames());
			for (Map.Entry<String, Matcher<? extends TreeNode>> e : propertyMatchers.entrySet()) {
				TreeNode value = item.get(e.getKey());
				if (!e.getValue().matches(value)) {
					mismatchDescription.appendText(e.getKey()).appendText(": ");
					e.getValue().describeMismatch(value, mismatchDescription);
					return false;
				}
				remainingFieldNames.remove(e.getKey());
			}
			if (!remainingFieldNames.isEmpty()) {
				mismatchDescription.appendText("unexpected properties: ").appendValue(remainingFieldNames);
				return false;
			}
			return true;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("JSON object { ");
			boolean first = true;
			for (Map.Entry<String, Matcher<? extends TreeNode>> e : propertyMatchers.entrySet()) {
				if (first) {
					first = false;
				}
				else {
					description.appendText(", ");
				}
				description.appendValue(e.getKey()).appendText(": ").appendDescriptionOf(e.getValue());
			}
			description.appendText(" }");
		}

		public ObjectNodeMatcher withProperty(String key, Matcher<? extends TreeNode> value) {
			propertyMatchers.put(key, value);
			return this;
		}
	}

	public static Matcher<TextNode> jsonString(String value) {
		return jsonString(equalTo(value));
	}

	public static Matcher<TextNode> jsonString(final Matcher<String> valueMatcher) {
		return new TypeSafeDiagnosingMatcher<TextNode>() {
			@Override
			protected boolean matchesSafely(TextNode item, Description mismatchDescription) {
				if (!valueMatcher.matches(item.asText())) {
					mismatchDescription.appendText("text was: ").appendValue(item.asText());
					return false;
				}
				return true;
			}
			
			@Override
			public void describeTo(Description description) {
				description.appendText("JSON text ").appendDescriptionOf(valueMatcher);
			}
		};
	}

	public static Matcher<TreeNode> jsonAny() {
		return new TypeSafeDiagnosingMatcher<TreeNode>() {
			@Override
			protected boolean matchesSafely(TreeNode item, Description mismatchDescription) {
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("JSON");
			}
		};
	}

	public static Matcher<NullNode> jsonNull() {
		return new TypeSafeDiagnosingMatcher<NullNode>() {
			@Override
			protected boolean matchesSafely(NullNode item, Description mismatchDescription) {
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("JSON null");
			}
		};
	}

	private static String authTicket(String userId) {
		return "u" + userId + ",x00000000";
	}

	private static String randomEmailAddress() {
		List<String> tlds = ImmutableList.of("com", "net", "org", "co.uk", "org.uk");
		String tld = tlds.get(new Random().nextInt(tlds.size()));
		return randomString() + "@" + randomString() + ".example." + tld;
	}

	private static String randomString(String prefix) {
		return prefix + "-" + randomString();
	}

	private static String randomString() {
		Random random = new Random();
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		int len = 10;
		StringBuilder builder = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
		}
		return builder.toString();
	}
}

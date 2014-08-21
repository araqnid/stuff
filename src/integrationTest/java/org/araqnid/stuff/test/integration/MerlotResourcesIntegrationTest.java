package org.araqnid.stuff.test.integration;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
	public void status_resource() throws Exception {
		CloseableHttpResponse response = doGet("/_api/merlot/");
		assertThat(response.getStatusLine(), is(ok()));
		assertThat(
				response,
				is(responseWithJsonContent(jsonObject()
						.withProperty("userInfo", Matchers.notNullValue(TreeNode.class))
						.withProperty("version", Matchers.notNullValue(TreeNode.class)))));
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
		private final Map<String, Matcher<TreeNode>> propertyMatchers = new LinkedHashMap<>();

		@Override
		protected boolean matchesSafely(ObjectNode item, Description mismatchDescription) {
			Set<String> remainingFieldNames = Sets.newHashSet(item.fieldNames());
			for (Map.Entry<String, Matcher<TreeNode>> e : propertyMatchers.entrySet()) {
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
			for (Map.Entry<String, Matcher<TreeNode>> e : propertyMatchers.entrySet()) {
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

		public ObjectNodeMatcher withProperty(String key, Matcher<TreeNode> value) {
			propertyMatchers.put(key, value);
			return this;
		}
	}
}

package org.araqnid.stuff;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.araqnid.stuff.CollectActivityEvents.ActivityEventRecord;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class ServerIntegrationTest {
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
	public void ruid_generated_and_returned_in_http_response() throws Exception {
		CloseableHttpResponse response = doGet("/");
		Header ruidHeader = response.getFirstHeader("X-RUID");
		MatcherAssert.assertThat(ruidHeader, is(headerWithValue(likeAUUID())));
		response.close();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void request_activity_emitted() throws Exception {
		doGet("/").close();
		MatcherAssert.assertThat(server.activityEvents(),
				contains(beginRequestRecord(equalTo("HttpRequest")), finishRequestRecord(equalTo("HttpRequest"))));
	}

	@Test
	public void ruid_echoed_from_http_request() throws Exception {
		String ourRuid = UUID.randomUUID().toString();
		CloseableHttpResponse response = doGet("/", ImmutableMap.of("X-RUID", ourRuid));
		Header ruidHeader = response.getFirstHeader("X-RUID");
		MatcherAssert.assertThat(ruidHeader, is(headerWithValue(equalTo(ourRuid))));
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

	public Matcher<Header> headerWithValue(final Matcher<String> valueMatcher) {
		return new TypeSafeDiagnosingMatcher<Header>() {
			@Override
			protected boolean matchesSafely(Header item, Description mismatchDescription) {
				String value = item.getValue();
				if (!valueMatcher.matches(value)) {
					mismatchDescription.appendText("header value ");
					valueMatcher.describeMismatch(value, mismatchDescription);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Header with value ").appendDescriptionOf(valueMatcher);
			}
		};
	}

	public Matcher<String> likeAUUID() {
		return new TypeSafeDiagnosingMatcher<String>() {
			private final Pattern pattern = Pattern
					.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

			@Override
			protected boolean matchesSafely(String item, Description mismatchDescription) {
				if (!pattern.matcher(item).matches()) {
					mismatchDescription.appendText("does not look like a UUID: ").appendValue(item);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("like a UUID");
			}
		};
	}

	public static Matcher<CollectActivityEvents.ActivityEventRecord> beginRequestRecord(
			final Matcher<String> requestType) {
		return new TypeSafeDiagnosingMatcher<CollectActivityEvents.ActivityEventRecord>() {
			@Override
			protected boolean matchesSafely(ActivityEventRecord item, Description mismatchDescription) {
				if (!item.method.equals("beginRequest")) {
					mismatchDescription.appendText("record class is ").appendValue(item.method);
					return false;
				}
				if (!requestType.matches(item.type)) {
					mismatchDescription.appendText("type is ").appendValue(item.type);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("beginRequest with type ").appendDescriptionOf(requestType);
			}
		};
	}

	public static Matcher<CollectActivityEvents.ActivityEventRecord> finishRequestRecord(
			final Matcher<String> requestType) {
		return new TypeSafeDiagnosingMatcher<CollectActivityEvents.ActivityEventRecord>() {
			@Override
			protected boolean matchesSafely(ActivityEventRecord item, Description mismatchDescription) {
				if (!item.method.equals("finishRequest")) {
					mismatchDescription.appendText("record class is ").appendValue(item.method);
					return false;
				}
				if (!requestType.matches(item.type)) {
					mismatchDescription.appendText("type is ").appendValue(item.type);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("finishRequest with type ").appendDescriptionOf(requestType);
			}
		};
	}
}

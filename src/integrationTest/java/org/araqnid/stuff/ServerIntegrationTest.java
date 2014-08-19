package org.araqnid.stuff;

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
import org.araqnid.stuff.config.AppConfig;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

public class ServerIntegrationTest {
	private Server server;
	private Injector injector;
	private int port;
	private String baseUri;
	private CloseableHttpClient httpClient;

	@Before
	public void setUp() throws Exception {
		injector = Guice.createInjector(Modules.override(new AppConfig()).with(new AbstractModule() {
			@Override
			protected void configure() {
			}

			@Singleton
			@Provides
			public Connector connector() {
				return new SelectChannelConnector();
			}
		}));
		server = injector.getInstance(Server.class);
		server.start();
		port = injector.getInstance(Connector.class).getLocalPort();
		baseUri = String.format("http://localhost:%d", port);
		httpClient = HttpClientBuilder.create().build();
	}

	@After
	public void tearDown() throws Exception {
		if (server != null) server.stop();
		if (httpClient != null) httpClient.close();
	}

	@Test
	public void ruid_generated_and_returned_in_http_response() throws Exception {
		CloseableHttpResponse response = doGet("/");
		Header ruidHeader = response.getFirstHeader("X-RUID");
		MatcherAssert.assertThat(ruidHeader, is(headerWithValue(likeAUUID())));
		response.close();
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
		Preconditions.checkArgument(path.startsWith("/"));
		HttpUriRequest request = new HttpGet(baseUri + path);
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}
		return httpClient.execute(request);
	}

	private CloseableHttpResponse doGet(String path) throws IOException {
		return doGet(path, Collections.<String, String> emptyMap());
	}

	private Matcher<Header> headerWithValue(final Matcher<String> valueMatcher) {
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

	private Matcher<String> likeAUUID() {
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
}

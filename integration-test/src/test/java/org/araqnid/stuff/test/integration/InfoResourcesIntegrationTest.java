package org.araqnid.stuff.test.integration;

import java.io.IOException;

import com.google.common.collect.ImmutableMultimap;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.araqnid.stuff.test.integration.HttpClientMatchers.HttpContentMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.araqnid.stuff.matchers.JsonStructureMatchers.jsonAny;
import static org.araqnid.stuff.matchers.JsonStructureMatchers.jsonObject;
import static org.araqnid.stuff.matchers.JsonStructureMatchers.jsonString;
import static org.araqnid.stuff.matchers.XmlMatchers.textAtXpath;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.ok;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithContent;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithJsonContent;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithXmlContent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class InfoResourcesIntegrationTest {
	@Rule
	public final ServerRunner server = new ServerRunner();

	@Test
	public void version_resource_as_json() throws Exception {
		try (CloseableHttpResponse response = server.doGetWithHeaders("/_api/info/version", ImmutableMultimap.of("Accept", "application/json"))) {
			assertThat(
					response,
					is(allOf(
							ok(),
							responseWithJsonContent(jsonObject().withProperty("title", jsonAny())
									.withProperty("vendor", jsonAny()).withProperty("version", jsonAny())))));
		}
	}

	@Test
	public void version_resource_as_xml() throws Exception {
		try (CloseableHttpResponse response = server.doGetWithHeaders("/_api/info/version", ImmutableMultimap.of("Accept", "application/xml"))) {
			assertThat(
					response,
					is(allOf(
							ok(),
							responseWithXmlContent(allOf(textAtXpath("/app-version/title", is(any(String.class))),
									textAtXpath("/app-version/vendor", is(any(String.class))),
									textAtXpath("/app-version/version", is(any(String.class))))))));
		}
	}

	@Test
	public void version_resource_as_plain_text() throws Exception {
		try (CloseableHttpResponse response = server.doGetWithHeaders("/_api/info/version", ImmutableMultimap.of("Accept", "text/plain"))) {
			assertThat(response,
					is(allOf(ok(), either(responseWithTextContent(any(String.class))).or(responseWithNullEntity()))));
		}
	}

	@Test
	@Ignore
	public void health_resource() throws Exception {
		try (CloseableHttpResponse response = server.doGet("/_api/info/health")) {
			assertThat(response, is(allOf(ok(), responseWithJsonContent(jsonString(any(String.class))))));
		}
	}

	@Test
	public void routing_resource_as_json() throws Exception {
		try (CloseableHttpResponse response = server.doGetWithHeaders("/_api/info/routing", ImmutableMultimap.of("Accept", "application/json"))) {
			assertThat(response, is(allOf(ok(), responseWithJsonContent(jsonAny()))));
		}
	}

	@Test
	public void routing_resource_as_plain_text() throws Exception {
		try (CloseableHttpResponse response = server.doGetWithHeaders("/_api/info/routing", ImmutableMultimap.of("Accept", "text/plain"))) {
			assertThat(response, is(allOf(ok(), responseWithTextContent(any(String.class)))));
		}
	}

	@Test
	public void routing_resource_as_html() throws Exception {
		try (CloseableHttpResponse response = server.doGetWithHeaders("/_api/info/routing", ImmutableMultimap.of("Accept", "text/html"))) {
			assertThat(response, is(allOf(ok(), responseWithHtmlContent(any(String.class)))));
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
				return EntityUtils.toString(item);
			}
		});
	}

	public static Matcher<HttpResponse> responseWithHtmlContent(final Matcher<String> contentMatcher) {
		return responseWithContent(new HttpContentMatcher<String>(equalTo("text/html"), contentMatcher) {
			@Override
			protected String doParse(final HttpEntity item) throws IOException {
				return EntityUtils.toString(item);
			}
		});
	}
}

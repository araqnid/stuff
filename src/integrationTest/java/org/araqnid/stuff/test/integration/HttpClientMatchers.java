package org.araqnid.stuff.test.integration;

import static org.araqnid.stuff.test.integration.ServerIntegrationTest.headerWithValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public final class HttpClientMatchers {
	private HttpClientMatchers() {
	}

	@Factory
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

	@Factory
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

	@Factory
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

	@Factory
	public static Matcher<Header> newCookie(final String name, final Matcher<String> valueMatcher) {
		return new TypeSafeDiagnosingMatcher<Header>() {
			@Override
			protected boolean matchesSafely(Header item, Description mismatchDescription) {
				if (!item.getName().equalsIgnoreCase("set-cookie")) {
					mismatchDescription.appendText("Header name was not 'Set-Cookie'");
					return false;
				}
				Pattern pattern = Pattern.compile("([^=;]+)=([^=;]+)");
				java.util.regex.Matcher matcher = pattern.matcher(item.getValue());
				if (!matcher.lookingAt()) {
					mismatchDescription.appendText("Header value does not look like a cookie setting: ").appendText(
							item.getValue());
					return false;
				}
				String cookieName = matcher.group(1);
				String cookieValue = matcher.group(2);
				if (!cookieName.equals(name)) {
					mismatchDescription.appendText("cookie name was ").appendValue(cookieName);
					return false;
				}
				if (!valueMatcher.matches(cookieValue)) {
					mismatchDescription.appendText("cookie value ");
					valueMatcher.describeMismatch(cookieValue, mismatchDescription);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Set cookie ").appendValue(name).appendText(" to ")
						.appendDescriptionOf(valueMatcher);
			}
		};
	}

	@Factory
	public static Matcher<Header> removeCookie(final String name) {
		return new TypeSafeDiagnosingMatcher<Header>() {
			@Override
			protected boolean matchesSafely(Header item, Description mismatchDescription) {
				if (!item.getName().equalsIgnoreCase("set-cookie")) {
					mismatchDescription.appendText("Header name was not 'Set-Cookie'");
					return false;
				}
				Pattern pattern = Pattern.compile("([^=;]+)=([^=;]+)");
				java.util.regex.Matcher matcher = pattern.matcher(item.getValue());
				if (!matcher.lookingAt()) {
					mismatchDescription.appendText("Header value does not look like a cookie setting: ").appendText(
							item.getValue());
					return false;
				}
				// TODO
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Remove cookie ").appendValue(name);
			}
		};
	}
}

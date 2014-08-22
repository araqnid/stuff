package org.araqnid.stuff.test.integration;

import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;

public final class HttpClientMatchers {
	private HttpClientMatchers() {
	}

	@Factory
	public static Matcher<HttpResponse> ok() {
		return responseWith(statusOk());
	}

	@Factory
	public static Matcher<HttpResponse> forbidden() {
		return responseWith(statusForbidden());
	}

	@Factory
	public static Matcher<HttpResponse> responseWith(final Matcher<StatusLine> statusMatcher) {
		return new TypeSafeDiagnosingMatcher<HttpResponse>() {
			@Override
			protected boolean matchesSafely(HttpResponse item, Description mismatchDescription) {
				if (!statusMatcher.matches(item.getStatusLine())) {
					mismatchDescription.appendText("response status ");
					statusMatcher.describeMismatch(item.getStatusLine(), mismatchDescription);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("response with status ").appendDescriptionOf(statusMatcher);
			}
		};
	}

	@Factory
	public static Matcher<StatusLine> statusOk() {
		return statusIs(HttpStatus.SC_OK);
	}

	@Factory
	public static Matcher<StatusLine> statusForbidden() {
		return statusIs(HttpStatus.SC_FORBIDDEN);
	}

	@Factory
	public static Matcher<StatusLine> statusIs(final int sc) {
		return new TypeSafeDiagnosingMatcher<StatusLine>() {
			@Override
			protected boolean matchesSafely(StatusLine item, Description mismatchDescription) {
				if (item.getStatusCode() != sc) {
					mismatchDescription.appendText("status line is ").appendValue(item);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("HTTP status ").appendValue(sc);
			}
		};
	}

	public static abstract class HttpContentMatcher<T> extends TypeSafeDiagnosingMatcher<HttpEntity> {
		private final Matcher<String> contentTypeMatcher;
		private final Matcher<T> contentMatcher;
		private boolean parsed;
		private T parsedValue;
		private Exception parseException;

		public HttpContentMatcher(Matcher<String> contentTypeMatcher, Matcher<T> contentMatcher) {
			this.contentTypeMatcher = contentTypeMatcher;
			this.contentMatcher = contentMatcher;
		}

		@Override
		protected boolean matchesSafely(HttpEntity item, Description mismatchDescription) {
			ContentType contentType = ContentType.parse(item.getContentType().getValue());
			if (!contentTypeMatcher.matches(contentType.getMimeType())) {
				mismatchDescription.appendText("content type ");
				contentTypeMatcher.describeMismatch(contentType.getMimeType(), mismatchDescription);
				return false;
			}
			T value = parse(item);
			if (!contentMatcher.matches(value)) {
				mismatchDescription.appendText("content ");
				contentMatcher.describeMismatch(value, mismatchDescription);
				return false;
			}
			return true;
		}

		protected T parse(HttpEntity item) {
			if (parsed) {
				if (parseException != null) throw new AssertionError("Failed to parse content", parseException);
				return parsedValue;
			}
			try {
				parsedValue = doParse(item);
				return parsedValue;
			} catch (Exception e) {
				parseException = e;
				throw new AssertionError("Failed to parse content", e);
			} finally {
				parsed = true;
			}
		}

		protected abstract T doParse(HttpEntity item) throws Exception;

		@Override
		public void describeTo(Description description) {
			description.appendText("type ").appendDescriptionOf(contentTypeMatcher).appendText(": ")
					.appendDescriptionOf(contentMatcher);
		}
	}

	public static Matcher<HttpResponse> responseWithContent(final Matcher<HttpEntity> entityMatcher) {
		return new TypeSafeDiagnosingMatcher<HttpResponse>() {
			@Override
			protected boolean matchesSafely(HttpResponse item, Description mismatchDescription) {
				if (!entityMatcher.matches(item.getEntity())) {
					mismatchDescription.appendText("entity ");
					entityMatcher.describeMismatch(item.getEntity(), mismatchDescription);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("entity ").appendDescriptionOf(entityMatcher);
			}
		};
	}

	@Factory
	public static <T extends TreeNode> Matcher<HttpResponse> responseWithJsonContent(final Matcher<T> contentMatcher) {
		return responseWithContent(new HttpContentMatcher<T>(equalTo("application/json"), contentMatcher) {
			@Override
			protected T doParse(HttpEntity item) throws IOException {
				return new MappingJsonFactory().createParser(item.getContent()).readValueAsTree();
			}
		});
	}

	@Factory
	@SafeVarargs
	public static Matcher<HttpResponse> responseWithCookies(Matcher<? super Header>... matchers) {
		return responseWithCookies(Arrays.asList(matchers));
	}

	@Factory
	public static Matcher<HttpResponse> responseWithCookies(List<Matcher<? super Header>> matchers) {
		return responseWithHeaders("Set-Cookie", matchers);
	}

	@Factory
	public static Matcher<HttpResponse> responseWithHeaders(final String headerName,
			final List<Matcher<? super Header>> matchers) {
		return new TypeSafeDiagnosingMatcher<HttpResponse>() {
			private Matcher<Iterable<? extends Header>> aggregatedMatcher = Matchers.<Header> contains(matchers);

			@Override
			protected boolean matchesSafely(HttpResponse item, Description mismatchDescription) {
				Header[] headers = item.getHeaders(headerName);
				List<Header> headerList = headers != null ? Arrays.asList(headers) : Collections.<Header> emptyList();
				if (!aggregatedMatcher.matches(headerList)) {
					mismatchDescription.appendValue(headerName).appendText(" headers: ");
					aggregatedMatcher.describeMismatch(headerList, mismatchDescription);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("response with ").appendValue(headerName).appendText(" headers: ")
						.appendDescriptionOf(aggregatedMatcher);
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

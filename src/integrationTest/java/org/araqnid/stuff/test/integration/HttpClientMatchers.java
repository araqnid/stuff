package org.araqnid.stuff.test.integration;

import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
		return either(statusIs(HttpStatus.SC_OK)).or(statusIs(HttpStatus.SC_NO_CONTENT));
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
		if (matchers.size() == 1) { return responseWithHeader("Set-Cookie", matchers.get(0)); }
		return responseWithHeaders("Set-Cookie", matchers);
	}

	@Factory
	public static Matcher<HttpResponse> responseWithHeaders(final String headerName,
			final List<Matcher<? super Header>> matchers) {
		return new TypeSafeDiagnosingMatcher<HttpResponse>() {
			private Matcher<Iterable<? extends Header>> aggregatedMatcher = matchers.isEmpty() ? Matchers
					.<Header> emptyIterable() : Matchers.<Header> contains(matchers);

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
	public static Matcher<HttpResponse> responseWithHeader(final String headerName,
			final Matcher<? super Header> headerMatcher) {
		return new TypeSafeDiagnosingMatcher<HttpResponse>() {
			@Override
			protected boolean matchesSafely(HttpResponse item, Description mismatchDescription) {
				Header[] headers = item.getHeaders(headerName);
				if (headers.length == 0) {
					mismatchDescription.appendText("no such header: ").appendValue(headerName);
				}
				if (headers.length == 1) {
					Header header = headers[0];
					if (!headerMatcher.matches(header)) {
						mismatchDescription.appendValue(headerName).appendText(" header ");
						headerMatcher.describeMismatch(header, mismatchDescription);
						return false;
					}
					return true;
				}
				for (Header h : headers) {
					if (headerMatcher.matches(h)) return true;
				}
				mismatchDescription.appendValue(headerName).appendText(" not matched: ")
						.appendDescriptionOf(headerMatcher);
				return false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("with ").appendValue(headerName).appendText(" header ")
						.appendDescriptionOf(headerMatcher);
			}
		};
	}

	@Factory
	public static Matcher<Header> newCookie(final String name, final Matcher<String> valueMatcher) {
		return singleCookie(new TypeSafeDiagnosingMatcher<HttpCookie>() {
			@Override
			protected boolean matchesSafely(HttpCookie item, Description mismatchDescription) {
				if (!item.getName().equals(name)) {
					mismatchDescription.appendText("Cookie name was ").appendValue(item.getName());
					return false;
				}
				if (!valueMatcher.matches(item.getValue())) {
					mismatchDescription.appendText("Cookie ").appendValue(name).appendText(" ");
					valueMatcher.describeMismatch(item.getValue(), mismatchDescription);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Set cookie ").appendValue(name).appendText(" to ")
						.appendDescriptionOf(valueMatcher);
			}
		});
	}

	@Factory
	public static Matcher<Header> removeCookie(final String name) {
		return singleCookie(new TypeSafeDiagnosingMatcher<HttpCookie>() {
			@Override
			protected boolean matchesSafely(HttpCookie item, Description mismatchDescription) {
				if (!item.getName().equals(name)) {
					mismatchDescription.appendText("Cookie was ").appendValue(item);
					return false;
				}
				if (item.getMaxAge() > 0) {
					mismatchDescription.appendText("Cookie ").appendValue(item.getName())
							.appendText(" has positive max-age");
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Remove cookie ").appendValue(name);
			}
		});
	}

	@Factory
	public static Matcher<Header> singleCookie(final Matcher<HttpCookie> cookieMatcher) {
		return new TypeSafeDiagnosingMatcher<Header>() {
			@Override
			protected boolean matchesSafely(Header item, Description mismatchDescription) {
				List<HttpCookie> parsed = HttpCookie.parse(item.toString());
				if (parsed.size() != 1) {
					mismatchDescription.appendText("Didn't get a unique cookie from header: ").appendValue(parsed);
					return false;
				}
				HttpCookie cookie = parsed.get(0);
				if (!cookieMatcher.matches(cookie)) {
					cookieMatcher.describeMismatch(cookie, mismatchDescription);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("single cookie ").appendDescriptionOf(cookieMatcher);
			}
		};
	}

	@Factory
	public static Matcher<Header> headerWithValue(final Matcher<String> valueMatcher) {
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
}

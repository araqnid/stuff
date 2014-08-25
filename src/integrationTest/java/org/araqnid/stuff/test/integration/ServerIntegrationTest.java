package org.araqnid.stuff.test.integration;

import static org.araqnid.stuff.test.integration.CollectActivityEvents.beginRequestRecord;
import static org.araqnid.stuff.test.integration.CollectActivityEvents.finishRequestRecord;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithHeader;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.headerWithValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.stringContainsInOrder;

import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Pattern;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ServerIntegrationTest extends IntegrationTest {
	@Test
	public void ruid_generated_and_returned_in_http_response() throws Exception {
		assertThat(doGet("/"), responseWithHeader("X-RUID", headerWithValue(likeAUUID())));
	}

	@Test
	public void server_identity_in_http_response() throws Exception {
		assertThat(doGet("/"),
				responseWithHeader("X-Server-Identity", headerWithValue(twoParts(any(String.class), likeAUUID()))));
	}

	@Test
	public void request_activity_emitted() throws Exception {
		assertThat(
				server.activityEventsForRuid(doGet("/").getFirstHeader("X-RUID").getValue()),
				includesSubsequence(
						beginRequestRecord(equalTo("HttpRequest")).withDescription(
								stringContainsInOrder(ImmutableList.of("GET", "/"))),
						finishRequestRecord(equalTo("HttpRequest"))));
	}

	@Test
	public void ruid_echoed_from_http_request() throws Exception {
		String ourRuid = UUID.randomUUID().toString();
		assertThat(doGetWithHeaders("/", ImmutableMap.of("X-RUID", ourRuid)),
				responseWithHeader("X-RUID", headerWithValue(equalTo(ourRuid))));
	}

	public static Matcher<String> likeAUUID() {
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

	public static Matcher<String> twoParts(final Matcher<String> first, final Matcher<String> second) {
		return new TypeSafeDiagnosingMatcher<String>() {
			@Override
			protected boolean matchesSafely(String item, Description mismatchDescription) {
				Iterator<String> iterator = Splitter.on(' ').split(item).iterator();
				if (!iterator.hasNext()) {
					mismatchDescription.appendText("No parts in string");
					return false;
				}
				String firstPart = iterator.next();
				if (!first.matches(firstPart)) {
					mismatchDescription.appendText("first part ");
					first.describeMismatch(firstPart, mismatchDescription);
					return false;
				}
				String secondPart = iterator.next();
				if (!second.matches(secondPart)) {
					mismatchDescription.appendText("second part ");
					second.describeMismatch(secondPart, mismatchDescription);
					return false;
				}
				if (iterator.hasNext()) {
					mismatchDescription.appendText("Found more than two parts");
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("two parts: ").appendDescriptionOf(first).appendText(", ")
						.appendDescriptionOf(second);
			}
		};
	}

	@SafeVarargs
	public static <T> Matcher<Iterable<T>> includesSubsequence(Matcher<? super T>... matchers) {
		return includesSubsequence(ImmutableList.copyOf(matchers));
	}

	public static <T> Matcher<Iterable<T>> includesSubsequence(final Iterable<Matcher<? super T>> matchers) {
		return new TypeSafeDiagnosingMatcher<Iterable<T>>() {
			@Override
			protected boolean matchesSafely(Iterable<T> item, Description mismatchDescription) {
				Iterator<T> valueIterator = item.iterator();
				Iterator<Matcher<? super T>> matchIterator = matchers.iterator();
				Matcher<? super T> currentMatcher = matchIterator.next();
				while (true) {
					if (!valueIterator.hasNext()) {
						mismatchDescription.appendText("No match for: ").appendDescriptionOf(currentMatcher);
						return false;
					}
					T value = valueIterator.next();
					if (currentMatcher.matches(value)) {
						if (!matchIterator.hasNext()) { return true; }
						currentMatcher = matchIterator.next();
					}
				}
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("an iterable including ");
				for (Iterator<Matcher<? super T>> iter = matchers.iterator(); iter.hasNext();) {
					description.appendDescriptionOf(iter.next());
					if (iter.hasNext()) {
						description.appendText(", ");
					}
				}
			}
		};
	}
}

package org.araqnid.stuff;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.araqnid.stuff.RequestActivity.ActivityEventSink;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class RequestActivityTest {
	private final String ruid = randomString();
	private final List<EventOutput> activityEventsOutput = new ArrayList<>();
	private final ActivityEventSink activityEventSink = new CaptureEvents(activityEventsOutput);

	@Test
	public void can_begin_a_request() {
		RequestActivity activity = new RequestActivity(ruid, activityEventSink);
		AppRequestType type = randomRequestType();
		String description = randomString();
		activity.beginRequest(type, description);
		MatcherAssert.assertThat(activityEventsOutput, events(
				a_begin_request()
					.with_ruid(ruid)
					.with_type(type)
					.with_description(description)
				));
	}

	@Test
	public void can_add_an_event_to_a_request() {
		RequestActivity activity = new RequestActivity(ruid, activityEventSink);
		activity.beginRequest(randomRequestType(), randomString());
		AppEventType eventType = randomEventType();
		String eventDescription = randomString();
		activity.beginEvent(eventType, eventDescription);
		MatcherAssert.assertThat(activityEventsOutput, events(
				a_begin_request(),
				a_begin_event()
					.with_ruid(ruid)
					.with_type(eventType)
					.with_description(eventDescription)
					.where_parent_event_id_is(Matchers.equalTo(activityEventsOutput.get(0).attributes.get("eventId")))
				));
	}

	@Test
	public void adding_a_second_event_links_to_the_first() {
		RequestActivity activity = new RequestActivity(ruid, activityEventSink);
		activity.beginRequest(randomRequestType(), randomString());
		activity.beginEvent(randomEventType(), randomString());
		activity.beginEvent(randomEventType(), randomString());
		MatcherAssert.assertThat(activityEventsOutput, events(
				a_begin_request(),
				a_begin_event(),
				a_begin_event()
					.where_parent_event_id_is(Matchers.equalTo(activityEventsOutput.get(1).attributes.get("eventId")))
				));
	}

	@Test
	public void can_finish_an_event() {
		RequestActivity activity = new RequestActivity(ruid, activityEventSink);
		activity.beginRequest(randomRequestType(), randomString());
		AppEventType eventType = randomEventType();
		String eventDescription = randomString();
		activity.beginEvent(eventType, eventDescription);
		activity.finishEvent(eventType);
		MatcherAssert.assertThat(activityEventsOutput, events(
				a_begin_request(),
				a_begin_event(),
				a_finish_event()
					.with_ruid(ruid)
					.where_event_id_is(Matchers.equalTo(activityEventsOutput.get(1).attributes.get("eventId")))
					.where_parent_event_id_is(Matchers.equalTo(activityEventsOutput.get(0).attributes.get("eventId")))
					.where_duration_is(Matchers.greaterThanOrEqualTo(0L))
				));
	}

	@Test
	public void can_finish_a_request() {
		RequestActivity activity = new RequestActivity(ruid, activityEventSink);
		AppRequestType requestType = randomRequestType();
		String requestDescription = randomString();
		activity.beginRequest(requestType, requestDescription);
		activity.finishRequest(requestType);
		MatcherAssert.assertThat(activityEventsOutput, events(
				a_begin_request(),
				a_finish_request()
					.with_ruid(ruid)
					.where_event_id_is(Matchers.equalTo(activityEventsOutput.get(0).attributes.get("eventId")))
					.where_duration_is(Matchers.greaterThanOrEqualTo(0L))
				));
	}

	@Test(expected = IllegalStateException.class)
	public void cannot_finish_a_request_specifying_a_mismatching_type() {
		RequestActivity activity = new RequestActivity(ruid, activityEventSink);
		AppRequestType requestType = randomRequestType();
		activity.beginRequest(requestType, randomString());
		activity.finishRequest(notThis(requestType));
	}

	@Test(expected = IllegalStateException.class)
	public void cannot_finish_a_request_with_an_outstanding_event() {
		RequestActivity activity = new RequestActivity(ruid, activityEventSink);
		AppRequestType requestType = randomRequestType();
		activity.beginRequest(requestType, randomString());
		AppEventType eventType = randomEventType();
		activity.beginEvent(eventType, randomString());
		activity.finishRequest(requestType);
	}

	@Test(expected = IllegalStateException.class)
	public void cannot_finish_a_request_with_no_request_started() {
		RequestActivity activity = new RequestActivity(ruid, activityEventSink);
		activity.finishRequest(randomRequestType());
	}

	@Test(expected = IllegalStateException.class)
	public void cannot_finish_an_event_specifying_a_mismatching_type() {
		RequestActivity activity = new RequestActivity(ruid, activityEventSink);
		activity.beginRequest(randomRequestType(), randomString());
		AppEventType eventType = randomEventType();
		activity.beginEvent(eventType, randomString());
		activity.finishEvent(notThis(eventType));
	}

	@Test(expected = IllegalStateException.class)
	public void cannot_finish_an_event_with_no_event_started() {
		RequestActivity activity = new RequestActivity(ruid, activityEventSink);
		activity.beginRequest(randomRequestType(), randomString());
		activity.finishEvent(randomEventType());
	}

	@Test(expected = IllegalStateException.class)
	public void cannot_begin_an_event_before_beginning_a_request() {
		RequestActivity activity = new RequestActivity(ruid, activityEventSink);
		activity.beginEvent(randomEventType(), randomString());
	}

	@Test(expected = IllegalStateException.class)
	public void cannot_end_an_event_before_beginning_a_request() {
		RequestActivity activity = new RequestActivity(ruid, activityEventSink);
		activity.finishEvent(randomEventType());
	}

	@Test(expected = IllegalStateException.class)
	public void cannot_end_a_request_before_beginning_a_request() {
		RequestActivity activity = new RequestActivity(ruid, activityEventSink);
		activity.finishRequest(randomRequestType());
	}

	@Test(expected = IllegalStateException.class)
	public void cannot_begin_another_request_after_ending_a_request() {
		RequestActivity activity = new RequestActivity(ruid, activityEventSink);
		AppRequestType requestType = randomRequestType();
		activity.beginRequest(requestType, randomString());
		activity.finishRequest(requestType);
		activity.beginRequest(randomRequestType(), randomString());
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

	private static AppRequestType randomRequestType() {
		return randomEnumInstance(AppRequestType.class);
	}

	private static AppEventType randomEventType() {
		return randomEnumInstance(AppEventType.class);
	}

	private static AppRequestType notThis(AppRequestType value) {
		return randomOtherInstanceOfEnum(AppRequestType.class, value);
	}

	private static AppEventType notThis(AppEventType value) {
		return randomOtherInstanceOfEnum(AppEventType.class, value);
	}

	private static <T extends Enum<T>> T randomEnumInstance(Class<T> enumClass) {
		return pickOne(EnumSet.allOf(enumClass));
	}

	private static <T extends Enum<T>> T randomOtherInstanceOfEnum(Class<T> enumClass, T excludedValue) {
		return pickOne(EnumSet.complementOf(EnumSet.of(excludedValue)));
	}

	private static <T> T pickOne(Set<T> values) {
		int index = new Random().nextInt(values.size());
		Iterator<T> iter = values.iterator();
		while (index-- > 0) {
			iter.next();
		}
		return iter.next();
	}

	@SafeVarargs
	private static Matcher<Iterable<? extends EventOutput>> events(Matcher<EventOutput>... eventMatchers) {
		if (eventMatchers.length == 0) { return new TypeSafeDiagnosingMatcher<Iterable<? extends EventOutput>>() {
			@Override
			protected boolean matchesSafely(Iterable<? extends EventOutput> item, Description mismatchDescription) {
				if (item.iterator().hasNext()) {
					mismatchDescription.appendText("outputs available");
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("No outputs");
			}
		}; }

		return Matchers.contains(eventMatchers);
	}

	private static BeginRequestMatcher a_begin_request() {
		return new BeginRequestMatcher();
	}

	private static BeginEventMatcher a_begin_event() {
		return new BeginEventMatcher();
	}

	private static FinishEventMatcher a_finish_event() {
		return new FinishEventMatcher();
	}

	private static FinishRequestMatcher a_finish_request() {
		return new FinishRequestMatcher();
	}

	private static abstract class EventMatcher extends TypeSafeDiagnosingMatcher<EventOutput> {
		protected final String eventName;
		protected final EventAttributesMatcher attributesMatcher;

		protected EventMatcher(String eventName, String... expectedKeys) {
			this.eventName = eventName;
			this.attributesMatcher = new EventAttributesMatcher(expectedKeys);
		}

		@Override
		protected boolean matchesSafely(EventOutput item, Description mismatchDescription) {
			if (!item.name.equals(eventName)) {
				mismatchDescription.appendText("output type is ").appendValue(item.name);
				return false;
			}
			if (!attributesMatcher.matches(item.attributes)) {
				mismatchDescription.appendText("In ").appendValue(item.name).appendText(" ");
				attributesMatcher.describeMismatch(item.attributes, mismatchDescription);
				return false;
			}
			return true;
		}

		@Override
		public void describeTo(Description description) {
			description.appendValue(eventName).appendText(" event with ").appendDescriptionOf(attributesMatcher);
		}
	}

	public static final class BeginRequestMatcher extends EventMatcher {
		public BeginRequestMatcher() {
			super("beginRequest", "ruid", "eventId", "type", "description");
		}

		public BeginRequestMatcher with_ruid(String value) {
			attributesMatcher.match("ruid", Matchers.equalTo(value));
			return this;
		}

		public BeginRequestMatcher where_ruid_is(Matcher<? super String> matcher) {
			attributesMatcher.match("ruid", matcher);
			return this;
		}

		public BeginRequestMatcher with_event_id(long value) {
			attributesMatcher.match("eventId", Matchers.equalTo(value));
			return this;
		}

		public BeginRequestMatcher where_event_id_is(Matcher<? super Long> matcher) {
			attributesMatcher.match("eventId", matcher);
			return this;
		}

		public BeginRequestMatcher with_type(AppRequestType value) {
			attributesMatcher.match("type", Matchers.equalTo(value.name()));
			return this;
		}

		public BeginRequestMatcher where_type_is(Matcher<? super String> matcher) {
			attributesMatcher.match("type", matcher);
			return this;
		}

		public BeginRequestMatcher with_description(String value) {
			attributesMatcher.match("description", Matchers.equalTo(value));
			return this;
		}

		public BeginRequestMatcher where_description_is(Matcher<? super String> matcher) {
			attributesMatcher.match("description", matcher);
			return this;
		}
	}

	public static final class BeginEventMatcher extends EventMatcher {
		public BeginEventMatcher() {
			super("beginEvent", "ruid", "parentEventId", "eventId", "type", "description");
		}

		public BeginEventMatcher with_ruid(String value) {
			attributesMatcher.match("ruid", Matchers.equalTo(value));
			return this;
		}

		public BeginEventMatcher where_ruid_is(Matcher<? super String> matcher) {
			attributesMatcher.match("ruid", matcher);
			return this;
		}

		public BeginEventMatcher with_event_id(long value) {
			attributesMatcher.match("eventId", Matchers.equalTo(value));
			return this;
		}

		public BeginEventMatcher where_event_id_is(Matcher<? super Long> matcher) {
			attributesMatcher.match("eventId", matcher);
			return this;
		}

		public BeginEventMatcher with_parent_event_id(long value) {
			attributesMatcher.match("parentEventId", Matchers.equalTo(value));
			return this;
		}

		public BeginEventMatcher where_parent_event_id_is(Matcher<? super Long> matcher) {
			attributesMatcher.match("parentEventId", matcher);
			return this;
		}

		public BeginEventMatcher with_type(AppEventType value) {
			attributesMatcher.match("type", Matchers.equalTo(value.name()));
			return this;
		}

		public BeginEventMatcher where_type_is(Matcher<? super String> matcher) {
			attributesMatcher.match("type", matcher);
			return this;
		}

		public BeginEventMatcher with_description(String value) {
			attributesMatcher.match("description", Matchers.equalTo(value));
			return this;
		}

		public BeginEventMatcher where_description_is(Matcher<? super String> matcher) {
			attributesMatcher.match("description", matcher);
			return this;
		}
	}

	public static final class FinishEventMatcher extends EventMatcher {
		public FinishEventMatcher() {
			super("finishEvent", "ruid", "parentEventId", "eventId", "type", "durationNanos");
		}

		public FinishEventMatcher with_ruid(String value) {
			attributesMatcher.match("ruid", Matchers.equalTo(value));
			return this;
		}

		public FinishEventMatcher where_ruid_is(Matcher<? super String> matcher) {
			attributesMatcher.match("ruid", matcher);
			return this;
		}

		public FinishEventMatcher with_event_id(long value) {
			attributesMatcher.match("eventId", Matchers.equalTo(value));
			return this;
		}

		public FinishEventMatcher where_event_id_is(Matcher<? super Long> matcher) {
			attributesMatcher.match("eventId", matcher);
			return this;
		}

		public FinishEventMatcher with_parent_event_id(long value) {
			attributesMatcher.match("parentEventId", Matchers.equalTo(value));
			return this;
		}

		public FinishEventMatcher where_parent_event_id_is(Matcher<? super Long> matcher) {
			attributesMatcher.match("parentEventId", matcher);
			return this;
		}

		public FinishEventMatcher with_type(AppEventType value) {
			attributesMatcher.match("type", Matchers.equalTo(value.name()));
			return this;
		}

		public FinishEventMatcher where_type_is(Matcher<? super String> matcher) {
			attributesMatcher.match("type", matcher);
			return this;
		}

		public FinishEventMatcher with_duration(long value) {
			attributesMatcher.match("durationNanos", Matchers.equalTo(value));
			return this;
		}

		public FinishEventMatcher where_duration_is(Matcher<? super Long> matcher) {
			attributesMatcher.match("durationNanos", matcher);
			return this;
		}
	}

	public static final class FinishRequestMatcher extends EventMatcher {
		public FinishRequestMatcher() {
			super("finishRequest", "ruid", "eventId", "type", "durationNanos");
		}

		public FinishRequestMatcher with_ruid(String value) {
			attributesMatcher.match("ruid", Matchers.equalTo(value));
			return this;
		}

		public FinishRequestMatcher where_ruid_is(Matcher<? super String> matcher) {
			attributesMatcher.match("ruid", matcher);
			return this;
		}

		public FinishRequestMatcher with_event_id(long value) {
			attributesMatcher.match("eventId", Matchers.equalTo(value));
			return this;
		}

		public FinishRequestMatcher where_event_id_is(Matcher<? super Long> matcher) {
			attributesMatcher.match("eventId", matcher);
			return this;
		}

		public FinishRequestMatcher with_type(AppRequestType value) {
			attributesMatcher.match("type", Matchers.equalTo(value.name()));
			return this;
		}

		public FinishRequestMatcher where_type_is(Matcher<? super String> matcher) {
			attributesMatcher.match("type", matcher);
			return this;
		}

		public FinishRequestMatcher with_duration(long value) {
			attributesMatcher.match("durationNanos", Matchers.equalTo(value));
			return this;
		}

		public FinishRequestMatcher where_duration_is(Matcher<? super Long> matcher) {
			attributesMatcher.match("durationNanos", matcher);
			return this;
		}
	}

	public static final class EventAttributesMatcher extends TypeSafeDiagnosingMatcher<Map<String, Object>> {
		private final Map<String, Matcher<?>> expectedAttributes = new HashMap<>();

		public EventAttributesMatcher(String... expectedKeys) {
			for (String key : expectedKeys) {
				expectedAttributes.put(key, Matchers.notNullValue());
			}
		}

		public void match(String key, Matcher<?> value) {
			expectedAttributes.put(key, value);
		}

		@Override
		protected boolean matchesSafely(Map<String, Object> item, Description mismatchDescription) {
			for (Map.Entry<String, Matcher<?>> e : expectedAttributes.entrySet()) {
				if (!item.containsKey(e.getKey())) {
					mismatchDescription.appendText("Attribute not present: ").appendValue(e.getKey());
					return false;
				}
				Object value = item.get(e.getKey());
				if (!e.getValue().matches(value)) {
					mismatchDescription.appendText("attribute value for ").appendValue(e.getKey()).appendText(" ");
					e.getValue().describeMismatch(value, mismatchDescription);
					return false;
				}
			}
			Set<String> extraKeys = new HashSet<>(item.keySet());
			extraKeys.removeAll(expectedAttributes.keySet());
			if (!extraKeys.isEmpty()) {
				mismatchDescription.appendText("Unexpected attributes present: ").appendValue(extraKeys);
				return false;
			}
			return true;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("attributes: ");
			boolean first = true;
			for (Map.Entry<String, Matcher<?>> e : expectedAttributes.entrySet()) {
				if (first) first = false;
				else description.appendText(" and ");
				description.appendText(" ").appendValue(e.getKey()).appendText(" is ")
						.appendDescriptionOf(e.getValue());
			}
		}
	}

	public static final class CaptureEvents implements ActivityEventSink {
		private final List<EventOutput> output;
		
		public CaptureEvents(List<EventOutput> output) {
			this.output = output;
		}

		@Override
		public void beginRequest(String ruid, long eventId, String type, String description) {
			add("beginRequest", "ruid", ruid, "eventId", eventId, "type", type, "description", description);
		}

		@Override
		public void beginEvent(String ruid, long eventId, long parentEventId, String type, String description) {
			add("beginEvent", "ruid", ruid, "eventId", eventId, "parentEventId", parentEventId, "type", type,
					"description", description);
		}

		@Override
		public void finishEvent(String ruid, long eventId, long parentEventId, String type, long durationNanos) {
			add("finishEvent", "ruid", ruid, "eventId", eventId, "parentEventId", parentEventId, "type", type,
					"durationNanos", durationNanos);
		}

		@Override
		public void finishRequest(String ruid, long eventId, String type, long durationNanos) {
			add("finishRequest", "ruid", ruid, "eventId", eventId, "type", type, "durationNanos", durationNanos);
		}

		private void add(String name, Object... attributes) {
			output.add(new EventOutput(name, attributes));
		}
	}

	public static final class EventOutput {
		public final String name;
		public final Map<String, Object> attributes;

		public EventOutput(String name, Object... attributes) {
			this.name = name;
			Builder<String, Object> builder = ImmutableMap.<String, Object> builder();
			for (int i = 0; i < attributes.length; i += 2) {
				builder.put((String) attributes[i], attributes[i + 1]);
			}
			this.attributes = builder.build();
		}
	}
}

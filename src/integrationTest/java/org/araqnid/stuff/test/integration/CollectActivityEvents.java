package org.araqnid.stuff.test.integration;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.araqnid.stuff.activity.ActivityEventSink;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import com.google.common.base.Optional;

public class CollectActivityEvents implements ActivityEventSink {
	public final Queue<ActivityEventRecord> events = new LinkedBlockingQueue<>();

	@Override
	public void beginRequest(String ruid, long eventId, String type, String description) {
		events.add(ActivityEventRecord.beginRequest(ruid, eventId, type, description));
	}

	@Override
	public void beginEvent(String ruid, long eventId, long parentEventId, String type, String description) {
		events.add(ActivityEventRecord.beginEvent(ruid, eventId, parentEventId, type, description));
	}

	@Override
	public void finishRequest(String ruid, long eventId, String type, long durationNanos) {
		events.add(ActivityEventRecord.finishRequest(ruid, eventId, type, durationNanos));
	}

	@Override
	public void finishEvent(String ruid, long eventId, long parentEventId, String type, long durationNanos) {
		events.add(ActivityEventRecord.finishEvent(ruid, eventId, parentEventId, type, durationNanos));
	}

	public static class ActivityEventRecord {
		public final String method;
		public final String ruid;
		public final long eventId;
		public final Optional<Long> parentEventId;
		public final String type;
		public final Optional<String> description;
		public final Optional<Long> durationNanos;

		public ActivityEventRecord(String method, String ruid, long eventId, Optional<Long> parentEventId, String type,
				Optional<String> description, Optional<Long> durationNanos) {
			this.method = method;
			this.ruid = ruid;
			this.eventId = eventId;
			this.parentEventId = parentEventId;
			this.type = type;
			this.description = description;
			this.durationNanos = durationNanos;
		}

		public static ActivityEventRecord beginRequest(String ruid, long eventId, String type, String description) {
			return new ActivityEventRecord("beginRequest", ruid, eventId, Optional.<Long> absent(), type,
					Optional.of(description), Optional.<Long> absent());
		}

		public static ActivityEventRecord beginEvent(String ruid, long eventId, long parentEventId, String type,
				String description) {
			return new ActivityEventRecord("beginEvent", ruid, eventId, Optional.of(parentEventId), type,
					Optional.of(description), Optional.<Long> absent());
		}

		public static ActivityEventRecord finishRequest(String ruid, long eventId, String type, long durationNanos) {
			return new ActivityEventRecord("finishRequest", ruid, eventId, Optional.<Long> absent(), type,
					Optional.<String> absent(), Optional.of(durationNanos));
		}

		public static ActivityEventRecord finishEvent(String ruid, long eventId, long parentEventId, String type,
				long durationNanos) {
			return new ActivityEventRecord("finishEvent", ruid, eventId, Optional.of(parentEventId), type,
					Optional.<String> absent(), Optional.of(durationNanos));
		}
	}

	public static ActivityRecordMatcher finishRequestRecord(final Matcher<String> requestType) {
		return new ActivityRecordMatcher("finishRequest", requestType);
	}

	public static BeginRequestRecordMatcher beginRequestRecord(final Matcher<String> requestType) {
		return new BeginRequestRecordMatcher(requestType);
	}

	public static class ActivityRecordMatcher extends TypeSafeDiagnosingMatcher<ActivityEventRecord> {
		private final String method;
		private final Matcher<String> requestType;
		private Matcher<String> ruid;

		public ActivityRecordMatcher(String method, Matcher<String> requestType) {
			this.method = method;
			this.requestType = requestType;
		}

		@Override
		protected boolean matchesSafely(ActivityEventRecord item, Description mismatchDescription) {
			if (!item.method.equals(method)) {
				mismatchDescription.appendText("method is ").appendValue(item.method);
				return false;
			}
			if (!requestType.matches(item.type)) {
				mismatchDescription.appendText("type is ").appendValue(item.type);
				return false;
			}
			if (ruid != null && !ruid.matches(item.ruid)) {
				mismatchDescription.appendText("ruid is ").appendValue(item.ruid);
				return false;
			}
			return true;
		}

		@Override
		public void describeTo(Description description) {
			description.appendValue(method).appendText(" with type ").appendDescriptionOf(requestType);
			if (ruid != null) {
				description.appendText(", ruid ").appendDescriptionOf(ruid);
			}
		}

		public ActivityRecordMatcher withRuid(String ruid) {
			this.ruid = Matchers.equalTo(ruid);
			return this;
		}

		public ActivityRecordMatcher withRuid(Matcher<String> ruid) {
			this.ruid = ruid;
			return this;
		}
	}

	public static class BeginRequestRecordMatcher extends ActivityRecordMatcher {
		private Matcher<Optional<String>> descriptionMatcher;

		public BeginRequestRecordMatcher(Matcher<String> requestType) {
			super("beginRequest", requestType);
		}

		@Override
		protected boolean matchesSafely(ActivityEventRecord item, Description mismatchDescription) {
			if (!super.matchesSafely(item, mismatchDescription)) return false;
			if (descriptionMatcher != null && !descriptionMatcher.matches(item.description)) {
				mismatchDescription.appendText("description is ").appendValue(item.description);
				return false;
			}
			return true;
		}

		@Override
		public void describeTo(Description description) {
			super.describeTo(description);
			if (description != null) {
				description.appendText(", description ").appendDescriptionOf(descriptionMatcher);
			}
		}

		public BeginRequestRecordMatcher withDescription(String description) {
			this.descriptionMatcher = isSome(Matchers.equalTo(description));
			return this;
		}

		public BeginRequestRecordMatcher withDescription(Matcher<String> descriptionMatcher) {
			this.descriptionMatcher = isSome(descriptionMatcher);
			return this;
		}

		public BeginRequestRecordMatcher withoutDescription() {
			this.descriptionMatcher = isNone(String.class);
			return this;
		}
	}

	public static <T> Matcher<Optional<T>> isNone(Class<T> clazz) {
		return new TypeSafeDiagnosingMatcher<Optional<T>>() {
			@Override
			protected boolean matchesSafely(Optional<T> item, Description mismatchDescription) {
				if (item.isPresent()) {
					mismatchDescription.appendText("value is present: ").appendValue(item.get());
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("absent");
			}
		};
	}

	public static <T> Matcher<Optional<T>> isSome(final Matcher<? super T> matcher) {
		return new TypeSafeDiagnosingMatcher<Optional<T>>() {
			@Override
			protected boolean matchesSafely(Optional<T> item, Description mismatchDescription) {
				if (!item.isPresent()) {
					mismatchDescription.appendText("value is absent");
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendDescriptionOf(matcher);
			}
		};
	}
}

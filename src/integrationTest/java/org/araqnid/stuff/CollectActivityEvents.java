package org.araqnid.stuff;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.araqnid.stuff.activity.ActivityEventSink;

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
}
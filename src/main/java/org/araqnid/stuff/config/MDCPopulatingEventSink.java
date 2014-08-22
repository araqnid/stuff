package org.araqnid.stuff.config;

import org.araqnid.stuff.activity.ActivityEventSink;
import org.slf4j.MDC;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class MDCPopulatingEventSink implements ActivityEventSink {
	private final ActivityEventSink underlying;

	@Inject
	public MDCPopulatingEventSink(@Named("logger") ActivityEventSink underlying) {
		this.underlying = underlying;
	}

	public void beginRequest(String ruid, long eventId, String type, String description) {
		underlying.beginRequest(ruid, eventId, type, description);
		MDC.put("ruid", ruid);
	}

	public void beginEvent(String ruid, long eventId, long parentEventId, String type, String description, long startTimeNanos) {
		underlying.beginEvent(ruid, eventId, parentEventId, type, description, startTimeNanos);
	}

	public void finishEvent(String ruid, long eventId, long parentEventId, String type, long durationNanos) {
		underlying.finishEvent(ruid, eventId, parentEventId, type, durationNanos);
	}

	public void finishRequest(String ruid, long eventId, String type, long durationNanos) {
		underlying.finishRequest(ruid, eventId, type, durationNanos);
		MDC.remove("ruid");
	}
}
package org.araqnid.stuff;

import org.araqnid.stuff.RequestActivity.ActivityEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class LogActivityEvents<R extends Enum<R>, E extends Enum<E>> implements ActivityEventSink<R, E> {
	private static final Logger LOG = LoggerFactory.getLogger(LogActivityEvents.class);

	@Override
	public void beginRequest(String ruid, long eventId, R type, String description) {
		MDC.put("ruid", ruid);
		LOG.info("begin " + ruid + " " + type + " " + eventId + " " + description);
	}

	@Override
	public void beginEvent(String ruid, long eventId, long parentEventId, E type, String description) {
		LOG.info("begin " + ruid + " " + type + " " + parentEventId + "->" + eventId + " " + description);
	}

	@Override
	public void finishEvent(String ruid, long eventId, long parentEventId, E type, long durationNanos) {
		LOG.info("end   " + ruid + " " + type + " " + parentEventId + "<-" + eventId + " "
				+ String.format("%.1fms", (double) durationNanos / 1E6));
	}

	@Override
	public void finishRequest(String ruid, long eventId, R type, long durationNanos) {
		LOG.info("end   " + ruid + " " + type + " " + eventId + " "
				+ String.format("%.1fms", (double) durationNanos / 1E6));
		MDC.remove("ruid");
	}
}

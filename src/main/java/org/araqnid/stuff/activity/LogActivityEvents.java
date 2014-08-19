package org.araqnid.stuff.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogActivityEvents implements ActivityEventSink {
	private static final Logger LOG = LoggerFactory.getLogger(LogActivityEvents.class);

	@Override
	public void beginRequest(String ruid, long eventId, String type, String description) {
		LOG.info("begin " + ruid + " " + type + " " + eventId + " " + description);
	}

	@Override
	public void beginEvent(String ruid, long eventId, long parentEventId, String type, String description) {
		LOG.info("begin " + ruid + " " + type + " " + parentEventId + "->" + eventId + " " + description);
	}

	@Override
	public void finishEvent(String ruid, long eventId, long parentEventId, String type, long durationNanos) {
		LOG.info("end   " + ruid + " " + type + " " + parentEventId + "<-" + eventId + " "
				+ String.format("%.1fms", (double) durationNanos / 1E6));
	}

	@Override
	public void finishRequest(String ruid, long eventId, String type, long durationNanos) {
		LOG.info("end   " + ruid + " " + type + " " + eventId + " "
				+ String.format("%.1fms", (double) durationNanos / 1E6));
	}
}

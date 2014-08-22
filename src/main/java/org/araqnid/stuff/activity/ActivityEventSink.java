package org.araqnid.stuff.activity;

public interface ActivityEventSink {
	void beginRequest(String ruid, long eventId, String type, String description);

	void beginEvent(String ruid, long eventId, long parentEventId, String type, String description, long startTimeNanos);

	void finishEvent(String ruid, long eventId, long parentEventId, String type, long durationNanos);

	void finishRequest(String ruid, long eventId, String type, long durationNanos);
}

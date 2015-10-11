package org.araqnid.stuff.activity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public interface ActivityEventSink {
	void activityNodeStart(UUID activityId,
			long nodeId,
			long nodeParentId,
			String type,
			Instant started,
			Object attributes);

	void activityNodeEnd(UUID activityId, long nodeId, boolean success, Duration duration, Object attributes);
}

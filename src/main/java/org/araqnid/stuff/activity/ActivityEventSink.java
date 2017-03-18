package org.araqnid.stuff.activity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.annotation.Nullable;

public interface ActivityEventSink {
	void activityNodeStart(UUID activityId,
						   long nodeId,
						   long nodeParentId,
						   String type,
						   Instant started,
						   @Nullable Object attributes);

	void activityNodeEnd(UUID activityId, long nodeId, boolean success, Duration duration, @Nullable Object attributes);
}

package org.araqnid.stuff.activity;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import com.google.common.base.Stopwatch;

public class ActivityNode implements Completable {
	public static final long NO_PARENT = 0L;
	private static final AtomicLong EVENT_ID_SOURCE = new AtomicLong();
	public final long id = EVENT_ID_SOURCE.incrementAndGet();
	@Nullable
	public final ActivityNode parent;
	public final String type;
	public final Activity activity;
	public final Instant started = Instant.now();
	public final Stopwatch stopwatch = Stopwatch.createStarted();
	public final Object nodeAttributes;

	public ActivityNode(Activity activity, ActivityNode parent, String type, Object nodeAttributes) {
		this.activity = activity;
		this.parent = parent;
		this.type = type;
		this.nodeAttributes = nodeAttributes;
	}

	public ActivityNode begin(String type) {
		return begin(type, null);
	}

	public ActivityNode begin(String type, Object attributes) {
		ActivityNode node = new ActivityNode(activity, this, type, attributes);
		node.begin();
		return node;
	}

	public Completable.Rec<ActivityNode> recordActivity(String type) {
		return recordActivity(type, null);
	}

	public Completable.Rec<ActivityNode> recordActivity(String type, Object attributes) {
		return new Completable.Rec<>(begin(type, attributes));
	}

	void begin() {
		activity.sink.activityNodeStart(activity.id, id, parent != null ? parent.id : NO_PARENT, type, started,
				nodeAttributes);
		ThreadActivity.transition(parent, this);
	}

	@Override
	public void complete(boolean success, Object completionAttributes) {
		activity.sink.activityNodeEnd(activity.id, id, success,
				Duration.ofNanos(stopwatch.elapsed(TimeUnit.NANOSECONDS)), completionAttributes);
		ThreadActivity.transition(this, parent);
	}
}

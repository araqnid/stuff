package org.araqnid.stuff;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.araqnid.stuff.config.ActivityScoped;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

@ActivityScoped
public class RequestActivity {
	private static final AtomicLong idGenerator = new AtomicLong();
	private final ActivityEventSink activityEventSink;
	private final String ruid;
	private EventNode event;
	private boolean used;

	@Inject
	public RequestActivity(String ruid, ActivityEventSink activityEventSink) {
		this.activityEventSink = activityEventSink;
		this.ruid = ruid;
	}

	public String getRuid() {
		return ruid;
	}

	public void beginRequest(String type, String description) {
		if (event != null) throw new IllegalStateException("Event stack is not empty");
		if (used) throw new IllegalStateException("RequestActivity should be not reused");
		type = type.intern();
		event = new EventNode(idGenerator.incrementAndGet(), type, description, null);
		activityEventSink.beginRequest(ruid, event.id, type, description);
		used = true;
	}

	public void beginEvent(String type, String description) {
		if (event == null) throw new IllegalStateException("Event stack is empty");
		type = type.intern();
		EventNode parent = event;
		event = new EventNode(idGenerator.incrementAndGet(), type, description, parent);
		activityEventSink.beginEvent(ruid, event.id, parent.id, type, description);
	}

	public void finishEvent(String type) {
		if (event == null) throw new IllegalStateException("Event stack is empty");
		type = type.intern();
		if (event.type != type) throw new IllegalStateException("Top event on stack '" + event.type
				+ "' does not match this type: " + type);
		event.stopwatch.stop();
		EventNode parent = event.parent;
		activityEventSink.finishEvent(ruid, event.id, parent != null ? parent.id : -1, type, event.stopwatch.elapsed(TimeUnit.NANOSECONDS));
		event = event.parent;
	}

	public void finishRequest(String type) {
		if (event == null) throw new IllegalStateException("Event stack is empty");
		type = type.intern();
		if (event.type != type) throw new IllegalStateException("Top event on stack '" + event.type
				+ "' does not match this type: " + type);
		event.stopwatch.stop();
		activityEventSink.finishRequest(ruid, event.id, type, event.stopwatch.elapsed(TimeUnit.NANOSECONDS));
		event = event.parent;
	}

	public static class EventNode {
		public final long id;
		public final String type;
		public final String description;
		public final Stopwatch stopwatch = Stopwatch.createStarted();
		public final EventNode parent;

		public EventNode(long id, String type, String description, EventNode parent) {
			this.id = id;
			this.type = type;
			this.description = description;
			this.parent = parent;
		}
	}

	public interface ActivityEventSink {
		void beginRequest(String ruid, long eventId, String type, String description);

		void beginEvent(String ruid, long eventId, long parentEventId, String type, String description);

		void finishEvent(String ruid, long eventId, long parentEventId, String type, long durationNanos);

		void finishRequest(String ruid, long eventId, String type, long durationNanos);
	}
}

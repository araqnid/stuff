package org.araqnid.stuff.activity;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

@ActivityScoped
public class RequestActivity {
	private static final AtomicLong idGenerator = new AtomicLong();
	private final ActivityEventSink activityEventSink;
	private final String ruid;
	private EventNode event;
	private boolean used;
	private EventNode rootEvent;

	@Inject
	public RequestActivity(String ruid, ActivityEventSink activityEventSink) {
		this.activityEventSink = activityEventSink;
		this.ruid = ruid;
	}

	public String getRuid() {
		return ruid;
	}

	public void beginRequest(AppRequestType type, String description) {
		if (event != null) throw new IllegalStateException("Event stack is not empty");
		if (used) throw new IllegalStateException("RequestActivity should be not reused");
		event = new EventNode(idGenerator.incrementAndGet(), type.name(), description, null);
		activityEventSink.beginRequest(ruid, event.id, type.name(), description);
		used = true;
		rootEvent = event;
	}

	public void beginEvent(AppEventType type, String description) {
		if (event == null) throw new IllegalStateException("Event stack is empty");
		EventNode parent = event;
		event = new EventNode(idGenerator.incrementAndGet(), type.name(), description, parent);
		activityEventSink.beginEvent(ruid, event.id, parent.id, type.name(), description);
	}

	public void finishEvent(AppEventType type) {
		if (event == null) throw new IllegalStateException("Event stack is empty");
		if (event.type != type.name()) throw new IllegalStateException("Top event on stack '" + event.type
				+ "' does not match this type: " + type);
		event.stopwatch.stop();
		EventNode parent = event.parent;
		activityEventSink.finishEvent(ruid, event.id, parent != null ? parent.id : -1, type.name(), event.stopwatch.elapsed(TimeUnit.NANOSECONDS));
		event = event.parent;
	}

	public void finishRequest(AppRequestType type) {
		if (event == null) throw new IllegalStateException("Event stack is empty");
		if (event.type != type.name()) throw new IllegalStateException("Top event on stack '" + event.type
				+ "' does not match this type: " + type);
		event.stopwatch.stop();
		activityEventSink.finishRequest(ruid, event.id, type.name(), event.stopwatch.elapsed(TimeUnit.NANOSECONDS));
		event = null;
		rootEvent = null;
	}

	public EventNode getRootEvent() {
		return rootEvent;
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
}

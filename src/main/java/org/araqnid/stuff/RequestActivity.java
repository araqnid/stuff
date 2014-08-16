package org.araqnid.stuff;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.araqnid.stuff.config.ActivityScoped;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

@ActivityScoped
public class RequestActivity<R extends Enum<R>, E extends Enum<E>> {
	private static final AtomicLong idGenerator = new AtomicLong();
	private final ActivityEventSink<R, E> activityEventSink;
	private final String ruid;
	private EventNode<R, E> event;
	private boolean used;

	@Inject
	public RequestActivity(String ruid, ActivityEventSink<R, E> activityEventSink) {
		this.activityEventSink = activityEventSink;
		this.ruid = ruid;
	}

	public String getRuid() {
		return ruid;
	}

	public void beginRequest(R type, String description) {
		if (event != null) throw new IllegalStateException("Event stack is not empty");
		if (used) throw new IllegalStateException("RequestActivity should be not reused");
		event = new EventNode<R, E>(idGenerator.incrementAndGet(), type, null, description, null);
		activityEventSink.beginRequest(ruid, event.id, type, description);
		used = true;
	}

	public void beginEvent(E type, String description) {
		if (event == null) throw new IllegalStateException("Event stack is empty");
		EventNode<R, E> parent = event;
		event = new EventNode<R, E>(idGenerator.incrementAndGet(), null, type, description, parent);
		activityEventSink.beginEvent(ruid, event.id, parent.id, type, description);
	}

	public void finishEvent(E type) {
		if (event == null) throw new IllegalStateException("Event stack is empty");
		if (event.type() != type) throw new IllegalStateException("Top event on stack '" + event.type()
				+ "' does not match this type: " + type);
		event.stopwatch.stop();
		EventNode<R, E> parent = event.parent;
		activityEventSink.finishEvent(ruid, event.id, parent != null ? parent.id : -1, type, event.stopwatch.elapsed(TimeUnit.NANOSECONDS));
		event = event.parent;
	}

	public void finishRequest(R type) {
		if (event == null) throw new IllegalStateException("Event stack is empty");
		if (event.type() != type) throw new IllegalStateException("Top event on stack '" + event.type()
				+ "' does not match this type: " + type);
		event.stopwatch.stop();
		activityEventSink.finishRequest(ruid, event.id, type, event.stopwatch.elapsed(TimeUnit.NANOSECONDS));
		event = event.parent;
	}

	public static class EventNode<R extends Enum<R>, E extends Enum<E>> {
		public final long id;
		public final R requestType;
		public final E eventType;
		public final String description;
		public final Stopwatch stopwatch = Stopwatch.createStarted();
		public final EventNode<R, E> parent;

		public EventNode(long id, R requestType, E eventType, String description, EventNode<R, E> parent) {
			this.id = id;
			this.requestType = requestType;
			this.eventType = eventType;
			this.description = description;
			this.parent = parent;
		}

		public Object type() {
			return requestType != null ? requestType : eventType;
		}
	}

	public interface ActivityEventSink<R extends Enum<R>, E extends Enum<E>> {
		void beginRequest(String ruid, long eventId, R type, String description);

		void beginEvent(String ruid, long eventId, long parentEventId, E type, String description);

		void finishEvent(String ruid, long eventId, long parentEventId, E type, long durationNanos);

		void finishRequest(String ruid, long eventId, R type, long durationNanos);
	}
}

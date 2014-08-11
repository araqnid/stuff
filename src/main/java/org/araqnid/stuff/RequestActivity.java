package org.araqnid.stuff;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;

@RequestScoped
public class RequestActivity {
	private static final AtomicLong idGenerator = new AtomicLong();
	private final ActivityEventSink activityEventSink;
	private String ruid;
	private List<EventNode> eventStack = Lists.newArrayList();
	private boolean used;

	@Inject
	public RequestActivity(ActivityEventSink activityEventSink) {
		this.activityEventSink = activityEventSink;
	}

	public String getRuid() {
		return ruid;
	}

	public void setRuid(String ruid) {
		this.ruid = ruid;
	}

	public void beginRequest(String type, String description) {
		if (!eventStack.isEmpty()) throw new IllegalStateException("Event stack is not empty");
		if (used) throw new IllegalStateException("RequestActivity should be not reused");
		type = type.intern();
		if (ruid == null) ruid = UUID.randomUUID().toString();
		EventNode node = new EventNode(idGenerator.incrementAndGet(), type, description);
		eventStack.add(node);
		activityEventSink.beginRequest(ruid, node.id, type, description);
		used = true;
	}

	public void beginEvent(String type, String description) {
		if (eventStack.isEmpty()) throw new IllegalStateException("Event stack is empty");
		type = type.intern();
		EventNode node = new EventNode(idGenerator.incrementAndGet(), type, description);
		EventNode parent = eventStack.get(eventStack.size() - 1);
		eventStack.add(node);
		activityEventSink.beginEvent(ruid, node.id, parent.id, type, description);
	}

	public EventNode finishEvent(String type) {
		if (eventStack.isEmpty()) throw new IllegalStateException("Event stack is empty");
		EventNode node = eventStack.get(eventStack.size() - 1);
		type = type.intern();
		if (node.type != type) throw new IllegalStateException("Top event on stack '" + node.type
				+ "' does not match this type: " + type);
		eventStack.remove(eventStack.size() - 1);
		node.stopwatch.stop();
		EventNode parent = eventStack.isEmpty() ? null : eventStack.get(eventStack.size() - 1);
		activityEventSink.finishEvent(ruid, node.id, parent != null ? parent.id : -1, type, node.stopwatch.elapsed(TimeUnit.NANOSECONDS));
		return node;
	}

	public EventNode finishRequest(String type) {
		if (eventStack.isEmpty()) throw new IllegalStateException("Event stack is empty");
		EventNode node = eventStack.get(eventStack.size() - 1);
		type = type.intern();
		if (node.type != type) throw new IllegalStateException("Top event on stack '" + node.type
				+ "' does not match this type: " + type);
		eventStack.remove(eventStack.size() - 1);
		node.stopwatch.stop();
		activityEventSink.finishRequest(ruid, node.id, type, node.stopwatch.elapsed(TimeUnit.NANOSECONDS));
		return node;
	}

	public static class EventNode {
		public final long id;
		public final String type;
		public final String description;
		public final Stopwatch stopwatch = Stopwatch.createStarted();

		public EventNode(long id, String type, String description) {
			this.id = id;
			this.type = type;
			this.description = description;
		}
	}

	public interface ActivityEventSink {
		void beginRequest(String ruid, long eventId, String type, String description);

		void beginEvent(String ruid, long eventId, long parentEventId, String type, String description);

		void finishEvent(String ruid, long eventId, long parentEventId, String type, long durationNanos);

		void finishRequest(String ruid, long eventId, String type, long durationNanos);
	}
}

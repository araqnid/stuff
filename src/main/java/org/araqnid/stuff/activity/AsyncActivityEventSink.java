package org.araqnid.stuff.activity;

import java.util.concurrent.BlockingQueue;


import com.google.inject.Inject;

public class AsyncActivityEventSink implements ActivityEventSink {
	private final BlockingQueue<Event> queue;

	@Inject
	public AsyncActivityEventSink(BlockingQueue<Event> queue) {
		this.queue = queue;
	}

	
	@Override
	public void beginRequest(final String ruid, final long eventId, final String type, final String description) {
		queue.add(new Event() {
			@Override
			public void deliver(ActivityEventSink sink) {
				sink.beginRequest(ruid, eventId, type, description);
			}
		});
	}


	@Override
	public void beginEvent(final String ruid, final long eventId, final long parentEventId, final String type, final String description) {
		queue.add(new Event() {
			@Override
			public void deliver(ActivityEventSink sink) {
				sink.beginEvent(ruid, eventId, parentEventId, type, description);
			}
		});
	}


	@Override
	public void finishEvent(final String ruid, final long eventId, final long parentEventId, final String type, final long durationNanos) {
		queue.add(new Event() {
			@Override
			public void deliver(ActivityEventSink sink) {
				sink.finishEvent(ruid, eventId, parentEventId, type, durationNanos);
			}
		});
	}


	@Override
	public void finishRequest(final String ruid, final long eventId, final String type, final long durationNanos) {
		queue.add(new Event() {
			@Override
			public void deliver(ActivityEventSink sink) {
				sink.finishRequest(ruid, eventId, type, durationNanos);
			}
		});
	}

	public interface Event {
		void deliver(ActivityEventSink sink);
	}
}

package org.araqnid.stuff;

import java.util.concurrent.BlockingQueue;

import org.araqnid.stuff.RequestActivity.ActivityEventSink;

import com.google.inject.Inject;

public class AsyncActivityEventSink<R extends Enum<R>, E extends Enum<E>> implements ActivityEventSink<R, E> {
	private final BlockingQueue<Event<R, E>> queue;

	@Inject
	public AsyncActivityEventSink(BlockingQueue<Event<R, E>> queue) {
		this.queue = queue;
	}

	
	@Override
	public void beginRequest(final String ruid, final long eventId, final R type, final String description) {
		queue.add(new Event<R, E>() {
			@Override
			public void deliver(ActivityEventSink<R, E> sink) {
				sink.beginRequest(ruid, eventId, type, description);
			}
		});
	}


	@Override
	public void beginEvent(final String ruid, final long eventId, final long parentEventId, final E type, final String description) {
		queue.add(new Event<R, E>() {
			@Override
			public void deliver(ActivityEventSink<R, E> sink) {
				sink.beginEvent(ruid, eventId, parentEventId, type, description);
			}
		});
	}


	@Override
	public void finishEvent(final String ruid, final long eventId, final long parentEventId, final E type, final long durationNanos) {
		queue.add(new Event<R, E>() {
			@Override
			public void deliver(ActivityEventSink<R, E> sink) {
				sink.finishEvent(ruid, eventId, parentEventId, type, durationNanos);
			}
		});
	}


	@Override
	public void finishRequest(final String ruid, final long eventId, final R type, final long durationNanos) {
		queue.add(new Event<R, E>() {
			@Override
			public void deliver(ActivityEventSink<R, E> sink) {
				sink.finishRequest(ruid, eventId, type, durationNanos);
			}
		});
	}

	public interface Event<R extends Enum<R>, E extends Enum<E>> {
		void deliver(ActivityEventSink<R, E> sink);
	}
}

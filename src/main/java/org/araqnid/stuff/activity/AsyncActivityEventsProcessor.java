package org.araqnid.stuff.activity;

import java.util.concurrent.BlockingQueue;

import org.araqnid.stuff.activity.AsyncActivityEventSink.Event;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;

public class AsyncActivityEventsProcessor extends AbstractExecutionThreadService {
	private static final Event STOP = new Event() {
		@Override
		public void deliver(ActivityEventSink sink) {
		}
	};
	private final ActivityEventSink sink;
	private final BlockingQueue<Event> queue;

	@Inject
	public AsyncActivityEventsProcessor(ActivityEventSink sink, BlockingQueue<Event> queue) {
		this.sink = sink;
		this.queue = queue;
	}

	@Override
	protected void run() throws Exception {
		while (isRunning()) {
			Event event = queue.take();
			if (event != STOP) event.deliver(sink);
		}
	}

	@Override
	protected void triggerShutdown() {
		queue.add(STOP);
	}
}

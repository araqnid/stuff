package org.araqnid.stuff;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.araqnid.stuff.AsyncActivityEventSink.Event;
import org.araqnid.stuff.RequestActivity.ActivityEventSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

public class AsyncActivityEventsProcessor<R extends Enum<R>, E extends Enum<E>> implements AppService {
	private static final Logger LOG = LoggerFactory.getLogger(AsyncActivityEventsProcessor.class);
	private final ActivityEventSink<R, E> sink;
	private final ExecutorService executor;
	private final BlockingQueue<Event<R, E>> queue;

	@Inject
	public AsyncActivityEventsProcessor(ActivityEventSink<R, E> sink, BlockingQueue<Event<R, E>> queue) {
		this.sink = sink;
		this.queue = queue;
		this.executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(
				"AsyncActivityEvents").build());
	}

	@Override
	public void start() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					do {
						Event<R, E> event = queue.take();
						event.deliver(sink);
					} while (true);
				} catch (InterruptedException e) {
					LOG.debug("Consuming event queue interruped, exiting");
				}
			}
		});
	}

	@Override
	public void stop() {
		executor.shutdownNow();
		try {
			executor.awaitTermination(2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOG.warn("Interrupted wait for consumer thread shutdown");
		}
	}
}

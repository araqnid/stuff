package org.araqnid.stuff;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class SpooledEventProcessorRunner {
	private static final Logger LOG = LoggerFactory.getLogger(SpooledEventProcessorRunner.class);
	private final BlockingQueue<Boolean> startedProcessing = new LinkedBlockingQueue<>();
	private final Service loader = new AbstractExecutionThreadService() {
		@Override
		protected void startUp() throws Exception {
			LOG.info("Loader starting up");
		}

		@Override
		protected void run() throws Exception {
			LOG.info("Loading stuff...");
			Thread.sleep(500L);
			LOG.info("Done loading");
		}

		@Override
		protected void shutDown() throws Exception {
			LOG.info("Loader shutting down");
		}

		@Override
		protected String serviceName() {
			return "loader";
		}
	};
	private final Service processor = new AbstractIdleService() {
		@Override
		protected void startUp() throws Exception {
			LOG.info("Starting processor service");
			startedProcessing.put(true);
		}

		@Override
		protected void shutDown() throws Exception {
			LOG.info("Stopping processor service");
		}

		@Override
		protected String serviceName() {
			return "processor";
		}
	};

	@Test
	public void services_are_new_after_construction() throws Exception {
		SpooledEventProcessor sep = new SpooledEventProcessor(loader, processor);
		assertThat(sep.state(), equalTo(Service.State.NEW));
		assertThat(loader.state(), equalTo(Service.State.NEW));
		assertThat(processor.state(), equalTo(Service.State.NEW));
	}

	@Test
	public void go() throws Exception {
		SpooledEventProcessor sep = new SpooledEventProcessor(loader, processor);
		sep.startAsync().awaitRunning(2, TimeUnit.SECONDS);
		Boolean started = startedProcessing.poll(2, TimeUnit.SECONDS);
		assertThat(started, equalTo(true));
		assertThat(loader.state(), equalTo(Service.State.TERMINATED));
		sep.stopAsync().awaitTerminated(2, TimeUnit.SECONDS);
		assertThat(processor.state(), equalTo(Service.State.TERMINATED));
		assertThat(sep.state(), equalTo(Service.State.TERMINATED));
	}
}

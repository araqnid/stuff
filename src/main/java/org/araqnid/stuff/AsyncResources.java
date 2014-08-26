package org.araqnid.stuff;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("async")
public class AsyncResources {
	private static final Logger LOG = LoggerFactory.getLogger(AsyncResources.class);
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);

	@GET
	public void get(@Suspended final AsyncResponse response) {
		LOG.info("begin");
		executor.schedule(new Runnable() {
			@Override
			public void run() {
				LOG.info("finish");
				response.resume("result!");
			}
		}, 2, TimeUnit.SECONDS);
	}

	@PreDestroy
	public void cleanup() {
		executor.shutdown();
		try {
			executor.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOG.warn("Failed to shutdown executor threads: " + e);
		}
	}
}

package org.araqnid.stuff;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.servlet.ServletScopes;
import com.surftools.BeanstalkClient.Client;
import com.surftools.BeanstalkClient.Job;

public class BeanstalkProcessor implements AppService {
	private static final Logger LOG = LoggerFactory.getLogger(BeanstalkProcessor.class);
	private final Provider<Client> connectionProvider;
	private final String tubeName;
	private final Provider<RequestActivity> requestStateProvider;
	private final Provider<? extends DeliveryTarget> targetProvider;
	private final ExecutorService executor;
	private final AtomicBoolean stopRequest;
	private final int maxThreads;

	public BeanstalkProcessor(Provider<Client> connectionProvider, String tubeName, int maxThreads,
			Provider<RequestActivity> requestStateProvider, Provider<? extends DeliveryTarget> targetProvider) {
		this.connectionProvider = connectionProvider;
		this.tubeName = tubeName;
		this.requestStateProvider = requestStateProvider;
		this.targetProvider = targetProvider;
		this.executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat(
				"beanstalk-" + tubeName + "-%d").build());
		this.stopRequest = new AtomicBoolean(false);
		this.maxThreads = maxThreads;
	}

	@Override
	public void start() {
		LOG.info("Starting");
		for (int i = 0; i < maxThreads; i++) {
			executor.execute(new TubeConsumer());
		}
	}

	@Override
	public void stop() {
		LOG.info("Requesting stop");
		stopRequest.set(true);
		executor.shutdown();
		try {
			executor.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOG.warn("Waiting for executor shutdown interrupted: " + e);
		}
		LOG.info("Stopped");
	}

	private boolean deliver(Job job) {
		PushedMdcValue queue = new PushedMdcValue("queue", tubeName);
		PushedMdcValue jobId = new PushedMdcValue("jobId", String.valueOf(job.getJobId()));
		try {
			return deliverWithinGuiceScope(job);
		} finally {
			PushedMdcValue.restoreValues(queue, jobId);
		}
	}

	private boolean deliverWithinGuiceScope(final Job job) {
		ImmutableMap<Key<?>, Object> seedMap = ImmutableMap.<Key<?>, Object> of(Key.get(Job.class), job);
		Callable<Boolean> scoped = ServletScopes.scopeRequest(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return dispatchDelivery(job);
			}
		}, seedMap);
		try {
			return scoped.call();
		} catch (Exception e) {
			LOG.error("Fatal exception processing Beanstalk job", e);
			return false;
		}
	}

	private boolean dispatchDelivery(Job job) {
		RequestActivity requestActivity = requestStateProvider.get();
		requestActivity.beginRequest("BJP", Joiner.on('\t').join(tubeName, job.getJobId()));
		try {
			return targetProvider.get().deliver(job.getData());
		} finally {
			requestActivity.finishRequest("BJP");
		}
	}

	private final class TubeConsumer implements Runnable {
		private Client connection;
		private Logger log = LoggerFactory.getLogger("jobs." + tubeName);

		@Override
		public void run() {
			connection = connectionProvider.get();
			log.debug("Got connection {}", connection);
			if (!tubeName.equals("default")) {
				connection.watch(tubeName);
				connection.ignore("default");
			}
			connection.useTube(tubeName);
			log.info("Listening for jobs");
			while (true) {
				if (stopRequest.get()) {
					break;
				}
				log.debug("Reserving job");
				Job job = connection.reserve(2);
				if (job != null) {
					log.debug("<{}> starting", job.getJobId());
					if (deliver(job)) {
						connection.delete(job.getJobId());
						log.info("<{}> processed", job.getJobId());
					}
					else {
						connection.release(job.getJobId(), 0, 5);
						log.info("<{}> failed temporarily", job.getJobId());
					}
				}
			}
			log.info("Closing connection");
			connection.close();
			connection = null;
		}
	}

	public interface DeliveryTarget {
		boolean deliver(byte[] data);
	}

	public static final class PushedMdcValue {
		private final String key;
		private final String oldValue;

		public PushedMdcValue(String key, String newValue) {
			this.key = key;
			oldValue = MDC.get(key);
			MDC.put(key, newValue);
		}

		public void restore() {
			if (oldValue != null) {
				MDC.put(key, oldValue);
			}
			else {
				MDC.remove(key);
			}
		}

		public static void restoreValues(PushedMdcValue... values) {
			for (PushedMdcValue value : values) {
				value.restore();
			}
		}
	}
}

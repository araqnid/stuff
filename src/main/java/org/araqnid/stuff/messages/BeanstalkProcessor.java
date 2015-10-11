package org.araqnid.stuff.messages;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.surftools.BeanstalkClient.BeanstalkException;
import com.surftools.BeanstalkClient.Client;
import com.surftools.BeanstalkClient.Job;

public class BeanstalkProcessor<T extends BeanstalkProcessor.DeliveryTarget> extends AbstractExecutionThreadService {
	private static final Logger LOG = LoggerFactory.getLogger(BeanstalkProcessor.class);
	private final Provider<Client> connectionProvider;
	private final String tubeName;
	private final Provider<T> targetProvider;
	private final Logger log;
	private boolean completed = false;
	private boolean haltRequested = false;
	private Client connection;

	public BeanstalkProcessor(Provider<Client> connectionProvider, String tubeName, Provider<T> targetProvider) {
		this.connectionProvider = connectionProvider;
		this.tubeName = tubeName;
		this.targetProvider = targetProvider;
		this.log = LoggerFactory.getLogger(BeanstalkProcessor.class.getName() + "." + tubeName);
	}

	@Override
	protected void run() throws Exception {
		LOG.info("Consuming from tube \"{}\"", tubeName);
		try {
			connect();
			try {
				consume();
			} finally {
				clearConnection();
			}
		} finally {
			synchronized (this) {
				completed = true;
			}
		}
	}

	@Override
	protected void triggerShutdown() {
		synchronized (this) {
			if (completed) return;
			haltRequested = true;
		}
		connection.close();
	}

	@Override
	protected String serviceName() {
		return "BeanstalkProcessor-" + tubeName;
	}

	private void consume() {
		setupConnection();
		log.debug("Listening for jobs");
		while (!isHaltRequested()) {
			log.debug("Reserving job");
			Job job;
			try {
				job = connection.reserve(100);
			} catch (BeanstalkException e) {
				if (isHaltRequested()) {
					log.debug("Error reserving job -- ignoring during halt", e);
				}
				else {
					log.error("Error reserving job", e);
				}
				return;
			}
			if (job == null) continue;
			log.debug("<{}> reserved", job.getJobId());
			try {
				if (deliver(job)) {
					connection.delete(job.getJobId());
					log.info("<{}> deleted", job.getJobId());
				}
				else {
					connection.release(job.getJobId(), 0, 5);
					log.info("<{}> released", job.getJobId());
				}
			} catch (Exception e) {
				connection.release(job.getJobId(), 0, 5);
				log.info("<{}> released after unhandled exception", job.getJobId(), e);
			}
		}
		log.debug("Closing connection");
		connection.close();
	}

	private void setupConnection() {
		if (!tubeName.equals("default")) {
			connection.watch(tubeName);
			connection.ignore("default");
		}
		connection.useTube(tubeName);
	}

	private void connect() {
		Client newConnection = connectionProvider.get();
		synchronized (this) {
			connection = newConnection;
		}
	}

	private synchronized void clearConnection() {
		connection = null;
	}

	private synchronized boolean isHaltRequested() {
		return haltRequested;
	}

	private boolean deliver(Job job) {
		PushedMdcValue queue = new PushedMdcValue("queue", tubeName);
		PushedMdcValue jobId = new PushedMdcValue("jobId", String.valueOf(job.getJobId()));
		try {
			return dispatchDelivery(job);
		} finally {
			PushedMdcValue.restoreValues(queue, jobId);
		}
	}

	private boolean dispatchDelivery(Job job) {
		return targetProvider.get().deliver(job.getData());
	}

	@Override
	public String toString() {
		return "BeanstalkProcessor:" + tubeName + " => " + targetProvider + " [" + state() + "]";
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

package org.araqnid.stuff;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.araqnid.stuff.activity.ActivityScopeControl;
import org.araqnid.stuff.activity.AppRequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ExecutionList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Provider;
import com.surftools.BeanstalkClient.BeanstalkException;
import com.surftools.BeanstalkClient.Client;
import com.surftools.BeanstalkClient.Job;

public class BeanstalkProcessor extends AbstractService {
	private static final Logger LOG = LoggerFactory.getLogger(BeanstalkProcessor.class);
	private final Provider<Client> connectionProvider;
	private final String tubeName;
	private final ActivityScopeControl scopeControl;
	private final Provider<? extends DeliveryTarget> targetProvider;
	private final ExecutorService executor;
	private final int maxThreads;
	private final ConcurrentLinkedDeque<TubeConsumer> consumers = new ConcurrentLinkedDeque<>();

	public BeanstalkProcessor(Provider<Client> connectionProvider,
			String tubeName,
			int maxThreads,
			ActivityScopeControl scopeControl,
			Provider<? extends DeliveryTarget> targetProvider) {
		this.connectionProvider = connectionProvider;
		this.tubeName = tubeName;
		this.scopeControl = scopeControl;
		this.targetProvider = targetProvider;
		this.executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat(
				"beanstalk-" + tubeName + "-%d").build());
		this.maxThreads = maxThreads;
	}

	@Override
	protected void doStart() {
		LOG.info("Consuming from tube \"{}\" with {} thread(s)", tubeName, maxThreads);
		for (int i = 0; i < maxThreads; i++) {
			final TubeConsumer consumer = new TubeConsumer();
			consumer.addCompletionListener(new Runnable() {
				@Override
				public void run() {
					consumers.remove();
					// TODO schedule reconnecting
				}
			}, MoreExecutors.sameThreadExecutor());
			consumers.add(consumer);
			executor.execute(consumer);
		}
		notifyStarted();
	}

	@Override
	protected void doStop() {
		executor.shutdown();
		Futures.allAsList(Iterables.transform(consumers, new Function<TubeConsumer, ListenableFuture<?>>() {
			@Override
			public ListenableFuture<?> apply(TubeConsumer consumer) {
				final SettableFuture<Boolean> future = SettableFuture.create();
				consumer.halt();
				consumer.addCompletionListener(new Runnable() {
					@Override
					public void run() {
						future.set(true);
					}
				}, MoreExecutors.sameThreadExecutor());
				return future;
			}
		})).addListener(new Runnable() {
			@Override
			public void run() {
				LOG.info("Stopped consuming from tube \"{}\"", tubeName);
				notifyStopped();
			}
		}, MoreExecutors.sameThreadExecutor());
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
		scopeControl.beginRequest(AppRequestType.BeanstalkMessage, Joiner.on('\t').join(tubeName, job.getJobId()));
		try {
			return targetProvider.get().deliver(job.getData());
		} finally {
			scopeControl.finishRequest(AppRequestType.BeanstalkMessage);
		}
	}

	@Override
	public String toString() {
		return "BeanstalkProcessor:" + tubeName + " => " + targetProvider + " [" + state() + "]";
	}

	private final class TubeConsumer implements Runnable {
		private Client connection;
		private Logger log = LoggerFactory.getLogger(BeanstalkProcessor.class.getName() + "." + tubeName);
		private ExecutionList completionListeners = new ExecutionList();
		private boolean completed;
		private boolean haltRequested;

		@Override
		public void run() {
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
				completionListeners.execute();
			}
		}

		public void halt() {
			synchronized (this) {
				if (completed) return;
				haltRequested = true;
			}
			connection.close();
		}

		public void addCompletionListener(Runnable runnable, Executor executor) {
			completionListeners.add(runnable, executor);
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
					log.info("<{}> released after unhandled exception", job.getJobId());
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

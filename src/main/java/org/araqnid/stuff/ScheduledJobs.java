package org.araqnid.stuff;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.servlet.ServletScopes;

public class ScheduledJobs implements AppService {
	private static final Logger LOG = LoggerFactory.getLogger(ScheduledJobs.class);
	private static final Provider<ScheduledExecutorService> EXECUTOR_FACTORY = new Provider<ScheduledExecutorService>() {
		@Override
		public ScheduledExecutorService get() {
			return new ScheduledThreadPoolExecutor(16, new ThreadFactoryBuilder().setNameFormat("Scheduled-%d").build());
		}
	};

	private final Set<JobDefinition> jobs;
	private final Provider<RequestActivity> requestStateProvider;
	private final Provider<ScheduledExecutorService> executorProvider;
	private ScheduledExecutorService executorService;

	@Inject
	public ScheduledJobs(Provider<RequestActivity> requestStateProvider, Set<JobDefinition> jobs) {
		this(requestStateProvider, EXECUTOR_FACTORY, jobs);
	}

	public ScheduledJobs(Provider<RequestActivity> requestStateProvider,
			Provider<ScheduledExecutorService> executorProvider, Set<JobDefinition> jobs) {
		this.requestStateProvider = requestStateProvider;
		this.executorProvider = executorProvider;
		this.jobs = jobs;
	}

	@Override
	public void start() {
		LOG.info("Starting job scheduler");
		executorService = executorProvider.get();
		for (JobDefinition definition : jobs) {
			executorService.scheduleAtFixedRate(withRequestScope(definition.body), definition.delay,
					definition.interval, TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public void stop() {
		LOG.info("Stopping job scheduler");
		executorService.shutdown();
		try {
			executorService.awaitTermination(2, TimeUnit.MINUTES);
		} catch (Exception e) {
			LOG.warn("Unable to wait for job shutdown", e);
		}
	}

	public Set<JobDefinition> getJobs() {
		return jobs;
	}

	private Runnable withRequestScope(final Runnable underlying) {
		return new Runnable() {
			@Override
			public void run() {
				Map<Key<?>, Object> seedMap = Collections.emptyMap();
				Callable<Void> scopeRequest = ServletScopes.scopeRequest(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						RequestActivity requestActivity = requestStateProvider.get();
						requestActivity.beginRequest("SCH", underlying.toString());
						try {
							underlying.run();
							return null;
						} finally {
							requestActivity.finishRequest("SCH");
						}
					}
				}, seedMap);
				try {
					scopeRequest.call();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	public static final class JobDefinition {
		public final Runnable body;
		public final long delay;
		public final long interval;

		public JobDefinition(Runnable body, long delay, long interval) {
			this.body = body;
			this.delay = delay;
			this.interval = interval;
		}
	}

	public static class CacheRefresher implements Runnable {
		private final RequestActivity requestActivity;

		@Inject
		public CacheRefresher(RequestActivity requestActivity) {
			this.requestActivity = requestActivity;
		}

		@Override
		public void run() {
			LOG.info("{} Cache refresh", requestActivity.getRuid());
		}
	}
}

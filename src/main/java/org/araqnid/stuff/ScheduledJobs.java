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
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletScopes;

public class ScheduledJobs implements AppService {
	private static final Logger LOG = LoggerFactory.getLogger(ScheduledJobs.class);
	private final Set<Provider<? extends Runnable>> jobs;
	private final Provider<RequestActivity> requestStateProvider;
	private ScheduledExecutorService executorService;

	@Inject
	public ScheduledJobs(Provider<RequestActivity> requestStateProvider, Set<Provider<? extends Runnable>> jobs) {
		this.requestStateProvider = requestStateProvider;
		this.jobs = jobs;
	}

	@Override
	public void start() {
		LOG.info("Starting job scheduler");
		executorService = new ScheduledThreadPoolExecutor(16, new ThreadFactoryBuilder().setNameFormat("Scheduled-%d")
				.build());
		for (Provider<? extends Runnable> provider : jobs) {
			executorService.scheduleAtFixedRate(withRequestScope(jobProvidedBy(provider)), 0, 5, TimeUnit.SECONDS);
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

	private <T extends Runnable> Runnable jobProvidedBy(final Provider<T> provider) {
		return new Runnable() {
			@Override
			public void run() {
				provider.get().run();
			}

			@Override
			public String toString() {
				return provider.toString();
			}
		};
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

	@Singleton
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

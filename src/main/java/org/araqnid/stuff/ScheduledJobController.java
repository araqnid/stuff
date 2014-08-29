package org.araqnid.stuff;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.araqnid.stuff.activity.ActivityScopeControl;
import org.araqnid.stuff.activity.AppRequestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ScheduledJobController extends AbstractIdleService {
	private static final Logger LOG = LoggerFactory.getLogger(ScheduledJobController.class);
	private static final Provider<ScheduledExecutorService> EXECUTOR_FACTORY = new Provider<ScheduledExecutorService>() {
		@Override
		public ScheduledExecutorService get() {
			return new ScheduledThreadPoolExecutor(16, new ThreadFactoryBuilder().setNameFormat("Scheduled-%d").build());
		}
	};

	private final ActivityScopeControl scopeControl;
	private final Set<JobDefinition> jobs;
	private final Provider<ScheduledExecutorService> executorProvider;
	private ScheduledExecutorService executorService;

	@Inject
	public ScheduledJobController(ActivityScopeControl scopeControl, Set<JobDefinition> jobs) {
		this(EXECUTOR_FACTORY, scopeControl, jobs);
	}

	public ScheduledJobController(Provider<ScheduledExecutorService> executorProvider,
			ActivityScopeControl scopeControl,
			Set<JobDefinition> jobs) {
		this.executorProvider = executorProvider;
		this.scopeControl = scopeControl;
		this.jobs = jobs;
	}

	@Override
	protected void startUp() throws Exception {
		executorService = executorProvider.get();
		for (JobDefinition definition : jobs) {
			executorService.scheduleAtFixedRate(withRequestScope(definition.body), definition.delay,
					definition.interval, TimeUnit.MILLISECONDS);
		}
	}

	@Override
	protected void shutDown() throws Exception {
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
				scopeControl.beginRequest(AppRequestType.ScheduledJob, underlying.toString());
				try {
					underlying.run();
				} finally {
					scopeControl.finishRequest(AppRequestType.ScheduledJob);
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
}

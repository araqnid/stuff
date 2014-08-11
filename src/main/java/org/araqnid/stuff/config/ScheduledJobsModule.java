package org.araqnid.stuff.config;

import java.util.List;
import java.util.Set;

import org.araqnid.stuff.RequestActivity;
import org.araqnid.stuff.ScheduledJobs;
import org.araqnid.stuff.ScheduledJobs.JobDefinition;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.ProviderWithDependencies;

public abstract class ScheduledJobsModule extends AbstractModule {
	private Builder builder;

	@Override
	protected void configure() {
		builder = new Builder();
		try {
			configureJobs();
			install(builder);
		} finally {
			builder = null;
		}
	}

	protected abstract void configureJobs();

	protected <T extends Runnable> JobBinding<T> run(Class<T> clazz) {
		return builder.createJobBinding(Key.get(clazz));
	}

	protected <T extends Runnable> JobBinding<T> run(Key<T> key) {
		return builder.createJobBinding(key);
	}

	private static class Builder implements Module {
		private final List<JobBinding<?>> bindings = Lists.newArrayList();

		@Override
		public void configure(Binder binder) {
			Set<Dependency<?>> dependencies = Sets.<Dependency<?>>newHashSet(Dependency.get(Key.get(RequestActivity.class)));
			Set<JobDefinition> jobProviders = Sets.newHashSet();
			for (JobBinding<?> job : bindings) {
				dependencies.add(Dependency.get(job.key));
				jobProviders.add(new JobDefinition(job.asRunnable(binder), job.delay, job.interval));
			}
			binder.bind(ScheduledJobs.class).toProvider(
					new ScheduledJobsProvider(ImmutableSet.copyOf(dependencies), binder.getProvider(RequestActivity.class), jobProviders));
		}

		public <T extends Runnable> JobBinding<T> createJobBinding(Key<T> key) {
			JobBinding<T> job = new JobBinding<T>(key);
			bindings.add(job);
			return job;
		}
	}

	private static final class ProvidedJobRunner<T extends Runnable> implements Runnable {
		private final Provider<T> provider;

		public ProvidedJobRunner(Provider<T> provider) {
			this.provider = provider;
		}

		@Override
		public void run() {
			provider.get().run();
		}

		@Override
		public String toString() {
			return provider.toString();
		}
	}

	private static final class ScheduledJobsProvider implements ProviderWithDependencies<ScheduledJobs> {
		private final Set<Dependency<?>> dependencies;
		private final Provider<RequestActivity> requestStateProvider;
		private final Set<JobDefinition> jobProviders;

		public ScheduledJobsProvider(Set<Dependency<?>> dependencies, Provider<RequestActivity> requestStateProvider,
				Set<JobDefinition> jobProviders) {
			this.dependencies = dependencies;
			this.requestStateProvider = requestStateProvider;
			this.jobProviders = jobProviders;
		}

		@Override
		public Set<Dependency<?>> getDependencies() {
			return dependencies;
		}

		@Override
		public ScheduledJobs get() {
			return new ScheduledJobs(requestStateProvider, jobProviders);
		}
	}

	public static class JobBinding<T extends Runnable> {
		private final Key<T> key;
		private long interval = 2000L;
		private long delay = -1L;

		public JobBinding(Key<T> key) {
			this.key = key;
		}

		public JobBinding<T> withDelayedStartup(long delay) {
			this.delay = delay;
			return this;
		}

		public JobBinding<T> withInterval(long interval) {
			this.interval = interval;
			return this;
		}

		private Runnable asRunnable(Binder binder) {
			return new ProvidedJobRunner<T>(binder.getProvider(key));
		}
	}
}

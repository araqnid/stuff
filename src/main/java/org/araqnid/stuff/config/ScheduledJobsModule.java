package org.araqnid.stuff.config;

import java.util.List;

import org.araqnid.stuff.ScheduledJobController;
import org.araqnid.stuff.ScheduledJobController.JobDefinition;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

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
			Multibinder<JobDefinition> jobs = Multibinder.newSetBinder(binder, JobDefinition.class);
			for (JobBinding<?> job : bindings) {
				jobs.addBinding().toInstance(new JobDefinition(job.asRunnable(binder), job.delay, job.interval));
			}
			binder.bind(ScheduledJobController.class).in(Singleton.class);
		}

		public <T extends Runnable> JobBinding<T> createJobBinding(Key<T> key) {
			JobBinding<T> job = new JobBinding<T>(key);
			bindings.add(job);
			return job;
		}
	}

	static final class ProvidedJobRunner<T extends Runnable> implements Runnable {
		final Provider<T> provider;

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

	public static class JobBinding<T extends Runnable> {
		private final Key<T> key;
		private long interval = 2000L;
		private long delay = 0L;

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

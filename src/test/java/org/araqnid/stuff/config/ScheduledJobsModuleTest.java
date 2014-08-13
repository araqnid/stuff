package org.araqnid.stuff.config;

import java.util.Random;

import org.araqnid.stuff.RequestActivity.ActivityEventSink;
import org.araqnid.stuff.ScheduledJobController;
import org.araqnid.stuff.ScheduledJobController.JobDefinition;
import org.araqnid.stuff.config.ScheduledJobsModule.ProvidedJobRunner;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class ScheduledJobsModuleTest {
	private Injector baseInjector;
	private ActivityEventSink activityEventSink = Mockito.mock(ActivityEventSink.class);

	@Before
	public void setup() {
		baseInjector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindScope(ActivityScoped.class, ActivityScope.SCOPE);
				bind(ActivityEventSink.class).toInstance(activityEventSink);
			}
		});
	}

	@Test
	public void creates_a_job_with_default_configuration() {
		Injector injector = baseInjector.createChildInjector(new ScheduledJobsModule() {
			@Override
			protected void configureJobs() {
				run(TestJob.class);
			}
		});
		ScheduledJobController jobs = injector.getInstance(ScheduledJobController.class);
		MatcherAssert.assertThat(
				jobs.getJobs(),
				Matchers.contains(aJob().calling(TestJob.class).havingDelay(Matchers.equalTo(0L))
						.havingInterval(Matchers.equalTo(2000L))));
	}

	@Test
	public void creates_a_job_with_specified_delay() {
		final long delay = new Random().nextLong();
		Injector injector = baseInjector.createChildInjector(new ScheduledJobsModule() {
			@Override
			protected void configureJobs() {
				run(TestJob.class).withDelayedStartup(delay);
			}
		});
		ScheduledJobController jobs = injector.getInstance(ScheduledJobController.class);
		MatcherAssert.assertThat(jobs.getJobs(), Matchers.contains(aJob().havingDelay(Matchers.equalTo(delay))));
	}

	@Test
	public void creates_a_job_with_specified_interval() {
		final long interval = new Random().nextLong();
		Injector injector = baseInjector.createChildInjector(new ScheduledJobsModule() {
			@Override
			protected void configureJobs() {
				run(TestJob.class).withInterval(interval);
			}
		});
		ScheduledJobController jobs = injector.getInstance(ScheduledJobController.class);
		MatcherAssert.assertThat(jobs.getJobs(), Matchers.contains(aJob().havingInterval(Matchers.equalTo(interval))));
	}

	private static <T extends Runnable> JobMatcher<T> aJob() {
		return new JobMatcher<T>();
	}

	private static final class JobMatcher<T extends Runnable> extends TypeSafeDiagnosingMatcher<JobDefinition> {
		private Matcher<Runnable> bodyMatcher;
		private Matcher<Long> delayMatcher;
		private Matcher<Long> intervalMatcher;

		@Override
		protected boolean matchesSafely(JobDefinition item, Description mismatchDescription) {
			if (bodyMatcher != null && !bodyMatcher.matches(item.body)) {
				mismatchDescription.appendText("job body ");
				bodyMatcher.describeMismatch(item.body, mismatchDescription);
				return false;
			}
			if (delayMatcher != null && !delayMatcher.matches(item.delay)) {
				mismatchDescription.appendText("startup delay ");
				delayMatcher.describeMismatch(item.delay, mismatchDescription);
				return false;
			}
			if (intervalMatcher != null && !intervalMatcher.matches(item.interval)) {
				mismatchDescription.appendText("interval ");
				intervalMatcher.describeMismatch(item.interval, mismatchDescription);
				return false;
			}
			return true;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("a job");
			if (bodyMatcher != null) description.appendText(" with body ").appendDescriptionOf(bodyMatcher);
			if (delayMatcher != null) description.appendText(" with delay ").appendDescriptionOf(delayMatcher);
			if (intervalMatcher != null) description.appendText(" with interval ").appendDescriptionOf(intervalMatcher);
		}

		public JobMatcher<T> calling(final Class<? extends Runnable> bodyClass) {
			bodyMatcher = new TypeSafeDiagnosingMatcher<Runnable>(Runnable.class) {
				@Override
				protected boolean matchesSafely(Runnable item, Description mismatchDescription) {
					if (!(item instanceof ProvidedJobRunner)) {
						mismatchDescription.appendValue(item).appendText(" is not a ProvidedJobRunner");
						return false;
					}
					ProvidedJobRunner<?> runner = (ProvidedJobRunner<?>) item;
					Runnable result = runner.provider.get();
					if (!bodyClass.isAssignableFrom(result.getClass())) {
						mismatchDescription.appendValue(result).appendText(" is not a ").appendValue(bodyClass);
						return false;
					}
					return true;
				}

				@Override
				public void describeTo(Description description) {
					description.appendText("calling ").appendValue(bodyClass.getName());
				}
			};
			return this;
		}

		public JobMatcher<T> havingDelay(Matcher<Long> delayMatcher) {
			this.delayMatcher = delayMatcher;
			return this;
		}

		public JobMatcher<T> havingInterval(Matcher<Long> intervalMatcher) {
			this.intervalMatcher = intervalMatcher;
			return this;
		}
	}

	private static final class TestJob implements Runnable {
		@Override
		public void run() {
		}
	}
}

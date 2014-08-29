package org.araqnid.stuff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.araqnid.stuff.ScheduledJobController.JobDefinition;
import org.araqnid.stuff.activity.ActivityScopeControl;
import org.araqnid.stuff.activity.ActivityScoped;
import org.araqnid.stuff.activity.AppRequestType;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Scope;

import static org.araqnid.stuff.testutil.RandomData.randomString;

public class ScheduledJobControllerTest {
	private final Provider<ScheduledExecutorService> executorProvider = new Provider<ScheduledExecutorService>() {
		@Override
		public ScheduledThreadPoolExecutor get() {
			return mockExecutor;
		}
	};
	private final ScheduledThreadPoolExecutor mockExecutor = Mockito.mock(ScheduledThreadPoolExecutor.class);
	private final List<Runnable> scheduledJobBodies = new ArrayList<>();
	private final ScheduledFuture<?> mockFuture = Mockito.mock(ScheduledFuture.class);
	private final Map<Key<?>, Object> testScopeContent = new HashMap<>();
	private final Scope testScope = new Scope() {
		@Override
		public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
			return new Provider<T>() {
				@Override
				public T get() {
					if (!inScope) throw new OutOfScopeException("Not in scope");
					@SuppressWarnings("unchecked")
					T value = (T) testScopeContent.get(key);
					if (value == null) {
						value = unscoped.get();
						testScopeContent.put(key, value);
					}
					return value;
				}
			};
		}
	};
	private final ActivityScopeControl mockScopeControl = Mockito.mock(ActivityScopeControl.class);
	private final ActivityScopeControl scopeControl = new ActivityScopeControl() {
		@Override
		public void beginRequest(String ruid, AppRequestType type, String description) {
			inScope = true;
			mockScopeControl.beginRequest(ruid, type, description);
		}

		@Override
		public void beginRequest(AppRequestType type, String description) {
			inScope = true;
			mockScopeControl.beginRequest(type, description);
		}

		@Override
		public void finishRequest(AppRequestType type) {
			inScope = false;
			mockScopeControl.finishRequest(type);
		}
	};
	private Injector injector;
	private boolean inScope;

	@Before
	public void setupInjector() {
		injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindScope(ActivityScoped.class, testScope);
				bind(ActivityScopedThing.class).in(ActivityScoped.class);
			}
		});
	}

	@Test
	public void no_jobs_are_scheduled_on_construction() throws Exception {
		Random random = new Random();
		long delay = random.nextLong();
		long interval = random.nextLong();
		ScheduledJobController.JobDefinition job = new ScheduledJobController.JobDefinition(
				Mockito.mock(Runnable.class), delay, interval);
		new ScheduledJobController(executorProvider, scopeControl, ImmutableSet.of(job));
		Mockito.verifyZeroInteractions(mockExecutor);
	}

	@Test
	public void jobs_are_scheduled_on_startup() throws Exception {
		Random random = new Random();
		long delay = random.nextLong();
		long interval = random.nextLong();
		ScheduledJobController.JobDefinition job = new ScheduledJobController.JobDefinition(
				Mockito.mock(Runnable.class), delay, interval);
		ScheduledJobController controller = new ScheduledJobController(executorProvider, scopeControl,
				ImmutableSet.of(job));
		controller.startAsync().awaitRunning();
		Mockito.verify(mockExecutor).scheduleAtFixedRate(Mockito.any(Runnable.class), Mockito.eq(delay),
				Mockito.eq(interval), Mockito.eq(TimeUnit.MILLISECONDS));
	}

	@Test
	public void executor_is_terminated_on_stop() throws Exception {
		ScheduledJobController controller = new ScheduledJobController(executorProvider, scopeControl,
				ImmutableSet.<JobDefinition> of());
		controller.startAsync().awaitRunning();
		controller.stopAsync().awaitTerminated();
		Mockito.verify(mockExecutor).shutdown();
		Mockito.verify(mockExecutor).awaitTermination(Mockito.anyLong(), Mockito.any(TimeUnit.class));
	}

	@Test
	public void scheduled_jobs_call_through_to_body() throws Exception {
		Random random = new Random();
		Runnable jobBody = Mockito.mock(Runnable.class);
		ScheduledJobController.JobDefinition job = new ScheduledJobController.JobDefinition(jobBody, random.nextLong(),
				random.nextLong());
		ScheduledJobController controller = new ScheduledJobController(executorProvider, scopeControl,
				ImmutableSet.of(job));
		captureSubmittedJobs();
		controller.startAsync().awaitRunning();
		scheduledJobBodies.get(0).run();
		Mockito.verify(jobBody).run();
	}

	@Test
	public void scheduled_job_execution_is_wrapped_in_activity_scope() throws Exception {
		final List<ActivityScopedThing> activityScopedThings = new ArrayList<>();
		Runnable jobBody = new Runnable() {
			@Override
			public void run() {
				try {
					activityScopedThings.add(injector.getInstance(ActivityScopedThing.class));
				} catch (ProvisionException e) {
					if (e.getCause() instanceof OutOfScopeException) {
						Assert.fail("Job body not in activity scope");
					}
					throw e;
				}
			}
		};
		Random random = new Random();
		ScheduledJobController.JobDefinition job = new ScheduledJobController.JobDefinition(jobBody, random.nextLong(),
				random.nextLong());
		ScheduledJobController controller = new ScheduledJobController(executorProvider, scopeControl,
				ImmutableSet.of(job));
		captureSubmittedJobs();
		controller.startAsync().awaitRunning();
		scheduledJobBodies.get(0).run();
		MatcherAssert.assertThat(activityScopedThings,
				Matchers.contains(Matchers.instanceOf(ActivityScopedThing.class)));
	}

	@Test
	public void scheduled_jobs_initialise_request_activity() throws Exception {
		final String jobString = randomString();
		Runnable jobBody = new Runnable() {
			@Override
			public void run() {
			}

			@Override
			public String toString() {
				return jobString;
			}
		};
		Random random = new Random();
		ScheduledJobController.JobDefinition job = new ScheduledJobController.JobDefinition(jobBody, random.nextLong(),
				random.nextLong());
		ScheduledJobController controller = new ScheduledJobController(executorProvider, scopeControl,
				ImmutableSet.of(job));
		captureSubmittedJobs();
		controller.startAsync().awaitRunning();
		scheduledJobBodies.get(0).run();
		Mockito.verify(mockScopeControl).beginRequest(Mockito.eq(AppRequestType.ScheduledJob),
				Mockito.argThat(Matchers.stringContainsInOrder(ImmutableList.of(jobString))));
		Mockito.verify(mockScopeControl).finishRequest(AppRequestType.ScheduledJob);
		Mockito.verifyNoMoreInteractions(mockScopeControl);
	}

	private void captureSubmittedJobs() {
		Mockito.when(
				mockExecutor.scheduleAtFixedRate(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.anyLong(),
						Mockito.any(TimeUnit.class))).then(new Answer<ScheduledFuture<?>>() {
			@Override
			public ScheduledFuture<?> answer(InvocationOnMock invocation) throws Throwable {
				scheduledJobBodies.add((Runnable) invocation.getArguments()[0]);
				return mockFuture;
			}
		});
	}

	public static final class ActivityScopedThing {
	}
}

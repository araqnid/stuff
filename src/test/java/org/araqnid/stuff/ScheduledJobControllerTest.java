package org.araqnid.stuff;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.araqnid.stuff.ScheduledJobController.JobDefinition;
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
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.ServletScopes;

public class ScheduledJobControllerTest {
	private final Provider<RequestActivity> requestStateProvider = new Provider<RequestActivity>() {
		@Override
		public RequestActivity get() {
			return requestActivity;
		}
	};
	private final Provider<ScheduledExecutorService> executorProvider = new Provider<ScheduledExecutorService>() {
		@Override
		public ScheduledThreadPoolExecutor get() {
			return mockExecutor;
		}
	};
	private final RequestActivity requestActivity = Mockito.mock(RequestActivity.class);
	private final ScheduledThreadPoolExecutor mockExecutor = Mockito.mock(ScheduledThreadPoolExecutor.class);
	private final List<Runnable> scheduledJobBodies = new ArrayList<>();
	private final ScheduledFuture<?> mockFuture = Mockito.mock(ScheduledFuture.class);
	private Injector injector;

	@Before
	public void setupInjector() {
		injector = Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bindScope(RequestScoped.class, ServletScopes.REQUEST);
				bind(RequestScopedThing.class).in(RequestScoped.class);
			}
		});
	}

	@Test
	public void no_jobs_are_scheduled_on_construction() throws Exception {
		Random random = new Random();
		long delay = random.nextLong();
		long interval = random.nextLong();
		ScheduledJobController.JobDefinition job = new ScheduledJobController.JobDefinition(Mockito.mock(Runnable.class), delay, interval);
		new ScheduledJobController(requestStateProvider, executorProvider, ImmutableSet.of(job));
		Mockito.verifyZeroInteractions(mockExecutor);
	}

	@Test
	public void jobs_are_scheduled_on_startup() throws Exception {
		Random random = new Random();
		long delay = random.nextLong();
		long interval = random.nextLong();
		ScheduledJobController.JobDefinition job = new ScheduledJobController.JobDefinition(Mockito.mock(Runnable.class), delay, interval);
		ScheduledJobController controller = new ScheduledJobController(requestStateProvider, executorProvider, ImmutableSet.of(job));
		controller.start();
		Mockito.verify(mockExecutor).scheduleAtFixedRate(Mockito.any(Runnable.class), Mockito.eq(delay),
				Mockito.eq(interval), Mockito.eq(TimeUnit.MILLISECONDS));
	}

	@Test
	public void executor_is_terminated_on_stop() throws Exception {
		ScheduledJobController controller = new ScheduledJobController(requestStateProvider, executorProvider,
				ImmutableSet.<JobDefinition> of());
		controller.start();
		controller.stop();
		Mockito.verify(mockExecutor).shutdown();
		Mockito.verify(mockExecutor).awaitTermination(Mockito.anyLong(), Mockito.any(TimeUnit.class));
	}

	@Test
	public void scheduled_jobs_call_through_to_body() throws Exception {
		Random random = new Random();
		Runnable jobBody = Mockito.mock(Runnable.class);
		ScheduledJobController.JobDefinition job = new ScheduledJobController.JobDefinition(jobBody, random.nextLong(), random.nextLong());
		ScheduledJobController controller = new ScheduledJobController(requestStateProvider, executorProvider, ImmutableSet.of(job));
		captureSubmittedJobs();
		controller.start();
		scheduledJobBodies.get(0).run();
		Mockito.verify(jobBody).run();
	}

	@Test
	public void scheduled_job_execution_is_wrapped_in_request_scope() throws Exception {
		final List<RequestScopedThing> requestScopedThings = new ArrayList<>();
		Runnable jobBody = new Runnable() {
			@Override
			public void run() {
				try {
					requestScopedThings.add(injector.getInstance(RequestScopedThing.class));
				} catch (ProvisionException e) {
					if (e.getCause() instanceof OutOfScopeException) {
						Assert.fail("Job body not in request scope");
					}
					throw e;
				}
			}
		};
		Random random = new Random();
		ScheduledJobController.JobDefinition job = new ScheduledJobController.JobDefinition(jobBody, random.nextLong(), random.nextLong());
		ScheduledJobController controller = new ScheduledJobController(requestStateProvider, executorProvider, ImmutableSet.of(job));
		captureSubmittedJobs();
		controller.start();
		scheduledJobBodies.get(0).run();
		MatcherAssert.assertThat(requestScopedThings, Matchers.contains(Matchers.instanceOf(RequestScopedThing.class)));
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
		ScheduledJobController.JobDefinition job = new ScheduledJobController.JobDefinition(jobBody, random.nextLong(), random.nextLong());
		ScheduledJobController controller = new ScheduledJobController(requestStateProvider, executorProvider, ImmutableSet.of(job));
		captureSubmittedJobs();
		controller.start();
		scheduledJobBodies.get(0).run();
		Mockito.verify(requestActivity).beginRequest(Mockito.eq("SCH"),
				Mockito.argThat(Matchers.stringContainsInOrder(ImmutableList.of(jobString))));
		Mockito.verify(requestActivity).finishRequest("SCH");
		Mockito.verifyNoMoreInteractions(requestActivity);
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

	public static final class RequestScopedThing {
	}

	private static String randomString() {
		Random random = new Random();
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		int len = 10;
		StringBuilder builder = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
		}
		return builder.toString();
	}
}

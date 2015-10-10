package org.araqnid.stuff;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

public class CompletableFutureThings {
	private static final Logger LOG = LoggerFactory.getLogger(CompletableFutureThings.class);

	@Rule
	public final ScheduledExecutorProvider scheduledExecutor = new ScheduledExecutorProvider();

	@Rule
	public final ExecutorProvider workExecutor = new ExecutorProvider();

	@Test
	public void run_supplier_asynchronously_on_default_pool() throws Exception {
		assertThat(CompletableFuture.supplyAsync(() -> {
			sleepUnchecked();
			return "I ran on " + thisThread();
		}).get(), matches("I ran on ForkJoinPool.commonPool-worker-\\d+"));
	}

	@Test
	public void run_supplier_asynchronously_on_specific_pool() throws Exception {
		assertThat(CompletableFuture.supplyAsync(() -> {
			sleepUnchecked();
			return "I ran on " + thisThread();
		}, workExecutor.get()).get(), matches("I ran on work-\\d+"));
	}

	@Test
	public void run_supplier_synchronously_by_overriding_executor() throws Exception {
		assertThat(CompletableFuture.supplyAsync(() -> {
			sleepUnchecked();
			return "I ran on " + thisThread();
		}, directExecutor()).get(), equalTo("I ran on " + thisThread()));
	}

	@Test
	public void trigger_completion_from_schedule() throws Exception {
		CompletableFuture<String> completable = new CompletableFuture<>();
		scheduledExecutor.get().schedule(() -> completable.complete("I ran on " + thisThread()), 100,
				TimeUnit.MILLISECONDS);
		assertThat(completable.get(), not(equalTo("I ran on " + thisThread())));
	}

	@Test
	public void trigger_completion_from_schedule_using_wrapper() throws Exception {
		assertThat(scheduler().schedule(() -> "I ran on " + thisThread(), 100, TimeUnit.MILLISECONDS).get(),
				not(equalTo("I ran on " + thisThread())));
	}

	@Test
	public void mangle_return_from_async_task_using_same_thread() throws Exception {
		assertThat(
				scheduler().schedule(() -> "I ran on " + thisThread(), 100, TimeUnit.MILLISECONDS)
						.thenApply(s -> s + " and applied a function on " + thisThread()).get(),
				matches("I ran on sch-\\d+ and applied a function on sch-\\d+"));
	}

	@Test
	public void mangle_return_from_async_task_on_specific_pool() throws Exception {
		assertThat(
				scheduler().schedule(() -> "I ran on " + thisThread(), 100, TimeUnit.MILLISECONDS)
						.thenApplyAsync(s -> {
							sleepUnchecked();
							return s + " and applied a function on " + thisThread();
						}, workExecutor.get()).get(), matches("I ran on sch-\\d+ and applied a function on work-\\d+"));
	}

	@Test
	public void consume_result_of_async_task() throws Exception {
		BlockingQueue<String> queue = new ArrayBlockingQueue<>(1);
		scheduler().schedule(() -> "I ran on " + thisThread(), 100, TimeUnit.MILLISECONDS).thenAccept(
				str -> queue.add(str + " and was consumed on " + thisThread()));
		assertThat(queue.poll(1, TimeUnit.SECONDS), matches("I ran on sch-\\d+ and was consumed on sch-\\d+"));
	}

	@Test
	public void can_wait_on_consume_result() throws Exception {
		AtomicReference<String> box = new AtomicReference<>();
		CompletableFuture<Void> future = scheduler().schedule(() -> "I ran on " + thisThread(), 100,
				TimeUnit.MILLISECONDS).thenAccept(str -> box.set(str + " and was consumed on " + thisThread()));
		assertThat(box.get(), nullValue());
		future.get();
		assertThat(box.get(), matches("I ran on sch-\\d+ and was consumed on sch-\\d+"));
	}

	@Test
	public void consume_result_of_async_task_on_specific_pool() throws Exception {
		BlockingQueue<String> queue = new ArrayBlockingQueue<>(1);
		scheduler().schedule(() -> "I ran on " + thisThread(), 100, TimeUnit.MILLISECONDS).thenAcceptAsync(
				str -> queue.add(str + " and was consumed on " + thisThread()), workExecutor.get());
		assertThat(queue.poll(1, TimeUnit.SECONDS), matches("I ran on sch-\\d+ and was consumed on work-\\d+"));
	}

	@Test
	public void process_result_or_exception_of_async_task() throws Exception {
		BlockingQueue<String> listenerQueue = new ArrayBlockingQueue<>(1);
		BlockingQueue<String> consumerQueue = new ArrayBlockingQueue<>(1);
		scheduler().schedule(() -> "I ran on " + thisThread(), 100, TimeUnit.MILLISECONDS).whenComplete((str, ex) -> {
			listenerQueue.add(str + ", received on " + thisThread());
		}).thenAccept(consumerQueue::add);
		assertThat(listenerQueue.poll(1, TimeUnit.SECONDS), matches("I ran on sch-\\d+, received on sch-\\d+"));
		assertThat(consumerQueue.poll(1, TimeUnit.SECONDS), matches("I ran on sch-\\d+"));
	}

	@Test
	public void process_result_or_exception_of_async_task_on_specific_pool() throws Exception {
		BlockingQueue<String> listenerQueue = new ArrayBlockingQueue<>(1);
		BlockingQueue<String> consumerQueue = new ArrayBlockingQueue<>(1);
		scheduler().schedule(() -> "I ran on " + thisThread(), 100, TimeUnit.MILLISECONDS)
				.whenCompleteAsync((str, ex) -> {
					listenerQueue.add(str + ", received on " + thisThread());
				}, workExecutor.get()).thenAccept(consumerQueue::add);
		assertThat(listenerQueue.poll(1, TimeUnit.SECONDS), matches("I ran on sch-\\d+, received on work-\\d+"));
		assertThat(consumerQueue.poll(1, TimeUnit.SECONDS), matches("I ran on sch-\\d+"));
	}

	@Test
	public void process_exception_or_result_of_async_task() throws Exception {
		BlockingQueue<String> listenerQueue = new ArrayBlockingQueue<>(1);
		BlockingQueue<String> consumerQueue = new ArrayBlockingQueue<>(1);
		scheduler().schedule((Supplier<String>) () -> {
			throw new IllegalStateException("I failed on " + thisThread());
		}, 100, TimeUnit.MILLISECONDS).whenComplete((str, ex) -> {
			listenerQueue.add(ex.getMessage() + ", received on " + thisThread());
		}).thenAccept(consumerQueue::add);
		assertThat(listenerQueue.poll(1, TimeUnit.SECONDS), matches("I failed on sch-\\d+, received on sch-\\d+"));
		assertThat(consumerQueue.poll(1, TimeUnit.SECONDS), nullValue());
	}

	@Test
	public void process_exception_or_result_of_async_task_on_specific_pool() throws Exception {
		BlockingQueue<String> listenerQueue = new ArrayBlockingQueue<>(1);
		BlockingQueue<String> consumerQueue = new ArrayBlockingQueue<>(1);
		scheduler().schedule((Supplier<String>) () -> {
			throw new IllegalStateException("I failed on " + thisThread());
		}, 100, TimeUnit.MILLISECONDS).whenCompleteAsync((str, ex) -> {
			listenerQueue.add(ex.getMessage() + ", received on " + thisThread());
		}, workExecutor.get()).thenAccept(consumerQueue::add);
		assertThat(listenerQueue.poll(1, TimeUnit.SECONDS), matches("I failed on sch-\\d+, received on work-\\d+"));
		assertThat(consumerQueue.poll(1, TimeUnit.SECONDS), nullValue());
	}

	@Test
	public void convert_exception_into_result() throws Exception {
		BlockingQueue<String> consumerQueue = new ArrayBlockingQueue<>(1);
		scheduler().schedule((Supplier<String>) () -> {
			throw new IllegalStateException("I failed on " + thisThread());
		}, 100, TimeUnit.MILLISECONDS).exceptionally(ex -> ex.getMessage() + ", handled on " + thisThread())
				.thenAccept(consumerQueue::add);
		assertThat(consumerQueue.poll(1, TimeUnit.SECONDS), matches("I failed on sch-\\d+, handled on sch-\\d+"));
	}

	@Test
	public void runs_something_after_completion() throws Exception {
		BlockingQueue<String> consumerQueue = new ArrayBlockingQueue<>(1);
		scheduler().schedule(() -> "I ran on " + thisThread(), 100, TimeUnit.MILLISECONDS).thenRun(
				() -> consumerQueue.add("Finished on " + thisThread()));
		assertThat(consumerQueue.poll(1, TimeUnit.SECONDS), matches("Finished on sch-\\d+"));
	}

	@Test
	public void runs_something_after_completion_on_specific_pool() throws Exception {
		BlockingQueue<String> consumerQueue = new ArrayBlockingQueue<>(1);
		scheduler().schedule(() -> "I ran on " + thisThread(), 100, TimeUnit.MILLISECONDS).thenRunAsync(
				() -> consumerQueue.add("Finished on " + thisThread()), workExecutor.get());
		assertThat(consumerQueue.poll(1, TimeUnit.SECONDS), matches("Finished on work-\\d+"));
	}

	@Test
	public void composes_two_executions() throws Exception {
		assertThat(
				scheduler()
						.schedule(() -> "A ran on " + thisThread(), 100, TimeUnit.MILLISECONDS)
						.thenCompose(
								str -> {
									String prefix = str + "; composed on " + thisThread();
									return scheduler().schedule(() -> prefix + "; B ran on " + thisThread(), 100,
											TimeUnit.MILLISECONDS);
								}).get(), matches("A ran on sch-\\d+; composed on sch-\\d+; B ran on sch-\\d+"));
	}

	@Test
	public void composes_two_executions_with_composition_on_specific_pool() throws Exception {
		assertThat(
				scheduler()
						.schedule(() -> "A ran on " + thisThread(), 100, TimeUnit.MILLISECONDS)
						.thenComposeAsync(
								str -> {
									String prefix = str + "; composed on " + thisThread();
									return scheduler().schedule(() -> prefix + "; B ran on " + thisThread(), 100,
											TimeUnit.MILLISECONDS);
								}, workExecutor.get()).get(),
				matches("A ran on sch-\\d+; composed on work-\\d+; B ran on sch-\\d+"));
	}

	@Test
	public void combines_two_executions() throws Exception {
		CompletableFuture<String> resultA = scheduler().schedule(() -> "A ran on " + thisThread(), 100,
				TimeUnit.MILLISECONDS);
		CompletableFuture<String> resultB = scheduler().schedule(() -> "B ran on " + thisThread(), 100,
				TimeUnit.MILLISECONDS);
		assertThat(
				resultA.thenCombine(resultB,
						(a, b) -> ImmutableMap.of("A", a, "B", b, "X", "combined on " + thisThread())).get(),
				allOf(hasEntry(equalTo("A"), matches("A ran on sch-\\d+")),
						hasEntry(equalTo("B"), matches("B ran on sch-\\d+")),
						hasEntry(equalTo("X"), matches("combined on sch-\\d+"))));
	}

	@Test
	public void combines_two_executions_on_specific_pool() throws Exception {
		CompletableFuture<String> resultA = scheduler().schedule(() -> "A ran on " + thisThread(), 100,
				TimeUnit.MILLISECONDS);
		CompletableFuture<String> resultB = scheduler().schedule(() -> "B ran on " + thisThread(), 100,
				TimeUnit.MILLISECONDS);
		assertThat(
				resultA.thenCombineAsync(resultB,
						(a, b) -> ImmutableMap.of("A", a, "B", b, "X", "combined on " + thisThread()),
						workExecutor.get()).get(),
				allOf(hasEntry(equalTo("A"), matches("A ran on sch-\\d+")),
						hasEntry(equalTo("B"), matches("B ran on sch-\\d+")),
						hasEntry(equalTo("X"), matches("combined on work-\\d+"))));
	}

	@Test
	public void consumes_two_executions() throws Exception {
		BlockingQueue<Map<String, String>> consumerQueue = new ArrayBlockingQueue<>(1);
		CompletableFuture<String> resultA = scheduler().schedule(() -> "A ran on " + thisThread(), 100,
				TimeUnit.MILLISECONDS);
		CompletableFuture<String> resultB = scheduler().schedule(() -> "B ran on " + thisThread(), 100,
				TimeUnit.MILLISECONDS);
		resultA.thenAcceptBoth(resultB,
				(a, b) -> consumerQueue.add(ImmutableMap.of("A", a, "B", b, "X", "consumed on " + thisThread())));
		assertThat(
				consumerQueue.poll(1, TimeUnit.SECONDS),
				Matchers.allOf(hasEntry(equalTo("A"), matches("A ran on sch-\\d+")),
						hasEntry(equalTo("B"), matches("B ran on sch-\\d+")),
						hasEntry(equalTo("X"), matches("consumed on sch-\\d+"))));
	}

	@Test
	public void consumes_two_executions_on_specific_pool() throws Exception {
		BlockingQueue<Map<String, String>> consumerQueue = new ArrayBlockingQueue<>(1);
		CompletableFuture<String> resultA = scheduler().schedule(() -> "A ran on " + thisThread(), 100,
				TimeUnit.MILLISECONDS);
		CompletableFuture<String> resultB = scheduler().schedule(() -> "B ran on " + thisThread(), 100,
				TimeUnit.MILLISECONDS);
		resultA.thenAcceptBothAsync(resultB,
				(a, b) -> consumerQueue.add(ImmutableMap.of("A", a, "B", b, "X", "consumed on " + thisThread())),
				workExecutor.get());
		assertThat(
				consumerQueue.poll(1, TimeUnit.SECONDS),
				allOf(hasEntry(equalTo("A"), matches("A ran on sch-\\d+")),
						hasEntry(equalTo("B"), matches("B ran on sch-\\d+")),
						hasEntry(equalTo("X"), matches("consumed on work-\\d+"))));
	}

	public static final class Scheduler {
		private final ScheduledExecutorService service;

		public Scheduler(ScheduledExecutorService service) {
			this.service = service;
		}

		public <T> CompletableFuture<T> schedule(Supplier<T> supplier, long delay, TimeUnit unit) {
			CompletableFuture<T> completable = new CompletableFuture<>();
			service.schedule(() -> {
				try {
					completable.complete(supplier.get());
				} catch (Throwable t) {
					completable.completeExceptionally(t);
				}
			}, delay, unit);
			return completable;
		}
	}

	private void sleepUnchecked() {
		LOG.info("Start sleep");
		try {
			Thread.sleep(100L);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
		LOG.info("Stop sleep");
	}

	private Scheduler scheduler() {
		return new Scheduler(scheduledExecutor.get());
	}

	private static String thisThread() {
		return Thread.currentThread().getName();
	}

	private static final class ExecutorProvider extends FixtureProvider<ExecutorService> {
		private static final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("work-%d").build();

		@Override
		protected ExecutorService create() {
			return new ExecutorService() {
				private final ExecutorService underlying = Executors.newCachedThreadPool(threadFactory);

				@Override
				public void execute(Runnable command) {
					underlying.execute(wrap(command));
				}

				@Override
				public void shutdown() {
					underlying.shutdown();
				}

				@Override
				public List<Runnable> shutdownNow() {
					return underlying.shutdownNow();
				}

				@Override
				public boolean isShutdown() {
					return underlying.isShutdown();
				}

				@Override
				public boolean isTerminated() {
					return underlying.isTerminated();
				}

				@Override
				public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
					return underlying.awaitTermination(timeout, unit);
				}

				@Override
				public <T> Future<T> submit(Callable<T> task) {
					return underlying.submit(wrap(task));
				}

				@Override
				public <T> Future<T> submit(Runnable task, T result) {
					return underlying.submit(wrap(task), result);
				}

				@Override
				public Future<?> submit(Runnable task) {
					return underlying.submit(wrap(task));
				}

				@Override
				public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
						throws InterruptedException {
					return underlying.invokeAll(transform(tasks, this::wrap));
				}

				@Override
				public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
						long timeout,
						TimeUnit unit) throws InterruptedException {
					return underlying.invokeAll(transform(tasks, this::wrap), timeout, unit);
				}

				@Override
				public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException,
						ExecutionException {
					return underlying.invokeAny(transform(tasks, this::wrap));
				}

				@Override
				public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
						throws InterruptedException, ExecutionException, TimeoutException {
					return underlying.invokeAny(transform(tasks, this::wrap), timeout, unit);
				}

				private Runnable wrap(Runnable r) {
					return () -> {
						LOG.info("Start run");
						try {
							r.run();
						} finally {
							LOG.info("Stop run");
						}
					};
				}

				private <T> Callable<T> wrap(Callable<T> c) {
					return () -> {
						LOG.info("Start call");
						try {
							return c.call();
						} finally {
							LOG.info("Stop call");
						}
					};
				}
			};
		}

		@Override
		protected void cleanup(ExecutorService thing) {
			MoreExecutors.shutdownAndAwaitTermination(thing, 1, TimeUnit.SECONDS);
		}
	}

	private static final class ScheduledExecutorProvider extends FixtureProvider<ScheduledExecutorService> {
		private static final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("sch-%d").build();

		@Override
		protected ScheduledExecutorService create() {
			return new ScheduledExecutorService() {
				private final ScheduledExecutorService underlying = Executors.newScheduledThreadPool(4, threadFactory);

				@Override
				public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
					return underlying.schedule(wrap(command), delay, unit);
				}

				@Override
				public void execute(Runnable command) {
					underlying.execute(wrap(command));
				}

				@Override
				public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
					return underlying.schedule(wrap(callable), delay, unit);
				}

				@Override
				public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
						long initialDelay,
						long period,
						TimeUnit unit) {
					return underlying.scheduleAtFixedRate(wrap(command), initialDelay, period, unit);
				}

				@Override
				public void shutdown() {
					underlying.shutdown();
				}

				@Override
				public List<Runnable> shutdownNow() {
					return underlying.shutdownNow();
				}

				@Override
				public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
						long initialDelay,
						long delay,
						TimeUnit unit) {
					return underlying.scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit);
				}

				@Override
				public boolean isShutdown() {
					return underlying.isShutdown();
				}

				@Override
				public boolean isTerminated() {
					return underlying.isTerminated();
				}

				@Override
				public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
					return underlying.awaitTermination(timeout, unit);
				}

				@Override
				public <T> Future<T> submit(Callable<T> task) {
					return underlying.submit(wrap(task));
				}

				@Override
				public <T> Future<T> submit(Runnable task, T result) {
					return underlying.submit(wrap(task), result);
				}

				@Override
				public Future<?> submit(Runnable task) {
					return underlying.submit(wrap(task));
				}

				@Override
				public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
						throws InterruptedException {
					return underlying.invokeAll(transform(tasks, this::wrap));
				}

				@Override
				public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
						long timeout,
						TimeUnit unit) throws InterruptedException {
					return underlying.invokeAll(transform(tasks, this::wrap), timeout, unit);
				}

				@Override
				public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException,
						ExecutionException {
					return underlying.invokeAny(transform(tasks, this::wrap));
				}

				@Override
				public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
						throws InterruptedException, ExecutionException, TimeoutException {
					return underlying.invokeAny(transform(tasks, this::wrap), timeout, unit);
				}

				private Runnable wrap(Runnable r) {
					return () -> {
						LOG.info("Start run");
						try {
							r.run();
						} finally {
							LOG.info("Stop run");
						}
					};
				}

				private <T> Callable<T> wrap(Callable<T> c) {
					return () -> {
						LOG.info("Start call");
						try {
							return c.call();
						} finally {
							LOG.info("Stop call");
						}
					};
				}
			};
		}

		@Override
		protected void cleanup(ScheduledExecutorService thing) {
			MoreExecutors.shutdownAndAwaitTermination(thing, 1, TimeUnit.SECONDS);
		}
	}

	private static abstract class FixtureProvider<T> implements TestRule, Supplier<T> {
		private T thing;

		@Override
		public final Statement apply(Statement base, Description description) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					thing = create();
					try {
						base.evaluate();
					} finally {
						cleanup(thing);
					}
				}
			};
		}

		@Override
		public final T get() {
			if (thing == null) throw new IllegalStateException("not available");
			return thing;
		}

		protected abstract T create();

		protected abstract void cleanup(T thing);
	}

	private static Matcher<CharSequence> matches(String regex) {
		Pattern pattern = Pattern.compile(regex);
		return new TypeSafeDiagnosingMatcher<CharSequence>() {
			@Override
			protected boolean matchesSafely(CharSequence item, org.hamcrest.Description mismatchDescription) {
				java.util.regex.Matcher matcher = pattern.matcher(item);
				if (!matcher.matches()) {
					mismatchDescription.appendText("input was ").appendValue(item);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(org.hamcrest.Description description) {
				description.appendText("matching ").appendValue(pattern.pattern());
			}
		};
	}

}

package org.araqnid.stuff.refdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Functions;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SynchronisingReferenceDataFinderTest {
	@SuppressWarnings("unchecked")
	@Test
	public void passes_through_to_underlying_fetcher() {
		Executor executor = MoreExecutors.sameThreadExecutor();
		ReferenceDataFinder<String, String> underlying = mock(ReferenceDataFinder.class);
		ReferenceDataFinder<String, String> syncer = new SynchronisingReferenceDataFinder<>(underlying, executor);

		when(underlying.get(anySet())).thenReturn(ImmutableMap.<String, String> of("A", "apple"));
		assertThat(syncer.get(ImmutableSet.of("A")), hasEntry("A", "apple"));

		verify(underlying).get(ImmutableSet.of("A"));
		verifyNoMoreInteractions(underlying);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void future_passes_through_to_underlying_fetcher() throws Exception {
		final List<Runnable> commands = new ArrayList<>();
		Executor executor = new Executor() {
			@Override
			public void execute(Runnable command) {
				commands.add(command);
			}
		};
		ReferenceDataFinder<String, String> underlying = mock(ReferenceDataFinder.class);
		AsyncReferenceDataFinder<String, String> syncer = new SynchronisingReferenceDataFinder<>(underlying, executor);

		when(underlying.get(anySet())).thenReturn(ImmutableMap.<String, String> of("A", "apple"));

		ListenableFuture<Map<String, String>> future = syncer.future(ImmutableSet.of("A"));
		assertThat(commands, hasSize(1));
		commands.get(0).run();
		assertThat(future.get(), hasEntry("A", "apple"));

		verify(underlying).get(ImmutableSet.of("A"));
		verifyNoMoreInteractions(underlying);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void unknown_items_are_dropped() {
		Executor executor = MoreExecutors.sameThreadExecutor();
		ReferenceDataFinder<String, String> underlying = mock(ReferenceDataFinder.class);
		ReferenceDataFinder<String, String> syncer = new SynchronisingReferenceDataFinder<>(underlying, executor);

		when(underlying.get(anySet())).thenReturn(ImmutableMap.<String, String> of());
		assertThat(syncer.get(ImmutableSet.of("A")).keySet(), Matchers.hasSize(0));

		verify(underlying).get(ImmutableSet.of("A"));
		verifyNoMoreInteractions(underlying);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void unexpected_items_are_ignored() {
		Executor executor = MoreExecutors.sameThreadExecutor();
		ReferenceDataFinder<String, String> underlying = mock(ReferenceDataFinder.class);
		ReferenceDataFinder<String, String> syncer = new SynchronisingReferenceDataFinder<>(underlying, executor);

		when(underlying.get(anySet())).thenReturn(ImmutableMap.<String, String> of("B", "bear"));
		assertThat(syncer.get(ImmutableSet.of("A")).keySet(), Matchers.hasSize(0));

		verify(underlying).get(ImmutableSet.of("A"));
		verifyNoMoreInteractions(underlying);
	}

	@SuppressWarnings("unchecked")
	@Test
	@Ignore("flickering")
	public void requests_for_the_same_keys_join() throws Exception {
		final ReferenceDataFinder<String, String> underlying = mock(ReferenceDataFinder.class);
		when(underlying.get(anySet())).thenReturn(ImmutableMap.<String, String> of("A", "apple"));

		final CountDownLatch entryLatch = new CountDownLatch(1);
		final CountDownLatch exitLatch = new CountDownLatch(1);
		ReferenceDataFinder<String, String> latcher = new ReferenceDataFinder<String, String>() {
			@Override
			public Map<String, String> get(Set<String> keys) {
				entryLatch.countDown();
				try {
					exitLatch.await();
				} catch (InterruptedException e) {
					throw new IllegalStateException();
				}
				return underlying.get(keys);
			}
		};
		final ReferenceDataFinder<String, String> syncer = new SynchronisingReferenceDataFinder<>(latcher,
				MoreExecutors.sameThreadExecutor());
		final ListeningExecutorService localExecutor = MoreExecutors
				.listeningDecorator(Executors.newCachedThreadPool());
		ListenableFuture<List<Map<String, String>>> futures = Futures.allAsList(Iterables.transform(
				ImmutableList.of(1, 2, 3, 4),
				Functions.forSupplier(new Supplier<ListenableFuture<Map<String, String>>>() {
					@Override
					public ListenableFuture<Map<String, String>> get() {
						return localExecutor.submit(new Callable<Map<String, String>>() {
							@Override
							public Map<String, String> call() throws Exception {
								return syncer.get(ImmutableSet.of("A"));
							}
						});
					}
				})));

		entryLatch.await();
		Thread.sleep(500L);
		exitLatch.countDown();

		Assert.assertEquals(
				ImmutableList.of(ImmutableMap.of("A", "apple"), ImmutableMap.of("A", "apple"),
						ImmutableMap.of("A", "apple"), ImmutableMap.of("A", "apple")), Futures.getUnchecked(futures));
		verify(underlying).get(ImmutableSet.of("A"));
		verifyNoMoreInteractions(underlying);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void underlying_exceptions_are_relayed_through_future() throws Exception {
		final List<Runnable> commands = new ArrayList<>();
		Executor executor = new Executor() {
			@Override
			public void execute(Runnable command) {
				commands.add(command);
			}
		};
		ReferenceDataFinder<String, String> underlying = mock(ReferenceDataFinder.class);
		AsyncReferenceDataFinder<String, String> syncer = new SynchronisingReferenceDataFinder<>(underlying, executor);

		UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException();
		when(underlying.get(anySet())).thenThrow(unsupportedOperationException);

		ListenableFuture<Map<String, String>> future = syncer.future(ImmutableSet.of("A"));
		assertThat(commands, hasSize(1));
		commands.get(0).run();
		try {
			future.get();
			Assert.fail();
		} catch (ExecutionException e) {
			assertThat(e, hasProperty("cause", sameInstance(unsupportedOperationException)));
		}
	}
}

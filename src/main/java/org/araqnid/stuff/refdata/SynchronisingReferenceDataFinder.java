package org.araqnid.stuff.refdata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

public class SynchronisingReferenceDataFinder<K, V> implements AsyncReferenceDataFinder<K, V> {
	private final ReferenceDataFinder<K, V> underlying;
	private final Map<K, Request> ongoing = new HashMap<>();
	private final Executor executor;

	public SynchronisingReferenceDataFinder(ReferenceDataFinder<K, V> underlying, Executor executor) {
		this.underlying = underlying;
		this.executor = executor;
	}

	@Override
	public Map<K, V> get(Set<K> keys) {
		RequestLookupResult lookup = createRequest(keys);
		try {
			ImmutableMap.Builder<K, V> mapBuilder = ImmutableMap.builder();
			if (lookup.newRequest.isPresent()) {
				Request request = lookup.newRequest.get();
				Map<K, V> newValues = Futures.getUnchecked(request.future);
				for (K key : keys) {
					V value = newValues.get(key);
					if (value != null) mapBuilder.put(key, value);
				}
			}
			for (Map.Entry<Request, Collection<K>> e : lookup.joinRequests.asMap().entrySet()) {
				Map<K, V> requestValues = Futures.getUnchecked(e.getKey().future);
				for (K key : e.getValue()) {
					V value = requestValues.get(key);
					if (value != null) mapBuilder.put(key, value);
				}
			}
			return mapBuilder.build();
		} finally {
			lookup.release();
		}
	}

	@Override
	public ListenableFuture<Map<K, V>> future(Set<K> keys) {
		final RequestLookupResult lookup = createRequest(keys);
		List<Set<K>> keyLists = new ArrayList<>(lookup.joinRequests.size());
		List<ListenableFuture<Map<K, V>>> futures = new ArrayList<>(lookup.joinRequests.size());
		if (lookup.newRequest.isPresent()) {
			Request newRequest = lookup.newRequest.get();
			keyLists.add(newRequest.keys);
			futures.add(newRequest.future);
		}
		for (Map.Entry<Request, Collection<K>> e : lookup.joinRequests.asMap().entrySet()) {
			keyLists.add(ImmutableSet.copyOf(e.getValue()));
			futures.add(e.getKey().future);
		}
		final List<Set<K>> keyLists0 = ImmutableList.copyOf(keyLists);
		return Futures.transform(Futures.allAsList(futures), new Function<List<Map<K, V>>, Map<K, V>>() {
			@Override
			public Map<K, V> apply(List<Map<K, V>> input) {
				ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
				for (int i = 0; i < keyLists0.size(); i++) {
					Set<K> keys = keyLists0.get(i);
					Map<K, V> values = input.get(i);
					for (K key : keys) {
						V value = values.get(key);
						if (value != null) builder.put(key, value);
					}
				}
				lookup.release();
				return builder.build();
			}
		});
	}

	private synchronized RequestLookupResult createRequest(Set<K> keys) {
		Multimap<Request, K> joinRequests = HashMultimap.create();
		Set<K> newKeys = new HashSet<>();
		for (K key : keys) {
			Request req = ongoing.get(key);
			if (req != null) {
				joinRequests.put(req, key);
			}
			else {
				newKeys.add(key);
			}
		}
		if (newKeys.isEmpty()) return new RequestLookupResult(joinRequests);

		Request request = new Request(newKeys, futureFor(newKeys));
		for (K key : newKeys) {
			ongoing.put(key, request);
		}

		return new RequestLookupResult(request, joinRequests);
	}

	private ListenableFuture<Map<K, V>> futureFor(Set<K> newKeys) {
		if (underlying instanceof AsyncReferenceDataFinder)
			return ((AsyncReferenceDataFinder<K, V>) underlying).future(newKeys);

		ListenableFutureTask<Map<K, V>> futureTask = ListenableFutureTask.create(new Invocation(newKeys));
		executor.execute(futureTask);
		return futureTask;
	}

	private synchronized void release(Request request) {
		for (K key : request.keys) {
			ongoing.remove(key);
		}
	}

	private final class Invocation implements Callable<Map<K, V>> {
		private final Set<K> keys;

		public Invocation(Set<K> keys) {
			this.keys = ImmutableSet.copyOf(keys);
		}

		@Override
		public Map<K, V> call() throws Exception {
			return underlying.get(keys);
		}
	}

	private class RequestLookupResult {
		private final Optional<Request> newRequest;
		private final Multimap<Request, K> joinRequests;

		public RequestLookupResult(Request newRequest, Multimap<Request, K> joinRequests) {
			this.newRequest = Optional.of(newRequest);
			this.joinRequests = ImmutableMultimap.copyOf(joinRequests);
		}

		public RequestLookupResult(Multimap<Request, K> joinRequests) {
			this.newRequest = Optional.absent();
			this.joinRequests = ImmutableMultimap.copyOf(joinRequests);
		}

		public void release() {
			if (newRequest.isPresent()) SynchronisingReferenceDataFinder.this.release(newRequest.get());
		}
	}

	private class Request {
		private final Set<K> keys;
		private final ListenableFuture<Map<K, V>> future;

		public Request(Set<K> keys, ListenableFuture<Map<K, V>> future) {
			this.keys = ImmutableSet.copyOf(keys);
			this.future = future;
		}
	}
}

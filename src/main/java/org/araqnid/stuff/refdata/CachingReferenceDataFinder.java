package org.araqnid.stuff.refdata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

public class CachingReferenceDataFinder<K, V> implements AsyncReferenceDataFinder<K, V> {
	private final ReferenceDataFinder<K, V> underlying;
	private final Cache<K, V> cache;
	private final Executor executor;

	public CachingReferenceDataFinder(ReferenceDataFinder<K, V> underlying, Cache<K, V> cache, Executor executor) {
		this.underlying = underlying;
		this.cache = cache;
		this.executor = executor;
	}

	public interface Cache<K, V> {
		V getValue(K key);

		void putValue(K key, V value);
	}

	@Override
	public Map<K, V> get(Set<K> keys) {
		Set<K> uncached = new HashSet<>();
		Map<K, V> result = new HashMap<>();
		for (K key : keys) {
			V value = cache.getValue(key);
			if (value == null)
				uncached.add(key);
			else result.put(key, value);
		}
		if (!uncached.isEmpty()) {
			Map<K, V> loaded = underlying.get(uncached);
			for (K key : uncached) {
				V value = loaded.get(key);
				if (value != null) {
					result.put(key, value);
					cache.putValue(key, value);
				}
			}
		}
		return result;
	}

	@Override
	public ListenableFuture<Map<K, V>> future(Set<K> keys) {
		final Set<K> uncached = new HashSet<>();
		ImmutableMap.Builder<K, V> foundInCache = ImmutableMap.builder();
		for (K key : keys) {
			V value = cache.getValue(key);
			if (value == null)
				uncached.add(key);
			else foundInCache.put(key, value);
		}
		final ImmutableMap<K, V> fromCache = foundInCache.build();
		if (uncached.isEmpty()) return Futures.<Map<K, V>> immediateFuture(fromCache);
		ListenableFuture<Map<K, V>> future = futureFor(uncached);
		return Futures.transform(future, new Function<Map<K, V>, Map<K, V>>() {
			@Override
			public Map<K, V> apply(Map<K, V> produced) {
				Map<K, V> result = new HashMap<>();
				result.putAll(fromCache);
				for (K key : uncached) {
					V value = produced.get(key);
					if (value != null) {
						result.put(key, value);
						cache.putValue(key, value);
					}
				}
				return result;
			}
		});

	}

	private ListenableFuture<Map<K, V>> futureFor(final Set<K> keys) {
		if (underlying instanceof AsyncReferenceDataFinder)
			return ((AsyncReferenceDataFinder<K, V>) underlying).future(keys);
		ListenableFutureTask<Map<K, V>> task = ListenableFutureTask.create(new Callable<Map<K, V>>() {
			@Override
			public Map<K, V> call() throws Exception {
				return underlying.get(keys);
			}
		});
		executor.execute(task);
		return task;
	}
}

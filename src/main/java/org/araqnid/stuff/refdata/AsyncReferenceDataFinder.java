package org.araqnid.stuff.refdata;

import java.util.Map;
import java.util.Set;

import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncReferenceDataFinder<K, V> extends ReferenceDataFinder<K, V> {
	ListenableFuture<Map<K, V>> future(Set<K> keys);
}

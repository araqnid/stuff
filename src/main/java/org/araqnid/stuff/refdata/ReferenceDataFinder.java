package org.araqnid.stuff.refdata;

import java.util.Map;
import java.util.Set;

public interface ReferenceDataFinder<K, V> {
	Map<K, V> get(Set<K> keys);
}

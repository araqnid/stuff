package org.araqnid.stuff;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class GreetingRepository {
	private static final Logger LOG = LoggerFactory.getLogger(GreetingRepository.class);
	private final Map<String, String> data = new HashMap<>();

	public synchronized String find(String name) {
		LOG.info("find({})", name);
		return data.get(name);
	}

	public synchronized void save(String name, String greeting) {
		LOG.info("save({},{})", name, greeting);
		data.put(name, greeting);
	}

	public synchronized boolean delete(String name) {
		LOG.info("delete({})", name);
		return data.remove(name) != null;
	}
}

package org.araqnid.stuff.messages;

import redis.clients.jedis.Jedis;

import com.google.inject.Provider;

public class RedisEventSpooler {
	private final Provider<Jedis> redisProvider;
	private final String listName;

	public RedisEventSpooler(Provider<Jedis> redisProvider, String listName) {
		this.redisProvider = redisProvider;
		this.listName = listName;
	}

	public void spool(String payload) {
		try (Jedis redis = redisProvider.get()) {
			redis.rpush(listName, payload);
		}
	}
}

package org.araqnid.stuff;

import javax.inject.Provider;

import org.araqnid.stuff.activity.ActivityScopeControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.google.common.util.concurrent.AbstractExecutionThreadService;

import static org.araqnid.stuff.activity.AppRequestType.EventReplay;

public class RedisEventLoader extends AbstractExecutionThreadService {
	private static final Logger LOG = LoggerFactory.getLogger(RedisEventLoader.class);
	private final EventTarget eventTarget;
	private final Provider<Jedis> redisProvider;
	private final String key;
	private final ActivityScopeControl scopeControl;
	private final int pageSize;
	private Jedis redis;
	private volatile boolean shutdownRequested;

	public RedisEventLoader(EventTarget eventTarget,
			Provider<Jedis> redisProvider,
			String key,
			ActivityScopeControl scopeControl,
			int pageSize) {
		this.eventTarget = eventTarget;
		this.redisProvider = redisProvider;
		this.key = key;
		this.pageSize = pageSize;
		this.scopeControl = scopeControl;
	}

	@Override
	protected void startUp() throws Exception {
		redis = redisProvider.get();
	}

	@Override
	protected void run() throws Exception {
		scopeControl.beginRequest(EventReplay, key);
		try {
			long llen = redis.llen(key);
			LOG.info("Loading {} events from Redis list \"{}\"", llen, key);
			for (long i = 0; i < llen; i += pageSize) {
				for (String string : redis.lrange(key, i, i + pageSize - 1)) {
					if (shutdownRequested) return;
					eventTarget.processEvent(string);
				}
			}
		} finally {
			scopeControl.finishRequest(EventReplay);
		}
	}

	@Override
	protected void shutDown() throws Exception {
		if (redis != null) {
			redis.close();
		}
	}

	@Override
	protected void triggerShutdown() {
		shutdownRequested = true;
	}

	public interface EventTarget {
		void processEvent(String payload);
	}
}

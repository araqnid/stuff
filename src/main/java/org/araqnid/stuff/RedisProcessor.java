package org.araqnid.stuff;

import org.araqnid.stuff.activity.ActivityScopeControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import redis.clients.jedis.Jedis;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Provider;

import static org.araqnid.stuff.activity.AppRequestType.RedisMessage;

public class RedisProcessor extends AbstractExecutionThreadService {
	private final Provider<Jedis> connectionProvider;
	private final String key;
	private final String processingSuffix = ".working";
	private final Provider<? extends DeliveryTarget> targetProvider;
	private final Logger log;
	private final ActivityScopeControl scopeControl;

	public RedisProcessor(Provider<Jedis> connectionProvider,
			String key,
			ActivityScopeControl scopeControl,
			Provider<? extends DeliveryTarget> targetProvider) {
		this.connectionProvider = connectionProvider;
		this.key = key;
		this.targetProvider = targetProvider;
		this.scopeControl = scopeControl;
		this.log = LoggerFactory.getLogger(RedisProcessor.class.getName() + "." + key);
	}

	@Override
	protected void run() throws Exception {
		Jedis jedis = connectionProvider.get();
		String inProgressKey = key + processingSuffix;
		log.info("Consuming from Redis list \"{}\"", key);
		while (isRunning()) {
			log.debug("retrieving value from list");
			String value = jedis.brpoplpush(key, inProgressKey, 1);
			if (value != null) {
				log.debug("<{}> retrieved", value);
				if (deliver(value)) {
					jedis.lrem(inProgressKey, 0, value);
					log.debug("<{}> removed", value);
				}
				else {
					jedis.lrem(inProgressKey, 0, value);
					jedis.lpush(key, value);
					log.debug("<{}> recycled", value);
				}
			}
		}
		log.info("Consumption stopped");
	}

	private boolean deliver(String value) {
		PushedMdcValue queue = new PushedMdcValue("queue", key);
		PushedMdcValue jobId = new PushedMdcValue("jobId", value);
		try {
			return dispatchDelivery(value);
		} finally {
			PushedMdcValue.restoreValues(queue, jobId);
		}
	}

	private boolean dispatchDelivery(String value) {
		scopeControl.beginRequest(RedisMessage, Joiner.on('\t').join(key, value));
		try {
			return targetProvider.get().deliver(value);
		} finally {
			scopeControl.finishRequest(RedisMessage);
		}
	}

	@Override
	public String toString() {
		return "RedisProcessor:" + key + " => " + targetProvider;
	}

	public interface DeliveryTarget {
		boolean deliver(String data);
	}

	private static final class PushedMdcValue {
		private final String key;
		private final String oldValue;

		public PushedMdcValue(String key, String newValue) {
			this.key = key;
			oldValue = MDC.get(key);
			MDC.put(key, newValue);
		}

		public void restore() {
			if (oldValue != null) {
				MDC.put(key, oldValue);
			}
			else {
				MDC.remove(key);
			}
		}

		public static void restoreValues(PushedMdcValue... values) {
			for (PushedMdcValue value : values) {
				value.restore();
			}
		}
	}
}

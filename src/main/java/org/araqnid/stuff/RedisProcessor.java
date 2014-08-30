package org.araqnid.stuff;

import java.net.SocketException;

import org.araqnid.stuff.activity.ActivityScopeControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

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
	private Jedis jedis;

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
	protected void startUp() throws Exception {
		jedis = connectionProvider.get();
		jedis.connect();
	}

	@Override
	protected void run() throws Exception {
		String inProgressKey = key + processingSuffix;
		log.info("Consuming from Redis list \"{}\"", key);
		while (isRunning()) {
			log.debug("retrieving value from list");
			String value;
			try {
				value = jedis.brpoplpush(key, inProgressKey, 30);
			} catch (JedisConnectionException e) {
				if (!isRunning() && e.getCause() instanceof SocketException) {
					log.debug("Ignoring network exception during shutdown: " + e);
					return;
				}
				throw e;
			}
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
	}

	@Override
	protected void shutDown() throws Exception {
		log.info("Consumption stopped");
		if (jedis.isConnected()) jedis.disconnect();
	}

	@Override
	protected void triggerShutdown() {
		jedis.disconnect();
	}

	@Override
	protected String serviceName() {
		return "RedisProcessor-" + key;
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
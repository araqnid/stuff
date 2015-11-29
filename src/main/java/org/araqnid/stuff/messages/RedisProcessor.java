package org.araqnid.stuff.messages;

import java.io.IOException;
import java.net.SocketException;

import javax.inject.Provider;

import org.araqnid.stuff.zedis.Zedis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Monitor;

import redis.clients.jedis.exceptions.JedisConnectionException;

public class RedisProcessor extends AbstractExecutionThreadService {
	private final Provider<Zedis> connectionProvider;
	private final String key;
	private final String processingSuffix = ".working";
	private final DeliveryTarget target;
	private final Logger log;
	private final Monitor monitor = new Monitor();
	private final Monitor.Guard notCurrentlyDelivering = new Monitor.Guard(monitor) {
		@Override
		public boolean isSatisfied() {
			return !delivering;
		}
	};
	private Zedis jedis;
	private boolean delivering;

	public RedisProcessor(Provider<Zedis> connectionProvider, String key, DeliveryTarget target) {
		this.connectionProvider = connectionProvider;
		this.key = key;
		this.target = target;
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
				monitor.enter();
				try {
					if (!isRunning()) {
						log.error("<{}> aborting delivery due to shutdown", value);
						jedis.rpush(key, value);
						jedis.lrem(inProgressKey, 0, value);
						return;
					}
					log.debug("<{}> retrieved", value);
					delivering = true;
				} finally {
					monitor.leave();
				}
				try {
					if (deliver(value)) {
						jedis.lrem(inProgressKey, 0, value);
						log.debug("<{}> removed", value);
					}
					else {
						jedis.lrem(inProgressKey, 0, value);
						jedis.lpush(key, value);
						log.debug("<{}> recycled", value);
					}
				} catch (Exception e) {
					log.error("Unhandled exception delivering message, leaving it on in-progress key", e);
				}
				monitor.enter();
				try {
					delivering = false;
				} finally {
					monitor.leave();
				}
			}
		}
	}

	@Override
	protected void shutDown() throws Exception {
		log.info("Consumption stopped");
		jedis.close();
	}

	@Override
	protected void triggerShutdown() {
		if (monitor.enterIf(notCurrentlyDelivering)) {
			try {
				jedis.close();
			} catch (IOException e) {
				log.debug("Ignoring exception closing Zedis", e);
			}
		}
	}

	@Override
	protected String serviceName() {
		return "RedisProcessor-" + key;
	}

	private boolean deliver(String value) {
		try (MDCCloseable m = MDC.putCloseable("queue", key)) {
			return dispatchDelivery(value);
		}
	}

	private boolean dispatchDelivery(String value) {
		return target.deliver(value);
	}

	@Override
	public String toString() {
		return "RedisProcessor:" + key + " => " + target + " [" + state() + "]";
	}

	public interface DeliveryTarget {
		boolean deliver(String data);
	}
}

package org.araqnid.stuff.messages;

import java.io.EOFException;
import java.util.concurrent.CompletionException;

import org.araqnid.stuff.zedis.ZedisClient;
import org.araqnid.stuff.zedis.ZedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Monitor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.CompletableFuture.allOf;

public class RedisProcessor extends AbstractExecutionThreadService {
	private final ZedisClient client;
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
	private ZedisConnection conn;
	private boolean delivering;

	public RedisProcessor(ZedisClient client, String key, DeliveryTarget target) {
		this.client = client;
		this.key = key;
		this.target = target;
		this.log = LoggerFactory.getLogger(RedisProcessor.class.getName() + "." + key);
	}

	@Override
	protected void startUp() throws Exception {
		conn = client.connect().join();
	}

	@Override
	protected void run() throws Exception {
		String inProgressKey = key + processingSuffix;
		log.info("Consuming from Redis list \"{}\"", key);
		while (isRunning()) {
			log.debug("retrieving value from list");
			String value;
			try {
				value = conn.brpoplpush(key, inProgressKey, 30).thenApply(RedisProcessor::asString).join();
			} catch (CompletionException e) {
				if (!isRunning() && e.getCause() instanceof EOFException) {
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
						allOf(conn.rpush(key, value), conn.lrem(inProgressKey, 0, value)).join();
						return;
					}
					log.debug("<{}> retrieved", value);
					delivering = true;
				} finally {
					monitor.leave();
				}
				try {
					if (deliver(value)) {
						conn.lrem(inProgressKey, 0, value).join();
						log.debug("<{}> removed", value);
					}
					else {
						allOf(conn.lrem(inProgressKey, 0, value), conn.lpush(key, value)).join();
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
		log.debug("exiting main loop");
	}

	@Override
	protected void shutDown() throws Exception {
		log.info("Consumption stopped");
		conn.close();
	}

	@Override
	protected void triggerShutdown() {
		if (monitor.enterIf(notCurrentlyDelivering)) {
			log.debug("closing connection to trigger shutdown");
			monitor.leave();
			conn.close();
		}
		else {
			log.debug("currently delivering- leaving connection open");
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

	private static String asString(byte[] bytes) {
		if (bytes == null) return null;
		return new String(bytes, UTF_8);
	}

	@Override
	public String toString() {
		return "RedisProcessor:" + key + " => " + target + " [" + state() + "]";
	}

	public interface DeliveryTarget {
		boolean deliver(String data);
	}
}

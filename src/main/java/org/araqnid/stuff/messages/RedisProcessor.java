package org.araqnid.stuff.messages;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.araqnid.stuff.zedis.ZedisClient;
import org.araqnid.stuff.zedis.ZedisConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Monitor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.supplyAsync;

public class RedisProcessor extends AbstractService {
	private final ZedisClient client;
	private final String key;
	private final String inProgressKey;
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
	private final ExecutorService deliveryExecutor;

	public RedisProcessor(ZedisClient client, String key, DeliveryTarget target) {
		this.client = client;
		this.key = key;
		this.target = target;
		this.log = LoggerFactory.getLogger(RedisProcessor.class.getName() + "." + key);
		this.deliveryExecutor = Executors
				.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("RedisProcessor-" + key + "-%d").build());
		this.inProgressKey = key + ".working";
	}

	@Override
	protected void doStart() {
		CompletableFuture<ZedisConnection> connectionFuture;
		try {
			connectionFuture = client.connect();
		} catch (IOException e) {
			notifyFailed(e);
			return;
		}
		connectionFuture.whenComplete((ZedisConnection newConnection, Throwable ex) -> {
			if (newConnection != null) {
				conn = newConnection;
				notifyStarted();
				log.info("Consuming from Redis list \"{}\"", key);
				poll();
			}
			else {
				notifyFailed(ex);
			}
		});
	}

	@Override
	protected void doStop() {
		if (monitor.enterIf(notCurrentlyDelivering)) {
			log.debug("closing connection to trigger shutdown");
			monitor.leave();
			conn.close();
		}
		else {
			log.debug("currently delivering- leaving connection open");
		}
	}

	private void poll() {
		conn.brpoplpush(key, inProgressKey, 30).thenApply(RedisProcessor::asString).thenAccept(value -> {
			if (value == null) {
				poll();
			}
			else {
				processValue(value);
			}
		}).whenComplete((x, e) -> {
			if (e != null) {
				if (!isRunning() && e instanceof CompletionException && e.getCause() instanceof EOFException) {
					log.debug("Ignoring network exception during shutdown: " + e);
					notifyStopped();
				}
				else {
					notifyFailed(e);
				}
			}
		});
	}

	private void processValue(String value) {
		monitor.enter();
		boolean abort = false;
		try {
			if (!isRunning()) {
				abort = true;
			}
			else {
				delivering = true;
			}
		} finally {
			monitor.leave();
		}

		if (abort) {
			log.error("<{}> aborting delivery due to shutdown", value);
			allOf(conn.rpush(key, value), conn.lrem(inProgressKey, 0, value)).whenComplete((result, ex2) -> {
				conn.close();
				notifyStopped();
			});
			return;
		}
		else {
			dispatch(value);
		}
	}

	private void dispatch(String value) {
		supplyAsync(() -> deliver(value), deliveryExecutor).thenCompose(delivered -> {
			if (delivered) {
				return conn.lrem(inProgressKey, 0, value).thenRun(() -> {
					log.debug("<{}> removed", value);
				});
			}
			else {
				return allOf(conn.lrem(inProgressKey, 0, value), conn.lpush(key, value)).thenRun(() -> {
					log.debug("<{}> recycled", value);
				});
			}
		}).whenComplete((x, err) -> {
			if (err != null) {
				log.error("Unhandled exception delivering message, leaving it on in-progress key", err);
			}
			monitor.enter();
			try {
				delivering = false;
			} finally {
				monitor.leave();
			}
			if (isRunning()) {
				poll();
			}
			else {
				conn.close();
				notifyStopped();
			}
		});
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

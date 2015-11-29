package org.araqnid.stuff.zedis;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ManagedSelector;
import org.eclipse.jetty.io.SelectChannelEndPoint;
import org.eclipse.jetty.io.SelectorManager;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.TimerScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

public class ZedisClient implements Closeable {
	private static final Logger LOG = LoggerFactory.getLogger(ZedisClient.class);
	private final ZedisSelectorManager selectorManager;
	private final InetSocketAddress address;
	private final Scheduler scheduler;
	private final AtomicReference<ZedisConnection> readyConnection = new AtomicReference<>();
	private final BlockingDeque<Command> pendingCommands = new LinkedBlockingDeque<>();

	public ZedisClient(Executor executor, String host, int port) {
		this.scheduler = new TimerScheduler("Zedis-timer", false);
		this.selectorManager = new ZedisSelectorManager(executor, scheduler);
		this.address = new InetSocketAddress(host, port);
	}

	public void start() {
		try {
			scheduler.start();
		} catch (Exception e) {
			throw new RuntimeException("Failed to start scheduler", e);
		}
		try {
			selectorManager.start();
		} catch (Exception e) {
			throw new RuntimeException("Failed to start selector manager", e);
		}
	}

	public CompletableFuture<ZedisConnection> connect() throws IOException {
		SocketChannel socket = SocketChannel.open();
		socket.configureBlocking(false);
		CompletableFuture<ZedisConnection> future = new CompletableFuture<>();
		if (socket.connect(address)) {
			LOG.debug("connected to {}", address);
			selectorManager.accept(socket, future);
		}
		else {
			LOG.debug("registering for connection to {}", address);
			selectorManager.connect(socket, future);
		}
		return future;
	}

	@Override
	public void close() throws IOException {
		@SuppressWarnings("resource")
		ZedisConnection connection = readyConnection.get();
		if (connection != null) {
			LOG.debug("Closing connection");
			connection.close();
		}
		try {
			selectorManager.stop();
		} catch (Exception e) {
			LOG.warn("Failed to stop selector manager", e);
		}
		try {
			scheduler.stop();
		} catch (Exception e) {
			LOG.warn("Failed to stop scheduler", e);
		}
	}

	public void connectionReady(ZedisConnection connection) {
		readyConnection.set(connection);
		LOG.debug("connection ready");
		for (Command command : pendingCommands) {
			connection.send(command);
		}
	}

	public void connectionInvalid(ZedisConnection connection) {
		readyConnection.compareAndSet(connection, null);
		LOG.debug("connection invalidated");
	}

	public CompletableFuture<Object> command(String command, Object... args) throws IOException {
		Command commandObject = new Command(command, args);
		ZedisConnection connection = readyConnection.get();
		if (connection != null) {
			connection.send(commandObject);
		}
		else {
			pendingCommands.add(commandObject);
		}
		return commandObject.future();
	}

	public void lpush(String key, String value) throws IOException {
		command("RPUSH", key, value);
	}

	public void rpush(String key, String value) throws IOException {
		command("RPUSH", key, value);
	}

	public void lrem(String key, int count, String value) throws IOException {
		command("LREM", key, count, value);
	}

	public String brpoplpush(String key1, String key2, Duration timeout) throws IOException {
		return brpoplpush(key1, key2, (int) timeout.getSeconds());
	}

	public String brpoplpush(String key1, String key2, int timeoutSeconds) throws IOException {
		return command("BRPOPLPUSH", key1, key2, timeoutSeconds)
				.thenApply(
						o -> Optional.ofNullable((byte[]) o).map((byte[] b) -> new String(b, StandardCharsets.UTF_8)))
				.handle((Optional<String> value, Throwable ex) -> {
					if (ex instanceof EOFException) return null;
					if (ex instanceof CompletionException && ex.getCause() instanceof EOFException) return null;
					if (ex != null) throw Throwables.propagate(ex);
					return value.orElse(null);
				}).join();
	}

	public class ZedisSelectorManager extends SelectorManager {
		private final Executor executor;

		public ZedisSelectorManager(Executor executor, Scheduler scheduler) {
			super(executor, scheduler, 1);
			this.executor = executor;
		}

		@Override
		protected EndPoint newEndPoint(SocketChannel channel, ManagedSelector selector, SelectionKey key)
				throws IOException {
			return new SelectChannelEndPoint(channel, selector, key, getScheduler(), 0L);
		}

		@Override
		public Connection newConnection(SocketChannel channel, EndPoint endpoint, Object attachment)
				throws IOException {
			@SuppressWarnings("unchecked")
			CompletableFuture<ZedisConnection> future = (CompletableFuture<ZedisConnection>) attachment;
			ZedisConnection zedisConnection = new ZedisConnection(endpoint, executor);
			zedisConnection.addListener(new Connection.Listener() {
				@Override
				public void onOpened(Connection connection) {
					ZedisClient.LOG.info("connected to {}", address);
					future.complete(zedisConnection);
					connectionReady(zedisConnection);
				}

				@Override
				public void onClosed(Connection connection) {
					ZedisClient.LOG.info("connection closed: {}", connection);
					connectionInvalid(zedisConnection);
				}
			});
			return zedisConnection;
		}

		@Override
		protected void connectionFailed(SocketChannel channel, Throwable ex, Object attachment) {
			super.connectionFailed(channel, ex, attachment);
			ZedisClient.LOG.error("connection failed", ex);
			@SuppressWarnings("unchecked")
			CompletableFuture<ZedisConnection> future = (CompletableFuture<ZedisConnection>) attachment;
			future.completeExceptionally(ex);
		}
	}

}

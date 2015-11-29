package org.araqnid.stuff.zedis;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class Zedis implements Closeable {
	private final ZedisClient client;
	@Nullable
	private ZedisConnection connection;

	public Zedis() {
		this("localhost");
	}

	public Zedis(String host) {
		this(host, 6379);
	}

	public Zedis(String host, int port) {
		this(Executors.newCachedThreadPool(
				new ThreadFactoryBuilder().setNameFormat("Zedis-" + host + "-" + port + "-%d").setDaemon(true).build()),
				host, port);
	}

	public Zedis(Executor executor, String host, int port) {
		this.client = new ZedisClient(executor, host, port);
	}

	public void connect() throws IOException {
		client.start();
		connection = client.connect().join();
	}

	@Override
	public void close() throws IOException {
		client.close();
	}

	public CompletableFuture<Object> command(String command, Object... args) {
		Preconditions.checkState(connection != null, "Not connected");
		return connection.command(command, args);
	}

	public void lpush(String key, String value) throws IOException {
		command("RPUSH", key, value).join();
	}

	public void rpush(String key, String value) throws IOException {
		command("RPUSH", key, value).join();
	}

	public void lrem(String key, int count, String value) throws IOException {
		command("LREM", key, count, value).join();
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
}

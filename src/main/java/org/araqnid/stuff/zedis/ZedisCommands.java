package org.araqnid.stuff.zedis;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public interface ZedisCommands {
	CompletableFuture<Object> command(String command, Object... args);

	default CompletableFuture<byte[]> brpoplpush(byte[] key, String inProgressKey, Duration timeout) {
		return brpoplpush(key, inProgressKey, (int) timeout.getSeconds());
	}

	default CompletableFuture<byte[]> brpoplpush(String key, String inProgressKey, Duration timeout) {
		return brpoplpush(key, inProgressKey, (int) timeout.getSeconds());
	}

	default CompletableFuture<byte[]> brpoplpush(byte[] key, String inProgressKey, int timeoutSeconds) {
		return command("BRPOPLPUSH", key, inProgressKey, timeoutSeconds).thenApply(o -> (byte[]) o);
	}

	default CompletableFuture<byte[]> brpoplpush(String key, String inProgressKey, int timeoutSeconds) {
		return command("BRPOPLPUSH", key, inProgressKey, timeoutSeconds).thenApply(o -> (byte[]) o);
	}

	default CompletableFuture<Integer> lpush(byte[] key, byte[] value) {
		return command("LPUSH", key, value).thenApply(o -> ((Number) o).intValue());
	}

	default CompletableFuture<Integer> lpush(byte[] key, String value) {
		return command("LPUSH", key, value).thenApply(o -> ((Number) o).intValue());
	}

	default CompletableFuture<Integer> lpush(String key, byte[] value) {
		return command("LPUSH", key, value).thenApply(o -> ((Number) o).intValue());
	}

	default CompletableFuture<Integer> lpush(String key, String value) {
		return command("LPUSH", key, value).thenApply(o -> ((Number) o).intValue());
	}

	default CompletableFuture<Integer> lrem(byte[] key, int count, String value) {
		return command("LREM", key, count, value).thenApply(o -> ((Number) o).intValue());
	}

	default CompletableFuture<Integer> lrem(String key, int count, String value) {
		return command("LREM", key, count, value).thenApply(o -> ((Number) o).intValue());
	}

	default CompletableFuture<Integer> rpush(byte[] key, byte[] value) {
		return command("RPUSH", key, value).thenApply(o -> ((Number) o).intValue());
	}

	default CompletableFuture<Integer> rpush(byte[] key, String value) {
		return command("RPUSH", key, value).thenApply(o -> ((Number) o).intValue());
	}

	default CompletableFuture<Integer> rpush(String key, byte[] value) {
		return command("RPUSH", key, value).thenApply(o -> ((Number) o).intValue());
	}

	default CompletableFuture<Integer> rpush(String key, String value) {
		return command("RPUSH", key, value).thenApply(o -> ((Number) o).intValue());
	}
}

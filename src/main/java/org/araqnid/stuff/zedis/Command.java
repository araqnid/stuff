package org.araqnid.stuff.zedis;

import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableList;

import static org.araqnid.stuff.zedis.Marshaller.marshal;

final class Command {
	private final CompletableFuture<Object> responseCallback;
	private final byte[] marshalled;
	private final ResponseParser parser;
	private final String command;
	private final ImmutableList<Object> args;

	Command(String command, Object... args) {
		this.command = command;
		this.args = ImmutableList.copyOf(args);
		String[] parts = new String[args.length + 1];
		parts[0] = command;
		for (int i = 0; i < args.length; i++) {
			if (args[i] != null) {
				parts[i + 1] = String.valueOf(args[i]);
			}
		}
		this.marshalled = marshal(Arrays.asList(parts));
		this.responseCallback = new CompletableFuture<Object>();
		this.parser = new ResponseParser();
	}

	public boolean received(ByteBuffer buf) {
		if (!parser.consume(buf)) return false;
		Object value = parser.get();
		if (value instanceof ErrorMessage)
			responseCallback.completeExceptionally(new RemoteException(((ErrorMessage) value).message()));
		responseCallback.complete(value);
		return true;
	}

	public CompletableFuture<Object> future() {
		return responseCallback;
	}

	public void failed(Throwable x) {
		responseCallback.completeExceptionally(x);
	}

	public boolean isBlocking() {
		return marshalled[0] == 'B';
	}

	public byte[] asBytes() {
		return marshalled;
	}

	@Override
	public String toString() {
		return "Zedis.Command{" + command + " " + args + " | " + parser + "}";
	}
}

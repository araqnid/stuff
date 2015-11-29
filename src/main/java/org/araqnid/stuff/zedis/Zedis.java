package org.araqnid.stuff.zedis;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ManagedSelector;
import org.eclipse.jetty.io.SelectChannelEndPoint;
import org.eclipse.jetty.io.SelectorManager;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.IteratingCallback;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.TimerScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

public class Zedis implements Closeable {
	private static final Logger LOG = LoggerFactory.getLogger(Zedis.class);
	private final ZedisSelectorManager selectorManager;
	private final InetSocketAddress address;
	private final Scheduler scheduler;
	private SocketChannel socket;
	private final AtomicReference<ZedisConnection> readyConnection = new AtomicReference<>();
	private final BlockingDeque<Command> pendingCommands = new LinkedBlockingDeque<>();

	public Zedis(Executor executor, String host, int port) {
		this.scheduler = new TimerScheduler("Zedis-timer", false);
		this.selectorManager = new ZedisSelectorManager(executor, scheduler);
		this.address = new InetSocketAddress(host, port);
	}

	public void connect() throws IOException {
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
		beginConnection();
	}

	private void beginConnection() throws IOException {
		socket = SocketChannel.open();
		socket.configureBlocking(false);
		if (socket.connect(address)) {
			LOG.debug("connected to {}", address);
			selectorManager.accept(socket, null);
		}
		else {
			LOG.debug("registering for connection to {}", address);
			selectorManager.connect(socket, null);
		}
	}

	@Override
	public void close() throws IOException {
		@SuppressWarnings("resource")
		ZedisConnection connection = readyConnection.get();
		if (connection != null) {
			LOG.debug("Closing connection");
			connection.close();
		}
		else if (socket != null) {
			LOG.debug("Closing socket");
			socket.close();
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
			return new ZedisConnection(endpoint, executor);
		}

		@Override
		public void connectionOpened(Connection connection) {
			super.connectionOpened(connection);
			Zedis.LOG.info("connected to {}", address);
		}

		@Override
		public void connectionClosed(Connection connection) {
			super.connectionClosed(connection);
			Zedis.LOG.info("connection closed: {}", connection);
		}

		@Override
		protected void connectionFailed(SocketChannel channel, Throwable ex, Object attachment) {
			super.connectionFailed(channel, ex, attachment);
			Zedis.LOG.error("connection failed", ex);
		}
	}

	enum SendState {
		CONNECTING, IDLE, SENDING, BLOCKING
	};

	enum ReceiveState {
		CONNECTING, IDLE, LISTENING, READING
	};

	public class ZedisConnection extends AbstractConnection {
		private SendState sendState = SendState.CONNECTING;
		private ReceiveState recvState = ReceiveState.CONNECTING;
		private final Deque<Command> commandsSent = new LinkedBlockingDeque<>();
		private final Deque<Command> commandQueue = new LinkedBlockingDeque<>();
		private final ByteBuffer responseBuf = ByteBuffer.allocate(1024);

		public ZedisConnection(EndPoint endp, Executor executor) {
			super(endp, executor);
			responseBuf.position(0);
			responseBuf.limit(0);
		}

		public final class SendCommand extends IteratingCallback {
			private final Command command;
			private final Callback callback;
			private final ByteBuffer msg;

			public SendCommand(Command command, Callback callback) {
				this.command = command;
				this.callback = callback;
				msg = ByteBuffer.wrap(command.asBytes());
			}

			@Override
			protected Action process() throws Exception {
				if (msg.position() == msg.limit()) {
					if (command.isBlocking()) {
						LOG.debug("finished flushing command -> BLOCKING");
						sendState = SendState.BLOCKING;
					}
					else {
						LOG.debug("finished flushing command -> IDLE");
						sendState = SendState.IDLE;
					}
					callback.succeeded();
					startListening();
					return Action.SUCCEEDED;
				}
				LOG.debug("sending {}", command);
				getEndPoint().write(this, msg);
				return Action.SCHEDULED;
			}
		}

		public final class QueueSender extends IteratingCallback {
			@Override
			protected Action process() throws Exception {
				Command command;
				synchronized (ZedisConnection.this) {
					LOG.debug("QueueSender: sendState={} recvState={} qsize={}", sendState, recvState,
							commandQueue.size());
					if (sendState != SendState.IDLE) return Action.SUCCEEDED;
					command = commandQueue.poll();
					if (command == null) return Action.SUCCEEDED;
					sendState = SendState.SENDING;
					commandsSent.addLast(command);
				}
				new SendCommand(command, this).iterate();
				return Action.SCHEDULED;
			}
		}

		private void startListening() {
			boolean start;
			synchronized (this) {
				if (recvState == ReceiveState.IDLE) {
					start = true;
					recvState = ReceiveState.LISTENING;
				}
				else {
					start = false;
				}
			}
			if (start) fillInterested();
		}

		@Override
		public void onOpen() {
			super.onOpen();
			synchronized (this) {
				sendState = SendState.IDLE;
				recvState = ReceiveState.IDLE;
			}
			connectionReady(this);
		}

		public void send(Command command) {
			commandQueue.add(command);
			sendCommands();
		}

		public void sendCommands() {
			new QueueSender().iterate();
		}

		@Override
		public void onFillable() {
			synchronized (this) {
				recvState = ReceiveState.READING;
			}
			try {
				int got = getEndPoint().fill(responseBuf);
				LOG.debug("filled {}", got);

				if (got < 0) {
					LOG.warn("EOF on input");
					connectionInvalid(this);
					socket.close();
					return;
				}

				boolean doMore = true;
				while (doMore && responseBuf.position() < responseBuf.limit()) {
					Command sentCommand = commandsSent.getFirst();
					LOG.debug("responding to {} ({})", sentCommand, responseBuf);
					if (sentCommand.received(responseBuf)) {
						commandsSent.removeFirst();
						if (sentCommand.isBlocking()) {
							LOG.debug("got response to blocking command, reactivate send queue");
							sendState = SendState.IDLE;
							new QueueSender().iterate();
						}
						doMore = true;
					}
					else {
						doMore = false;
					}
				}
			} catch (IOException e) {
				LOG.error("Exception filling input", e);
			}
			boolean listen;
			synchronized (this) {
				if (!commandsSent.isEmpty()) {
					recvState = ReceiveState.LISTENING;
					listen = true;
				}
				else {
					recvState = ReceiveState.IDLE;
					listen = false;
				}
			}
			if (listen) fillInterested();
		}

		@Override
		public void onClose() {
			super.onClose();
			LOG.debug("onClose {}", this);
			for (Command command : commandsSent) {
				command.failed(new EOFException());
			}
			for (Command command : commandQueue) {
				command.failed(new EOFException());
			}
		}

		@Override
		public String toString() {
			return "ZedisConnection{" + sendState + "," + recvState + "}";
		}
	}
}

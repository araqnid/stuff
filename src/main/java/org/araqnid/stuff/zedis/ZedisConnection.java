package org.araqnid.stuff.zedis;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;

import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.IteratingCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ZedisConnection extends AbstractConnection {
	private static final Logger LOG = LoggerFactory.getLogger(ZedisConnection.class);
	private ZedisConnection.SendState sendState = ZedisConnection.SendState.CONNECTING;
	private ZedisConnection.ReceiveState recvState = ZedisConnection.ReceiveState.CONNECTING;
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
					sendState = ZedisConnection.SendState.BLOCKING;
				}
				else {
					LOG.debug("finished flushing command -> IDLE");
					sendState = ZedisConnection.SendState.IDLE;
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
				LOG.debug("QueueSender: sendState={} recvState={} qsize={}", sendState, recvState, commandQueue.size());
				if (sendState != ZedisConnection.SendState.IDLE) return Action.SUCCEEDED;
				command = commandQueue.poll();
				if (command == null) return Action.SUCCEEDED;
				sendState = ZedisConnection.SendState.SENDING;
				commandsSent.addLast(command);
			}
			new SendCommand(command, this).iterate();
			return Action.SCHEDULED;
		}
	}

	private enum SendState {
		CONNECTING, IDLE, SENDING, BLOCKING
	}

	private enum ReceiveState {
		CONNECTING, IDLE, LISTENING, READING
	}

	private void startListening() {
		boolean start;
		synchronized (this) {
			if (recvState == ZedisConnection.ReceiveState.IDLE) {
				start = true;
				recvState = ZedisConnection.ReceiveState.LISTENING;
			}
			else {
				start = false;
			}
		}
		if (start) fillInterested();
	}

	@Override
	public void onOpen() {
		synchronized (this) {
			sendState = ZedisConnection.SendState.IDLE;
			recvState = ZedisConnection.ReceiveState.IDLE;
		}
		super.onOpen();
	}

	public CompletableFuture<Object> command(String command, Object... args) {
		Command commandObject = new Command(command, args);
		send(commandObject);
		return commandObject.future();
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
			recvState = ZedisConnection.ReceiveState.READING;
		}
		try {
			int got = getEndPoint().fill(responseBuf);
			LOG.debug("filled {}", got);

			if (got < 0) {
				LOG.warn("EOF on input");
				getEndPoint().close();
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
						sendState = ZedisConnection.SendState.IDLE;
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
				recvState = ZedisConnection.ReceiveState.LISTENING;
				listen = true;
			}
			else {
				recvState = ZedisConnection.ReceiveState.IDLE;
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

package org.araqnid.stuff.zedis;

import java.nio.ByteBuffer;

public class ResponseParser {
	private enum State {
		TYPE, NUMBER, CR_LF, FINISHED, BULK_STRING;
	};

	private State state = State.TYPE;
	private State stacked;
	private long n = 0;
	private Object value;
	private ByteBuffer str;

	public boolean consume(ByteBuffer buf) {
		while (buf.hasRemaining()) {
			char c = (char) buf.get();
			switch (state) {
			case TYPE:
				switch (c) {
				case ':':
					state = State.NUMBER;
					break;
				case '$':
					state = State.NUMBER;
					stacked = State.BULK_STRING;
					break;
				default:
					throw new IllegalStateException(toString() + " got " + c);
				}
				break;
			case NUMBER:
				if (c == '\r') {
					value = n;
					state = State.CR_LF;
				}
				else if (Character.isDigit(c)) {
					int digit = c - '0';
					n = 10 * n + digit;
				}
				else throw new IllegalStateException(toString() + " got " + c);
				break;
			case CR_LF:
				if (c == '\n') {
					if (stacked == null) {
						state = State.FINISHED;
						return true;
					}
					else {
						state = stacked;
						stacked = null;
					}
				}
				else {
					throw new IllegalStateException(toString() + " got " + c);
				}
				break;
			case BULK_STRING:
				if (n == 0) {
					if (c == '\r') {
						state = State.CR_LF;
					}
					else {
						throw new IllegalStateException(toString() + " got " + c);
					}
				}
				else {
					if (str == null) {
						if (buf.hasArray() && buf.remaining() >= n) {
							// copy directly to result
							byte[] b = new byte[(int) n];
							System.arraycopy(buf.array(), buf.position() - 1, b, 0, (int) n);
							buf.position(buf.position() - 1 + (int) n);
							n = 0;
							value = b;
							break;
						}
						str = ByteBuffer.allocate((int) n);
						value = str.array();
					}
					str.put((byte) c);
					n--;
					if (buf.hasArray()) {
						int take = (int) n;
						if (take > buf.remaining()) {
							take = buf.remaining();
						}
						str.put(buf.array(), buf.position(), take);
						n -= take;
						buf.position(buf.position() + take);
					}
				}
				break;
			default:
				throw new IllegalStateException(toString());
			}
		}
		return false;
	}

	public Object get() {
		if (state != State.FINISHED) throw new IllegalStateException(toString());
		return value;
	}

	@Override
	public String toString() {
		return "ResponseParser{" + state + ",n=" + n + "}";
	}
}

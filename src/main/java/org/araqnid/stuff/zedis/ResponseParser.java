package org.araqnid.stuff.zedis;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

public class ResponseParser {
	private enum State {
		TYPE, NUMBER, NUMBER_CONTINUED, CR_LF, FINISHED, SIMPLE_STRING, ERROR_STRING, BULK_STRING, ARRAY;
	};

	private State state = State.TYPE;
	private State stacked;
	private long n = 0;
	private int sign = 0;
	private Object value;
	private StringBuilder str;
	private ByteBuffer bytes;
	private List<Object> list;
	private int arrayLength;

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
				case '+':
					state = State.SIMPLE_STRING;
					str = new StringBuilder();
					break;
				case '-':
					state = State.ERROR_STRING;
					str = new StringBuilder();
					break;
				case '*':
					state = State.NUMBER;
					stacked = State.ARRAY;
					break;
				default:
					throw new IllegalStateException(toString() + " got " + c);
				}
				break;
			case NUMBER:
				if (c == '-') {
					sign = -1;
					n = 0;
					state = State.NUMBER_CONTINUED;
				}
				else if (Character.isDigit(c)) {
					n = c - '0';
					sign = 1;
					state = State.NUMBER_CONTINUED;
				}
				else throw new IllegalStateException(toString() + " got " + c);
				break;
			case NUMBER_CONTINUED:
				if (c == '\r') {
					value = n * sign;
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
						if (list != null) {
							list.add(value);
							if (list.size() < arrayLength) {
								value = null;
								state = State.TYPE;
								break;
							} else {
								value = list;
							}
						}
						state = State.FINISHED;
						return true;
					}
					else {
						state = stacked;
						stacked = null;
						if ((state == State.BULK_STRING || state == State.ARRAY) && sign < 0) {
							value = null;
							state = State.FINISHED;
							return true;
						}
						if (state == State.ARRAY) {
							if (n == 0) {
								value = ImmutableList.of();
								state = State.FINISHED;
								return true;
							}
							else {
								arrayLength = (int) n;
								list = new ArrayList<Object>(arrayLength);
								state = State.TYPE;
							}
						}
					}
				}
				else {
					throw new IllegalStateException(toString() + " got " + c);
				}
				break;
			case SIMPLE_STRING:
				if (c == '\r') {
					value = str.toString();
					state = State.CR_LF;
				}
				else {
					str.append(c);
				}
				break;
			case ERROR_STRING:
				if (c == '\r') {
					value = new ErrorMessage(str.toString());
					state = State.CR_LF;
				}
				else {
					str.append(c);
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
					if (bytes == null) {
						if (buf.hasArray() && buf.remaining() >= n) {
							// copy directly to result
							byte[] b = new byte[(int) n];
							System.arraycopy(buf.array(), buf.position() - 1, b, 0, (int) n);
							buf.position(buf.position() - 1 + (int) n);
							n = 0;
							value = b;
							break;
						}
						bytes = ByteBuffer.allocate((int) n);
						value = bytes.array();
					}
					bytes.put((byte) c);
					n--;
					if (buf.hasArray()) {
						int take = (int) n;
						if (take > buf.remaining()) {
							take = buf.remaining();
						}
						bytes.put(buf.array(), buf.position(), take);
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
		return MoreObjects.toStringHelper(this)
				.omitNullValues()
				.addValue(state)
				.add("n", n)
				.add("sgn", sign)
				.add("bytes", bytes)
				.add("str", str)
				.add("list", list)
				.add("arrayLength", arrayLength)
				.toString();
	}
}

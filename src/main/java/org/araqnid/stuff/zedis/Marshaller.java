package org.araqnid.stuff.zedis;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;

public final class Marshaller {
	public static byte[] marshal(Object... values) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			for (Object value : values) {
				marshal(value, baos);
			}
			return baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void marshal(Object value, OutputStream output) throws IOException {
		if (value == null) {
			marshalNullBulkString(output);
		}
		else if (value instanceof CharSequence) {
			marshalBulkString((CharSequence) value, output);
		}
		else if (value instanceof byte[]) {
			marshalBulkString((byte[]) value, output);
		}
		else if (value instanceof Long) {
			marshalNumber((Long) value, output);
		}
		else if (value instanceof Integer) {
			marshalNumber((Integer) value, output);
		}
		else if (value instanceof Collection) {
			marshalArray((Collection<?>) value, output);
		}
		else {
			throw new IllegalArgumentException("Unhandled value type: " + value.getClass().getName());
		}
	}

	private static void marshalArray(Collection<?> value, OutputStream output) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(output, UTF_8);
		output.write('*');
		writer.write(Integer.toString(value.size()));
		writer.flush();
		output.write(CRLF);
		for (Object subvalue : value) {
			marshal(subvalue, output);
		}
	}

	private static void marshalNumber(long value, OutputStream output) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(output, UTF_8);
		output.write(':');
		writer.write(Long.toString(value));
		writer.flush();
		output.write(CRLF);
	}

	private static void marshalBulkString(byte[] value, OutputStream output) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(output, UTF_8);
		output.write('$');
		writer.write(Integer.toString(value.length));
		writer.flush();
		output.write(CRLF);
		output.write(value);
		output.write(CRLF);
	}

	private static void marshalBulkString(CharSequence value, OutputStream output) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(output, UTF_8);
		output.write('$');
		writer.write(Integer.toString(value.length()));
		writer.flush();
		output.write(CRLF);
		writer.write(value.toString());
		writer.flush();
		output.write(CRLF);
	}

	private static final byte[] CRLF = "\r\n".getBytes(UTF_8);
	private static final byte[] NULL_BULK_STRING = "$-1\r\n".getBytes(UTF_8);

	private static void marshalNullBulkString(OutputStream output) throws IOException {
		output.write(NULL_BULK_STRING);
	}

	private Marshaller() {
	}
}

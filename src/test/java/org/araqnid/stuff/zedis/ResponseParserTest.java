package org.araqnid.stuff.zedis;

import java.nio.ByteBuffer;

import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ResponseParserTest {
	@Test
	public void needs_input() throws Exception {
		ResponseParser parser = new ResponseParser();
		assertThat(parser.consume(ByteBuffer.wrap("".getBytes(UTF_8))), equalTo(false));
	}

	@Test(expected = IllegalStateException.class)
	public void cannot_get_value_while_input_needed() throws Exception {
		ResponseParser parser = new ResponseParser();
		assertThat(parser.consume(ByteBuffer.wrap("".getBytes(UTF_8))), equalTo(false));
		parser.get();
	}

	@Test
	public void reads_number() throws Exception {
		ResponseParser parser = new ResponseParser();
		assertThat(parser.consume(ByteBuffer.wrap(":12345\r\n".getBytes(UTF_8))), equalTo(true));
		assertThat(parser.get(), equalTo(12345L));
	}

	@Test
	public void needs_input_after_partial_number() throws Exception {
		ResponseParser parser = new ResponseParser();
		assertThat(parser.consume(ByteBuffer.wrap(":12345".getBytes(UTF_8))), equalTo(false));
	}

	@Test
	public void needs_input_after_cr() throws Exception {
		ResponseParser parser = new ResponseParser();
		assertThat(parser.consume(ByteBuffer.wrap(":12345".getBytes(UTF_8))), equalTo(false));
	}

	@Test(expected = IllegalStateException.class)
	public void rejects_invalid_number() throws Exception {
		ResponseParser parser = new ResponseParser();
		parser.consume(ByteBuffer.wrap(":xxx\r\n".getBytes(UTF_8)));
	}

	@Test
	public void reads_bulk_string() throws Exception {
		ResponseParser parser = new ResponseParser();
		assertThat(parser.consume(ByteBuffer.wrap("$3\r\nfoo\r\n".getBytes(UTF_8))), equalTo(true));
		assertThat(new String((byte[]) parser.get()), equalTo("foo"));
	}
}

package org.araqnid.stuff.zedis;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

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
	public void reads_negative_number() throws Exception {
		ResponseParser parser = new ResponseParser();
		assertThat(parser.consume(ByteBuffer.wrap(":-1\r\n".getBytes(UTF_8))), equalTo(true));
		assertThat(parser.get(), equalTo(-1L));
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
	public void reads_simple_string() throws Exception {
		ResponseParser parser = new ResponseParser();
		assertThat(parser.consume(ByteBuffer.wrap("+OK\r\n".getBytes(UTF_8))), equalTo(true));
		assertThat(parser.get(), equalTo("OK"));
	}

	@Test
	public void reads_bulk_string() throws Exception {
		ResponseParser parser = new ResponseParser();
		assertThat(parser.consume(ByteBuffer.wrap("$3\r\nfoo\r\n".getBytes(UTF_8))), equalTo(true));
		looselyAssert(parser.get(), sameBytes("foo"));
	}

	@Test
	public void reads_null_bulk_string() throws Exception {
		ResponseParser parser = new ResponseParser();
		assertThat(parser.consume(ByteBuffer.wrap("$-1\r\n".getBytes(UTF_8))), equalTo(true));
		looselyAssert(parser.get(), nullValue());
	}

	@Test
	public void reads_error() throws Exception {
		ResponseParser parser = new ResponseParser();
		assertThat(parser.consume(ByteBuffer.wrap("-ERR unknown command 'foobar'\r\n".getBytes(UTF_8))), equalTo(true));
		looselyAssert(parser.get(), errorMessage("ERR unknown command 'foobar'"));
	}

	@Test
	public void reads_empty_array() throws Exception {
		ResponseParser parser = new ResponseParser();
		assertThat(parser.consume(ByteBuffer.wrap("*0\r\n".getBytes(UTF_8))), equalTo(true));
		looselyAssert(parser.get(), equalTo(ImmutableList.of()));
	}

	@Test
	public void reads_null_array() throws Exception {
		ResponseParser parser = new ResponseParser();
		assertThat(parser.consume(ByteBuffer.wrap("*-1\r\n".getBytes(UTF_8))), equalTo(true));
		looselyAssert(parser.get(), nullValue());
	}

	@Test
	public void reads_array_of_numbers() throws Exception {
		ResponseParser parser = new ResponseParser();
		assertThat(parser.consume(ByteBuffer.wrap("*2\r\n:1\r\n:2\r\n".getBytes(UTF_8))), equalTo(true));
		looselyAssert(parser.get(), equalTo(ImmutableList.of(1L, 2L)));
	}

	@Test
	public void reads_array_of_bulk_strings() throws Exception {
		ResponseParser parser = new ResponseParser();
		assertThat(parser.consume(ByteBuffer.wrap("*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n".getBytes(UTF_8))), equalTo(true));
		looselyAssert(parser.get(), hasSize(2));
	}

	@SuppressWarnings("unchecked")
	private static <V> void looselyAssert(Object value, Matcher<? super V> matcher) {
		assertThat(value, (Matcher<Object>) matcher);
	}

	private static Matcher<ErrorMessage> errorMessage(String message) {
		return new TypeSafeDiagnosingMatcher<ErrorMessage>() {
			private final Matcher<String> messageMatcher = equalTo(message);

			@Override
			protected boolean matchesSafely(ErrorMessage item, Description mismatchDescription) {
				mismatchDescription.appendText("message ");
				messageMatcher.describeMismatch(item.message(), mismatchDescription);
				return messageMatcher.matches(item.message());
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("error message ").appendValue(message);
			}
		};
	}

	private static Matcher<byte[]> sameBytes(String value) {
		return new TypeSafeDiagnosingMatcher<byte[]>() {
			private final byte[] bytes = value.getBytes(UTF_8);

			@Override
			protected boolean matchesSafely(byte[] item, Description mismatchDescription) {
				mismatchDescription.appendText("bytes looked like ").appendValue(new String(item, UTF_8));
				return Arrays.equals(item, bytes);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("same bytes as ").appendValue(value);
			}
		};
	}
}

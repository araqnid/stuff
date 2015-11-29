package org.araqnid.stuff.zedis;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.araqnid.stuff.zedis.Marshaller.marshal;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class MarshallerTest {
	@Test
	public void marshals_null() throws Exception {
		assertThat(marshal((String) null), sameBytes("$-1\r\n"));
	}

	@Test
	public void marshals_string() throws Exception {
		assertThat(marshal("foo"), sameBytes("$3\r\nfoo\r\n"));
	}

	@Test
	public void marshals_multiple_strings() throws Exception {
		assertThat(marshal("foo", "bar", "quux"), sameBytes("$3\r\nfoo\r\n$3\r\nbar\r\n$4\r\nquux\r\n"));
	}

	@Test
	public void marshals_bytes() throws Exception {
		assertThat(marshal(new byte[] { -1, -2, -3 }), sameBytes("$3\r\n\u00ff\u00fe\u00fd\r\n"));
	}

	@Test
	public void marshals_integer() throws Exception {
		assertThat(marshal(12345), sameBytes(":12345\r\n"));
	}

	@Test
	public void marshals_negative_integer() throws Exception {
		assertThat(marshal(-12345), sameBytes(":-12345\r\n"));
	}

	@Test
	public void marshals_long() throws Exception {
		assertThat(marshal(123451234512345L), sameBytes(":123451234512345\r\n"));
	}

	@Test
	public void marshals_list() throws Exception {
		assertThat(marshal(ImmutableList.of(1, 2, 3)), sameBytes("*3\r\n:1\r\n:2\r\n:3\r\n"));
	}

	@Test
	public void marshals_empty_list() throws Exception {
		assertThat(marshal(ImmutableList.of()), sameBytes("*0\r\n"));
	}

	private static Matcher<byte[]> sameBytes(String value) {
		return new TypeSafeDiagnosingMatcher<byte[]>() {
			private final byte[] bytes = value.getBytes(ISO_8859_1);

			@Override
			protected boolean matchesSafely(byte[] item, Description mismatchDescription) {
				mismatchDescription.appendText("bytes looked like ").appendValue(new String(item, ISO_8859_1));
				return Arrays.equals(item, bytes);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("same bytes as ").appendValue(value);
			}
		};
	}
}

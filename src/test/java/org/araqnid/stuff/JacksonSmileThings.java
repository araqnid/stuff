package org.araqnid.stuff;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class JacksonSmileThings {

	@Test
	public void string_written_as_smile() throws Exception {
		ObjectMapper smileMapper = new ObjectMapper(new SmileFactory());
		assertThat(smileMapper.writer().writeValueAsBytes("test"), looksLike(":)\n\u0001Ctest"));
	}

	@Test
	public void object_written_as_smile() throws Exception {
		ObjectMapper smileMapper = new ObjectMapper(new SmileFactory());
		assertThat(
				smileMapper.writer()
						.writeValueAsBytes(ImmutableMap.of("values",
								ImmutableList.of(ImmutableMap.of("colour", "red", "value", 1.2345),
										ImmutableMap.of("colour", "orange", "value", 1.2345),
										ImmutableMap.of("colour", "green", "value", 1.2345),
										ImmutableMap.of("colour", "blue", "value", 1.2345),
										ImmutableMap.of("colour", "indigo", "value", 1.2345),
										ImmutableMap.of("colour", "violet", "value", 1.2345)))),
				looksLike(":)\n\u0001\u00fa\u0085values\u00f8\u00fa\u0085colour"
						+ "Bred\u0084value)\u0000?yp\u00101\u0013:/\r\u00fb\u00faAEorangeB)\u0000?yp\u00101\u0013:/\r\u00fb\u00faADgreen"
						+ "B)\u0000?yp\u00101\u0013:/\r\u00fb\u00faACblueB)\u0000?yp\u00101\u0013:/\r\u00fb\u00faAEindigo"
						+ "B)\u0000?yp\u00101\u0013:/\r\u00fb\u00faAEvioletB)\u0000?yp\u00101\u0013:/\r\u00fb\u00f9\u00fb"));
	}

	@Test
	@Ignore("doesn't seem to work this way")
	public void string_written_as_smile_configured_in_writer() throws Exception {
		assertThat(new String(new ObjectMapper().writer().with(new SmileFactory()).writeValueAsBytes("test"),
				StandardCharsets.ISO_8859_1), equalTo(":)\n\u0001Ctest"));
	}

	private static Matcher<byte[]> looksLike(String example) {
		byte[] exampleBytes = example.getBytes(StandardCharsets.ISO_8859_1);
		return new TypeSafeDiagnosingMatcher<byte[]>() {
			@Override
			protected boolean matchesSafely(byte[] item, Description mismatchDescription) {
				if (Arrays.equals(item, exampleBytes)) { return true; }
				mismatchDescription.appendText("was ").appendValue(escape(item));
				return false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("bytes like ").appendValue(escape(exampleBytes));
			}

			private String escape(byte[] item) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < item.length; i++) {
					byte b = item[i];
					if (b == 10) {
						sb.append('\n');
					}
					else if (b == 13) {
						sb.append('\r');
					}
					else if (b < 32 || b == 127) {
						sb.append('\\');
						sb.append('x');
						sb.append(String.format("%02x", (b) & 0xff));
					}
					else {
						sb.append((char) b);
					}
				}
				String escaped = sb.toString();
				return escaped;
			}
		};
	}

}

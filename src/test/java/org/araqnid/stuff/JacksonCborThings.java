package org.araqnid.stuff;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static org.hamcrest.MatcherAssert.assertThat;

public class JacksonCborThings {

	@Test
	public void string_written_as_cbor_text_string() throws Exception {
		ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
		byte[] asUnicodeString = new byte[] { cborIntro(CBOR_TEXT_STRING, 4), 't', 'e', 's', 't' };
		assertThat(cborMapper.writer().writeValueAsBytes("test"), looksLike(asUnicodeString));
	}

	@Test
	public void object_written_as_cbor() throws Exception {
		ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
		assertThat(
				cborMapper.writer()
						.writeValueAsBytes(ImmutableMap.of("values",
								ImmutableList.of(ImmutableMap.of("colour", "red", "value", 1.2345),
										ImmutableMap.of("colour", "orange", "value", 1.2345),
										ImmutableMap.of("colour", "green", "value", 1.2345),
										ImmutableMap.of("colour", "blue", "value", 1.2345),
										ImmutableMap.of("colour", "indigo", "value", 1.2345),
										ImmutableMap.of("colour", "violet", "value", 1.2345)))),
				looksLike("\u00bffvalues\u009f\u00bffcolourcredevalue\u00fb?\u00f3\u00c0\u0083\u0012n\u0097\u008d\u00ff"
						+ "\u00bffcolourforangeevalue\u00fb?\u00f3\u00c0\u0083\u0012n\u0097\u008d\u00ff"
						+ "\u00bffcolouregreenevalue\u00fb?\u00f3\u00c0\u0083\u0012n\u0097\u008d\u00ff"
						+ "\u00bffcolourdblueevalue\u00fb?\u00f3\u00c0\u0083\u0012n\u0097\u008d\u00ff"
						+ "\u00bffcolourfindigoevalue\u00fb?\u00f3\u00c0\u0083\u0012n\u0097\u008d\u00ff"
						+ "\u00bffcolourfvioletevalue\u00fb?\u00f3\u00c0\u0083\u0012n\u0097\u008d\u00ff\u00ff\u00ff"));
	}

	@Test
	public void map_written_with_indefinite_length() throws Exception {
		ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
		assertThat(cborMapper.writer().writeValueAsBytes(ImmutableMap.of("abc", "def")),
				looksLike(new byte[] { cborIntro(CBOR_MAP, 31), cborIntro(CBOR_TEXT_STRING, 3), 'a', 'b', 'c',
						cborIntro(CBOR_TEXT_STRING, 3), 'd', 'e', 'f', cborBreak() }));
	}

	@Test
	public void list_written_with_indefinite_length() throws Exception {
		ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
		assertThat(cborMapper.writer().writeValueAsBytes(ImmutableList.of("abc", "def")),
				looksLike(new byte[] { cborIntro(CBOR_ARRAY, 31), cborIntro(CBOR_TEXT_STRING, 3), 'a', 'b', 'c',
						cborIntro(CBOR_TEXT_STRING, 3), 'd', 'e', 'f', cborBreak() }));
	}

	@Test
	public void pojo_written_as_map_with_indefinite_length() throws Exception {
		ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
		SimpleDatum datum = new SimpleDatum();
		datum.id = "abc";
		datum.name = "def";
		assertThat(cborMapper.writer().writeValueAsBytes(datum),
				looksLike(new byte[] { cborIntro(CBOR_MAP, 31), cborIntro(CBOR_TEXT_STRING, 2), 'i', 'd',
						cborIntro(CBOR_TEXT_STRING, 3), 'a', 'b', 'c', cborIntro(CBOR_TEXT_STRING, 4), 'n', 'a', 'm',
						'e', cborIntro(CBOR_TEXT_STRING, 3), 'd', 'e', 'f', cborBreak() }));
	}

	@Test
	public void true_false_and_null_written_as_single_bytes() throws Exception {
		ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
		assertThat(cborMapper.writer().writeValueAsBytes(Arrays.asList(true, false, null)),
				looksLike(new byte[] { cborIntro(CBOR_ARRAY, 31), cborIntro(CBOR_SPECIAL, 21),
						cborIntro(CBOR_SPECIAL, 20), cborIntro(CBOR_SPECIAL, 22), cborBreak() }));
	}

	private byte cborBreak() {
		return cborIntro(CBOR_SPECIAL, 31);
	}

	public static final class SimpleDatum {
		public String id;
		public String name;
	}

	// major types
	static final int CBOR_UINT = 0;
	static final int CBOR_NEGINT = 1;
	static final int CBOR_BYTE_STRING = 2;
	static final int CBOR_TEXT_STRING = 3;
	static final int CBOR_ARRAY = 4;
	static final int CBOR_MAP = 5;
	static final int CBOR_TAG = 6;
	static final int CBOR_SPECIAL = 7;

	private static byte cborIntro(int majorType, int otherBits) {
		return (byte) ((majorType & 7) << 5 | (otherBits & 31));
	}

	private static Matcher<byte[]> looksLike(byte[] exampleBytes) {
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

	private static Matcher<byte[]> looksLike(String example) {
		return looksLike(example.getBytes(StandardCharsets.ISO_8859_1));
	}
}

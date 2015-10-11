package org.araqnid.stuff;

import java.nio.charset.Charset;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertTrue;

public class SometubeHandlerTest {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private ObjectMapper mapper = new ObjectMapper();

	@Test
	public void parses_payload() {
		SometubeHandler handler = new SometubeHandler(mapper);
		assertTrue(handler.deliver("{ \"id\": 12345 }".getBytes(UTF8)));
	}

	@Test
	public void ignores_message_with_invalid_payload() {
		SometubeHandler handler = new SometubeHandler(mapper);
		assertTrue(handler.deliver("clunk".getBytes(UTF8)));
	}
}

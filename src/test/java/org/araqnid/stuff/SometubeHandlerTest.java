package org.araqnid.stuff;

import java.nio.charset.Charset;

import org.junit.Test;

import com.google.gson.JsonSyntaxException;

public class SometubeHandlerTest {
	private static final Charset UTF8 = Charset.forName("UTF-8");

	@Test
	public void parses_payload() {
		SometubeHandler handler = new SometubeHandler();
		handler.deliver("{ id: 12345 }".getBytes(UTF8));
	}

	@Test(expected = JsonSyntaxException.class)
	public void barfs_on_invalid_payload() {
		SometubeHandler handler = new SometubeHandler();
		handler.deliver("clunk".getBytes(UTF8));
	}
}

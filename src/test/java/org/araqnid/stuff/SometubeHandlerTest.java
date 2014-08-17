package org.araqnid.stuff;

import java.nio.charset.Charset;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.junit.Test;

public class SometubeHandlerTest {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private JsonFactory jsonFactory = new MappingJsonFactory();

	@Test
	public void parses_payload() {
		SometubeHandler handler = new SometubeHandler(jsonFactory);
		handler.deliver("{ \"id\": 12345 }".getBytes(UTF8));
	}

	@Test(expected = Exception.class)
	public void barfs_on_invalid_payload() {
		SometubeHandler handler = new SometubeHandler(jsonFactory);
		handler.deliver("clunk".getBytes(UTF8));
	}
}

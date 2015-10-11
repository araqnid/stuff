package org.araqnid.stuff;

import java.nio.charset.Charset;
import java.util.UUID;

import org.araqnid.stuff.activity.Activity;
import org.araqnid.stuff.activity.ActivityEventSink;
import org.araqnid.stuff.activity.ActivityScope;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class SometubeHandlerTest {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private final ObjectMapper mapper = new ObjectMapper();
	private final Activity activity = new Activity(UUID.randomUUID(), "Test", null, mock(ActivityEventSink.class));
	private final ActivityScope activityScope = () -> activity.root;

	@Test
	public void parses_payload() {
		SometubeHandler handler = new SometubeHandler(mapper, activityScope);
		assertTrue(handler.deliver("{ \"id\": 12345 }".getBytes(UTF8)));
	}

	@Test
	public void ignores_message_with_invalid_payload() {
		SometubeHandler handler = new SometubeHandler(mapper, activityScope);
		assertTrue(handler.deliver("clunk".getBytes(UTF8)));
	}
}

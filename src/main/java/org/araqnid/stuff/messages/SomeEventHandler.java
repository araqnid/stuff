package org.araqnid.stuff.messages;

import java.util.UUID;

import org.araqnid.stuff.messages.DispatchingMessageHandler.EventHandler;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SomeEventHandler implements EventHandler<SomeEventHandler.Data> {
	private static final Logger LOG = LoggerFactory.getLogger(SomeEventHandler.class);

	@Override
	public void handleEvent(UUID id, DateTime timestamp, SomeEventHandler.Data data) {
		LOG.info("{} test event at {}: {}", id, timestamp, data.name);
	}

	public static class Data {
		public String name;
	}
}

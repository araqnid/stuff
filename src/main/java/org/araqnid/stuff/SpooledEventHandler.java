package org.araqnid.stuff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpooledEventHandler {
	private static final Logger LOG = LoggerFactory.getLogger(SpooledEventHandler.class);

	public void handleEvent(String message) {
		LOG.info("Handle event: {}", message);
	}
}

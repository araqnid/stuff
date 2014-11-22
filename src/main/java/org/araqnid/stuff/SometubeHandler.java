package org.araqnid.stuff;

import java.io.IOException;

import org.araqnid.stuff.messages.BeanstalkProcessor.DeliveryTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;

public class SometubeHandler implements DeliveryTarget {
	private static final Logger LOG = LoggerFactory.getLogger(SometubeHandler.class);
	private final JsonFactory jsonFactory;

	@Inject
	public SometubeHandler(JsonFactory jsonFactory) {
		this.jsonFactory = jsonFactory;
	}

	@Override
	public boolean deliver(byte[] data) {
		Payload payload;
		try {
			payload = parse(data);
		} catch (JsonParseException e) {
			LOG.error("Invalid payload", e);
			return true;
		}
		LOG.info("Processing id:{}", payload.id);
		return true;
	}

	private Payload parse(byte[] data) throws JsonParseException {
		try {
			return jsonFactory.createParser(data).readValueAs(Payload.class);
		} catch (JsonParseException e) {
			throw e;
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class Payload {
		public int id;
	}
}

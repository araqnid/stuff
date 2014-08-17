package org.araqnid.stuff;

import java.io.IOException;

import org.araqnid.stuff.BeanstalkProcessor.DeliveryTarget;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		Payload payload = parse(data);
		LOG.info("Processing id:{}", payload.id);
		return true;
	}

	private Payload parse(byte[] data) {
		try {
			return jsonFactory.createJsonParser(data).readValueAs(Payload.class);
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

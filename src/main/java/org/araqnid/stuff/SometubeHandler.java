package org.araqnid.stuff;

import java.io.IOException;

import org.araqnid.stuff.BeanstalkProcessor.DeliveryTarget;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SometubeHandler implements DeliveryTarget {
	private static final Logger LOG = LoggerFactory.getLogger(SometubeHandler.class);

	@Override
	public boolean deliver(byte[] data) {
		Payload payload = parse(data);
		LOG.info("Processing id:{}", payload.id);
		return true;
	}

	private Payload parse(byte[] data) {
		try {
			JsonFactory jsonFactory = new MappingJsonFactory();
			JsonParser parser = jsonFactory.createJsonParser(data);
			return parser.readValueAs(Payload.class);
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

package org.araqnid.stuff;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.araqnid.stuff.BeanstalkProcessor.DeliveryTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SometubeHandler implements DeliveryTarget {
	private static final Logger LOG = LoggerFactory.getLogger(SometubeHandler.class);

	@Override
	public boolean deliver(byte[] data) {
		Payload payload = parse(data);
		LOG.info("Processing id:{}", payload.id);
		return true;
	}

	private Payload parse(byte[] data) {
		Gson gson = new GsonBuilder().create();
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		InputStreamReader reader = new InputStreamReader(bais, Charset.forName("UTF-8"));
		Payload payload = gson.fromJson(reader, Payload.class);
		return payload;
	}

	public static class Payload {
		public int id;
	}
}

package org.araqnid.stuff;

import java.io.IOException;

import javax.inject.Inject;

import org.araqnid.stuff.activity.ActivityNode;
import org.araqnid.stuff.activity.ActivityScope;
import org.araqnid.stuff.messages.BeanstalkProcessor.DeliveryTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class SometubeHandler implements DeliveryTarget {
	private static final Logger LOG = LoggerFactory.getLogger(SometubeHandler.class);
	private final ObjectReader jsonReader;
	private final ActivityScope activityScope;

	@Inject
	public SometubeHandler(ObjectMapper mapper, ActivityScope activityScope) {
		this.activityScope = activityScope;
		this.jsonReader = mapper.reader(Payload.class);
	}

	@Override
	public boolean deliver(byte[] data) {
		try (ActivityNode.Rec r = activityScope.current().recordActivity("SomeTubeHandler")) {
			Payload payload;
			try {
				payload = parse(data);
			} catch (JsonParseException e) {
				LOG.error("Invalid payload", e);
				return true;
			}
			LOG.info("Processing id:{}", payload.id);
			r.markSuccess();
			return true;
		}
	}

	private Payload parse(byte[] data) throws JsonParseException {
		try {
			return jsonReader.readValue(data);
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

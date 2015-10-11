package org.araqnid.stuff.activity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LogActivityEvents implements ActivityEventSink {
	private static final Logger LOG = LoggerFactory.getLogger(LogActivityEvents.class);
	private final ObjectMapper mapper;

	@Inject
	public LogActivityEvents(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public void activityNodeStart(UUID activityId,
			long nodeId,
			long nodeParentId,
			String type,
			Instant started,
			Object attributes) {
		LOG.info("start {} {} {} {} {} {}", activityId, nodeId, nodeParentId, type, started, toJson(attributes));
	}

	@Override
	public void activityNodeEnd(UUID activityId, long nodeId, boolean success, Duration duration, Object attributes) {
		LOG.info("end   {} {} {} {} {}", activityId, nodeId, success ? "OK" : "BAD", duration, toJson(attributes));
	}

	private String toJson(Object attributes) {
		try {
			return mapper.writeValueAsString(attributes);
		} catch (JsonProcessingException e) {
			return "/* error */ null";
		}
	}

}

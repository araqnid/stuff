package org.araqnid.stuff.activity;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import javax.inject.Inject;

import org.araqnid.stuff.config.ServerIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class LogActivityJsonEvents implements ActivityEventSink {
	private static final Logger LOG = LoggerFactory.getLogger(LogActivityJsonEvents.class);
	private final UUID instanceId;
	private final ObjectWriter startWriter;
	private final ObjectWriter endWriter;

	@Inject
	public LogActivityJsonEvents(ObjectMapper mapper, @ServerIdentity UUID instanceId) {
		this.instanceId = instanceId;
		this.startWriter = mapper.writer().without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
		this.endWriter = mapper.writer().without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
	}

	@Override
	public void activityNodeStart(UUID activityId,
			long nodeId,
			long nodeParentId,
			String type,
			Instant started,
			Object attributes) {
		ActivityNodeStartEvent event = new ActivityNodeStartEvent(instanceId, activityId, nodeId, nodeParentId, type,
				started, attributes);
		try {
			startWriter.writeValue(System.out, event);
			System.out.println("");
		} catch (IOException e) {
			LOG.warn("Failed to write activity start event", e);
		}
	}

	@Override
	public void activityNodeEnd(UUID activityId, long nodeId, boolean success, Duration duration, Object attributes) {
		ActivityNodeEndEvent event = new ActivityNodeEndEvent(instanceId, activityId, nodeId, success, duration,
				attributes);
		try {
			endWriter.writeValue(System.out, event);
			System.out.println("");
		} catch (IOException e) {
			LOG.warn("Failed to write activity end event", e);
		}
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
	@JsonSubTypes({ @JsonSubTypes.Type(ActivityNodeStartEvent.class), @JsonSubTypes.Type(ActivityNodeEndEvent.class) })
	public interface ActivityNodeEvent {
	}

	@JsonTypeName("start")
	public static final class ActivityNodeStartEvent implements ActivityNodeEvent {
		public final UUID instanceId;
		public final UUID activityId;
		public final long nodeId;
		@JsonInclude(Include.NON_EMPTY)
		public final long nodeParentId;
		@JsonProperty("node_type")
		public final String type;
		public final Instant started;
		public final Object attributes;

		public ActivityNodeStartEvent(UUID instanceId,
				UUID activityId,
				long nodeId,
				long nodeParentId,
				String type,
				Instant started,
				Object attributes) {
			this.instanceId = instanceId;
			this.activityId = activityId;
			this.nodeId = nodeId;
			this.nodeParentId = nodeParentId;
			this.type = type;
			this.started = started;
			this.attributes = attributes;
		}
	}

	@JsonTypeName("end")
	public static final class ActivityNodeEndEvent implements ActivityNodeEvent {
		public final UUID instanceId;
		public final UUID activityId;
		public final long nodeId;
		public final boolean success;
		public final Duration duration;
		public final Object attributes;

		public ActivityNodeEndEvent(UUID instanceId,
				UUID activityId,
				long nodeId,
				boolean success,
				Duration duration,
				Object attributes) {
			this.instanceId = instanceId;
			this.activityId = activityId;
			this.nodeId = nodeId;
			this.success = success;
			this.duration = duration;
			this.attributes = attributes;
		}
	}
}

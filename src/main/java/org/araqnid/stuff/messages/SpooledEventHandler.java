package org.araqnid.stuff.messages;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class SpooledEventHandler {
	private static final Logger LOG = LoggerFactory.getLogger(SpooledEventHandler.class);
	private final ObjectMapper objectMapper;
	private final Map<String, EventHandlerRef<?>> eventHandlers;

	@Inject
	public SpooledEventHandler(ObjectMapper objectMapper, TestEventHandler testEventHandler) {
		this(objectMapper, ImmutableMap.<String, EventHandler<?>> of("test", testEventHandler));
	}

	public SpooledEventHandler(ObjectMapper objectMapper, ImmutableMap<String, EventHandler<?>> handlers) {
		this.objectMapper = objectMapper;
		this.eventHandlers = ImmutableMap.copyOf(Maps.transformValues(handlers,
				new Function<EventHandler<?>, EventHandlerRef<?>>() {
					@Override
					public EventHandlerRef<?> apply(EventHandler<?> input) {
						return ref(input);
					}
				}));
	}

	public void handleEvent(String message) {
		Event event = parseJson(message);
		EventHandlerRef<?> handlerRef = eventHandlers.get(event.type);
		if (handlerRef == null) {
			LOG.info("{} {} unknown event type", event.type, event.id);
			return;
		}
		dispatch(handlerRef, event);
	}

	private Event parseJson(String message) {
		try {
			return objectMapper.readValue(message, Event.class);
		} catch (IOException e) {
			throw new IllegalStateException("Invalid message", e);
		}
	}

	private <T> void dispatch(EventHandlerRef<T> ref, Event event) {
		T data;
		try {
			data = ref.clazz.cast(objectMapper.reader(ref.clazz).readValue(event.data));
		} catch (IOException e) {
			LOG.error("{} {} unable to parse data", event.type, event.id, e);
			return;
		}
		ref.handler.handleEvent(event.id, event.timestamp, data);
	}

	private static <T> EventHandlerRef<T> ref(EventHandler<T> handler) {
		Method[] methods = handler.getClass().getMethods();
		for (Method method : methods) {
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (method.getName().equals("handleEvent") && parameterTypes.length == 3 && parameterTypes[0] == UUID.class
					&& parameterTypes[1] == DateTime.class && parameterTypes[2] != Object.class) {
				@SuppressWarnings("unchecked")
				Class<T> eventDataType = (Class<T>) parameterTypes[2];
				return new EventHandlerRef<T>(eventDataType, handler);
			}
		}
		throw new IllegalStateException();
	}

	private static final class EventHandlerRef<T> {
		private final Class<T> clazz;
		private final EventHandler<T> handler;

		public EventHandlerRef(Class<T> clazz, EventHandler<T> handler) {
			this.clazz = clazz;
			this.handler = handler;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Event {
		public UUID id;
		public String type;
		public DateTime timestamp;
		public JsonNode data;
	}

	public interface EventHandler<T> {
		void handleEvent(UUID id, DateTime timestamp, T data);
	}

	public static class TestEventHandler implements EventHandler<TestEventHandler.Data> {
		@Override
		public void handleEvent(UUID id, DateTime timestamp, Data data) {
			LOG.info("{} test event at {}: {}", id, timestamp, data.name);
		}

		public static class Data {
			public String name;
		}
	}
}

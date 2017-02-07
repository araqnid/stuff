package org.araqnid.stuff.messages;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

public class DispatchingMessageHandler implements MessageHandler {
	private static final Logger LOG = LoggerFactory.getLogger(DispatchingMessageHandler.class);
	private final ObjectMapper objectMapper;
	private final Map<String, EventHandlerRef<?>> eventHandlers;

	public DispatchingMessageHandler(ObjectMapper objectMapper, Map<String, EventHandler<?>> handlers) {
		this.objectMapper = objectMapper;
		this.eventHandlers =  handlers.entrySet().stream()
						.map(e -> Maps.immutableEntry(e.getKey(), ref(e.getValue())))
						.collect(toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public void handleMessage(String message) {
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
			data = ref.clazz.cast(objectMapper.readerFor(ref.clazz).readValue(event.data));
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
					&& parameterTypes[1] == Instant.class && parameterTypes[2] != Object.class) {
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
		public Instant timestamp;
		public JsonNode data;
	}

	public interface EventHandler<T> {
		void handleEvent(UUID id, Instant timestamp, T data);
	}
}

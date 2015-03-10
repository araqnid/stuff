package org.araqnid.stuff.config;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

public class JavaTimeModule extends SimpleModule {
	private static final long serialVersionUID = 2014082501L;

	private static final DateTimeFormatter INSTANT_FORMATTER = new DateTimeFormatterBuilder().parseCaseInsensitive()
			.append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral('T').appendValue(HOUR_OF_DAY, 2).appendLiteral(':')
			.appendValue(MINUTE_OF_HOUR, 2).appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2)
			.appendFraction(NANO_OF_SECOND, 3, 9, true).appendLiteral('Z').toFormatter(Locale.UK)
			.withZone(ZoneOffset.UTC);

	private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
			.parseCaseInsensitive().append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral('T')
			.appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2).appendLiteral(':')
			.appendValue(SECOND_OF_MINUTE, 2).appendFraction(NANO_OF_SECOND, 3, 9, true).toFormatter(Locale.UK);

	private static final DateTimeFormatter LOCAL_TIME_FORMATTER = new DateTimeFormatterBuilder().parseCaseInsensitive()
			.appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2).appendLiteral(':')
			.appendValue(SECOND_OF_MINUTE, 2).appendFraction(NANO_OF_SECOND, 3, 9, true).toFormatter(Locale.UK);

	public JavaTimeModule() {
		addSerializer(Instant.class, new InstantSerializer());
		addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
		addSerializer(LocalDate.class, ToStringSerializer.instance);
		addSerializer(LocalTime.class, new LocalTimeSerializer());

		addDeserializer(Instant.class, new InstantDeserializer());
	}

	static class InstantSerializer extends StdScalarSerializer<Instant> {
		private static final long serialVersionUID = 2014082501L;

		protected InstantSerializer() {
			super(Instant.class);
		}

		@Override
		public void serialize(Instant value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
				JsonGenerationException {
			jgen.writeString(INSTANT_FORMATTER.format(value));
		}
	}

	static class LocalDateTimeSerializer extends StdScalarSerializer<LocalDateTime> {
		private static final long serialVersionUID = 2015031001L;

		protected LocalDateTimeSerializer() {
			super(LocalDateTime.class);
		}

		@Override
		public void serialize(LocalDateTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
				JsonGenerationException {
			jgen.writeString(LOCAL_DATE_TIME_FORMATTER.format(value));
		}
	}

	static class LocalTimeSerializer extends StdScalarSerializer<LocalTime> {
		private static final long serialVersionUID = 2015031001L;

		protected LocalTimeSerializer() {
			super(LocalTime.class);
		}

		@Override
		public void serialize(LocalTime value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
				JsonGenerationException {
			jgen.writeString(LOCAL_TIME_FORMATTER.format(value));
		}
	}

	static class InstantDeserializer extends StdScalarDeserializer<Instant> {
		private static final long serialVersionUID = 2014082501L;

		public InstantDeserializer() {
			super(Instant.class);
		}

		@Override
		public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException,
				JsonProcessingException {
			return Instant.parse(p.getValueAsString());
		}
	}
}

package org.araqnid.stuff.config;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class JavaTimeModuleTest {
	private ObjectMapper objectMapper;

	@Before
	public void setUp() throws Exception {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
	}

	@Test
	public void instant_serialized_as_iso_string() throws Exception {
		Instant instant = LocalDateTime.of(2015, Month.FEBRUARY, 28, 13, 40, 39, 273000000).toInstant(
				ZoneOffset.of("Z"));
		assertThat(serialize(instant), equalTo("\"2015-02-28T13:40:39.273Z\""));
	}

	@Test
	public void instant_serialized_with_zero_millis() throws Exception {
		Instant instant = LocalDateTime.of(2015, Month.FEBRUARY, 28, 13, 40, 39, 0).toInstant(ZoneOffset.of("Z"));
		assertThat(serialize(instant), equalTo("\"2015-02-28T13:40:39.000Z\""));
	}

	@Test
	public void instant_deserialized_from_iso_string() throws Exception {
		Instant instant = LocalDateTime.of(2015, Month.FEBRUARY, 28, 13, 40, 39, 273000000).toInstant(
				ZoneOffset.of("Z"));
		assertThat(deserialize("\"2015-02-28T13:40:39.273Z\"", Instant.class), equalTo(instant));
	}

	private String serialize(Object obj) throws JsonProcessingException {
		return objectMapper.writeValueAsString(obj);
	}

	private <T> T deserialize(String str, Class<T> valueType) throws JsonParseException, JsonMappingException,
			IOException {
		return objectMapper.readValue(str, valueType);
	}
}

package org.araqnid.stuff;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class JacksonThings {
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void writer_closes_output_stream() throws Exception {
		OutputStream stream = mock(OutputStream.class);
		mapper.writer().writeValue(stream, new Data(3.14));
		verify(stream).close();
	}

	@Test
	public void writer_does_not_close_output_stream_when_feature_disabled() throws Exception {
		OutputStream stream = mock(OutputStream.class);
		mapper.writer().without(Feature.AUTO_CLOSE_TARGET).writeValue(stream, new Data(3.14));
		verify(stream, never()).close();
	}

	@Test(expected = JsonMappingException.class)
	public void writer_accepts_specific_type_only() throws Exception {
		mapper.writerFor(Data.class).writeValueAsString("foo");
	}

	@Test
	public void writer_writes_datetime_nicely() throws Exception {
		mapper.registerModule(new JodaModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		assertThat(mapper.writeValueAsString(DateTime.parse("2015-03-17T00:04:01Z")),
				equalTo("\"2015-03-17T00:04:01.000Z\""));
	}

	@Test
	public void writer_writes_instant_nicely() throws Exception {
		mapper.registerModule(new JodaModule());
		mapper.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
		assertThat(mapper.writeValueAsString(Instant.parse("2015-03-17T00:04:01Z")),
				equalTo("\"2015-03-17T00:04:01.000Z\""));
	}

	@Test
	public void reader_reads_datetime_nicely_even_when_not_configured_to_write_them() throws Exception {
		mapper.registerModule(new JodaModule());
		assertThat(mapper.readValue("\"2015-03-17T00:04:01.000Z\"", DateTime.class),
				equalTo(DateTime.parse("2015-03-17T00:04:01Z")));
	}

	@Test
	public void reader_converts_value_to_object_by_calling_simple_constructor() throws Exception {
		assertThat(mapper.readValue("3.14", Data.class), equalTo(new Data(3.14)));
	}

	@Test
	public void read_and_write_is_not_necessarily_idempotent() throws Exception {
		assertThat(mapper.writeValueAsString(mapper.readValue("3.14", Data.class)), not(equalTo("3.14")));
		assertThat(mapper.writeValueAsString(mapper.readValue("3.14", Data.class)), equalTo("{\"score\":3.14}"));
	}

	@Test
	public void read_and_write_of_simple_value_can_be_made_to_be_idempotent() throws Exception {
		assertThat(mapper.writeValueAsString(mapper.readValue("3.14", SimpleData.class)), equalTo("3.14"));
	}

	@Test
	public void writer_can_be_reused() throws Exception {
		ObjectWriter dataWriter = mapper.writerFor(Data.class);
		ExecutorService x = Executors.newCachedThreadPool();
		for (int i = 0; i < 100; i++) {
			x.execute(() -> {
				try {
					for (int j = 0; j < 200; j++) {
						assertThat(dataWriter.writeValueAsString(new Data(3.14)), equalTo("{\"score\":3.14}"));
						assertThat(dataWriter.writeValueAsString(new Data(2.18)), equalTo("{\"score\":2.18}"));
					}
				} catch (IOException e) {
					throw Throwables.propagate(e);
				}
			});
		}
		MoreExecutors.shutdownAndAwaitTermination(x, 1, TimeUnit.SECONDS);
	}

	@Test
	public void nan_written_as_a_string() throws Exception {
		assertThat(mapper.writeValueAsString(Double.NaN), equalTo("\"NaN\""));
		assertThat(mapper.writeValueAsString(Float.NaN), equalTo("\"NaN\""));
	}

	@Test
	public void infinity_written_as_a_string() throws Exception {
		assertThat(mapper.writeValueAsString(Double.POSITIVE_INFINITY), equalTo("\"Infinity\""));
		assertThat(mapper.writeValueAsString(Double.NEGATIVE_INFINITY), equalTo("\"-Infinity\""));
		assertThat(mapper.writeValueAsString(Float.POSITIVE_INFINITY), equalTo("\"Infinity\""));
		assertThat(mapper.writeValueAsString(Float.NEGATIVE_INFINITY), equalTo("\"-Infinity\""));
	}

	@Test
	public void maps_are_json_objects() throws Exception {
		assertThat(mapper.writeValueAsString(ImmutableMap.<String, Object> of("a", 1)), equalTo("{\"a\":1}"));
		assertThat(mapper.readValue("{\"a\":1}", Map.class), equalTo(ImmutableMap.<String, Object> of("a", 1)));
	}

	@Test
	public void lists_are_json_arrays() throws Exception {
		assertThat(mapper.writeValueAsString(ImmutableList.of(1, 2)), equalTo("[1,2]"));
		assertThat(mapper.readValue("[1,2]", List.class), equalTo(ImmutableList.of(1, 2)));
		assertThat(mapper.readValue("[1,2]", List.class), not(equalTo(mapper.readValue("[2,1]", List.class))));
	}

	@Test
	public void sets_are_json_arrays() throws Exception {
		assertThat(mapper.writeValueAsString(ImmutableSet.of(1, 2)), equalTo("[1,2]"));
		assertThat(mapper.readValue("[1,2]", Set.class), equalTo(ImmutableSet.of(1, 2)));
		assertThat(mapper.readValue("[1,2]", Set.class), equalTo(mapper.readValue("[2,1]", Set.class)));
	}

	@SuppressWarnings("serial")
	@Test
	public void serializers_can_be_configured_on_demand() throws Exception {
		mapper.registerModule(new SimpleModule() {
			@Override
			public void setupModule(SetupContext context) {
				context.addSerializers(new Serializers.Base() {
					@Override
					public JsonSerializer<?> findSerializer(SerializationConfig config,
							JavaType type,
							BeanDescription beanDesc) {
						final Class<?> raw = type.getRawClass();
						if (raw.getPackage() == JacksonThings.class.getPackage()) return new JsonSerializer<Object>() {
							@Override
							public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers)
									throws IOException, JsonProcessingException {
								gen.writeRaw("/*test*/");
								gen.writeNull();
							}
						};
						return super.findSerializer(config, type, beanDesc);
					}
				});
			}
		});
		assertThat(mapper.writeValueAsString(new Data(3.14)), equalTo("/*test*/null"));
		assertThat(mapper.writeValueAsString(ImmutableMap.of("key", new Data(3.14))), equalTo("{\"key\"/*test*/:null}"));
	}

	@Test
	public void jackson_performs_guice_injection_on_deserialized_objects() throws Exception {
		Injector injector = Guice.createInjector(new ObjectMapperModule(), new AbstractModule() {
			@Override
			protected void configure() {
				bind(JacksonThings.class).toInstance(JacksonThings.this);
			}
		});
		ObjectMapper ourMapper = injector.getInstance(ObjectMapper.class);
		assertThat(ourMapper.readValue("{\"value\":3.14}", DataWithExtra.class).extra, sameInstance(this));
	}

	@Test
	public void jackson_performs_guice_injection_on_deserialized_scalars() throws Exception {
		Injector injector = Guice.createInjector(new ObjectMapperModule(), new AbstractModule() {
			@Override
			protected void configure() {
				bind(JacksonThings.class).toInstance(JacksonThings.this);
			}
		});
		ObjectMapper ourMapper = injector.getInstance(ObjectMapper.class);
		assertThat(ourMapper.readValue("3.14", ScalarWithExtra.class).extra, sameInstance(this));
	}

	@Test
	public void data_object_can_be_converted_to_tree_node() throws Exception {
		assertThat(mapper.convertValue(new Data(3.14), JsonNode.class),
				equalTo(mapper.readValue("{\"score\":3.14}", JsonNode.class)));
	}

	@SuppressWarnings("serial")
	@Test
	public void converting_data_object_to_tree_node_uses_serializers() throws Exception {
		mapper.registerModule(new SimpleModule() {
			{
				addSerializer(Data.class, new StdScalarSerializer<Data>(Data.class) {
					@Override
					public void serialize(Data value, JsonGenerator jgen, SerializerProvider provider)
							throws IOException, JsonGenerationException {
						jgen.writeString("foo");
					}
				});
			}
		});
		assertThat(mapper.writeValueAsString(new Data(3.14)), equalTo("\"foo\""));
		assertThat(mapper.writeValueAsBytes(new Data(3.14)), equalTo("\"foo\"".getBytes(StandardCharsets.UTF_8)));
		assertThat(mapper.convertValue(new Data(3.14), JsonNode.class),
				equalTo(mapper.readValue("\"foo\"", JsonNode.class)));
	}

	public static class Data {
		@JsonProperty("score")
		public final double quux;

		public Data(double quux) {
			this.quux = quux;
		}

		@Override
		public int hashCode() {
			return Objects.hash(quux);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Data && Objects.equals(quux, ((Data) obj).quux);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).addValue(quux).toString();
		}
	}

	public static class SimpleData {
		private final double quux;

		public SimpleData(double quux) {
			this.quux = quux;
		}

		@JsonValue
		public double value() {
			return quux;
		}

		@Override
		public int hashCode() {
			return Objects.hash(quux);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Data && Objects.equals(quux, ((Data) obj).quux);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).addValue(quux).toString();
		}
	}

	public static class DataWithExtra {
		private final double value;
		private final JacksonThings extra;

		public DataWithExtra(@JsonProperty("value") double value, @JacksonInject JacksonThings extra) {
			this.value = value;
			this.extra = extra;
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof DataWithExtra && Objects.equals(value, ((DataWithExtra) obj).value);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("value", value).add("extra", extra).toString();
		}
	}

	public static class ScalarWithExtra {
		private final double value;
		private final JacksonThings extra;

		public ScalarWithExtra(double value, @JacksonInject JacksonThings extra) {
			this.value = value;
			this.extra = extra;
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof ScalarWithExtra && Objects.equals(value, ((ScalarWithExtra) obj).value);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("value", value).add("extra", extra).toString();
		}
	}
}

package org.araqnid.stuff;

import static org.araqnid.stuff.JsonEquivalenceMatchers.equivalentJsonNode;
import static org.araqnid.stuff.JsonEquivalenceMatchers.equivalentTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
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
import org.json.JSONObject;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonParser;
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
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

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
				equivalentTo("\"2015-03-17T00:04:01.000Z\""));
	}

	@Test
	public void writer_writes_instant_nicely() throws Exception {
		mapper.registerModule(new JodaModule());
		mapper.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
		assertThat(mapper.writeValueAsString(Instant.parse("2015-03-17T00:04:01Z")),
				equivalentTo("\"2015-03-17T00:04:01.000Z\""));
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
						assertThat(dataWriter.writeValueAsString(new Data(3.14)), equivalentTo("{\"score\":3.14}"));
						assertThat(dataWriter.writeValueAsString(new Data(2.18)), equivalentTo("{\"score\":2.18}"));
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
		assertThat(mapper.writeValueAsString(Double.NaN), equivalentTo("\"NaN\""));
		assertThat(mapper.writeValueAsString(Float.NaN), equivalentTo("\"NaN\""));
	}

	@Test
	public void infinity_written_as_a_string() throws Exception {
		assertThat(mapper.writeValueAsString(Double.POSITIVE_INFINITY), equivalentTo("\"Infinity\""));
		assertThat(mapper.writeValueAsString(Double.NEGATIVE_INFINITY), equivalentTo("\"-Infinity\""));
		assertThat(mapper.writeValueAsString(Float.POSITIVE_INFINITY), equivalentTo("\"Infinity\""));
		assertThat(mapper.writeValueAsString(Float.NEGATIVE_INFINITY), equivalentTo("\"-Infinity\""));
	}

	@Test
	public void maps_are_json_objects() throws Exception {
		assertThat(mapper.writeValueAsString(ImmutableMap.<String, Object> of("a", 1)), equivalentTo("{\"a\":1}"));
		assertThat(mapper.readValue("{\"a\":1}", Map.class), equalTo(ImmutableMap.<String, Object> of("a", 1)));
	}

	@Test
	public void lists_are_json_arrays() throws Exception {
		assertThat(mapper.writeValueAsString(ImmutableList.of(1, 2)), equivalentTo("[1,2]"));
		assertThat(mapper.readValue("[1,2]", List.class), equalTo(ImmutableList.of(1, 2)));
		assertThat(mapper.readValue("[1,2]", List.class), not(equalTo(mapper.readValue("[2,1]", List.class))));
	}

	@Test
	public void sets_are_json_arrays() throws Exception {
		assertThat(mapper.writeValueAsString(ImmutableSet.of(1, 2)), equivalentTo("[1,2]"));
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
		assertThat(mapper.valueToTree(new Data(3.14)), equalTo(mapper.readTree("{\"score\":3.14}")));
		assertThat(mapper.convertValue(new Data(3.14), JsonNode.class), equalTo(mapper.readTree("{\"score\":3.14}")));
	}

	@Test
	public void tree_node_objects_can_be_used_for_equality_checks() throws Exception {
		assertThat(mapper.readTree("{\"a\":1,\"b\":2}"), equalTo(mapper.readTree("{ \"b\" : 2, \"a\": 1 }")));
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
		assertThat(mapper.valueToTree(new Data(3.14)), equalTo(mapper.readTree("\"foo\"")));
	}

	@Test
	public void immutable_sets_can_be_read() throws Exception {
		mapper.registerModule(new GuavaModule());
		assertThat(mapper.readValue("[1, 2, 3]", ImmutableSet.class), equalTo(ImmutableSet.of(1, 2, 3)));
	}

	@Test
	public void object_reader_can_be_built_specifying_parameterised_type() throws Exception {
		mapper.registerModule(new GuavaModule());
		JavaType typeToken = mapper.getTypeFactory().constructParametrizedType(ImmutableSet.class, ImmutableSet.class,
				Long.class);
		assertThat(mapper.reader(typeToken).readValue("[1, 2, 3]"), equalTo(ImmutableSet.of(1L, 2L, 3L)));
	}

	@Test
	public void multimaps_are_objects_with_array_valued_properties() throws Exception {
		mapper.registerModule(new GuavaModule());
		assertThat(mapper.valueToTree(ImmutableMultimap.of("a", 1, "a", 2, "b", 3)),
				equivalentJsonNode("{\"a\":[1,2],\"b\":[3]}"));
	}

	@Test
	public void cross_library_with_json_org() throws Exception {
		mapper.registerModule(new JsonOrgModule());
		assertThat(mapper.valueToTree(new JSONObject().put("a", 1)), equivalentJsonNode("{\"a\":1}"));
	}

	@Test
	public void immutable_object_can_be_read_using_constructor_with_annotated_property_names() throws Exception {
		mapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
		mapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
		ValueClassWithAnnotatedCreatorConstructor value = mapper.readValue(
				"{name:'the name',description:'the description',price:42.24}",
				ValueClassWithAnnotatedCreatorConstructor.class);
		assertThat(value.name, equalTo("the name"));
		assertThat(value.description, equalTo("the description"));
		assertThat(value.price, closeTo(new BigDecimal(42.24), new BigDecimal(0.0001)));
		assertThat(mapper.writeValueAsString(value), equivalentTo("{name:'the name',description:'the description',price:42.24}"));
	}

	@Test
	public void immutable_object_can_be_read_using_delegate_creator() throws Exception {
		mapper.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
		mapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
		ValueClassWithDelegateCreatorConstructor value = mapper.readValue(
				"{name:'the name',description:'the description',price:42.24}",
				ValueClassWithDelegateCreatorConstructor.class);
		assertThat(value.name, equalTo("the name"));
		assertThat(value.description, equalTo("the description"));
		assertThat(value.price, closeTo(new BigDecimal(42.24), new BigDecimal(0.0001)));
		assertThat(mapper.writeValueAsString(value), equivalentTo("{name:'the name',description:'the description',price:42.24}"));
	}

	@Test
	public void numbers_can_be_serialized_as_strings() throws Exception {
		mapper.enable(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS);
		assertThat(mapper.writeValueAsString(ImmutableMap.<String, Number> builder()
				.put("int", 1)
				.put("short", (short) 2)
				.put("long", 3l)
				.put("double", 4.12)
				.put("float", 5.5f)
				.put("bigdecimal", new BigDecimal(6.78))
				.build()),
				equivalentTo("{ int: '1', short: '2', long: '3', double: '4.12', float: '5.5', bigdecimal: '"
						+ new BigDecimal(6.78) + "' }"));
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

	public static class ValueClassWithAnnotatedCreatorConstructor {
		public final String name;
		public final String description;
		public final BigDecimal price;

		@JsonCreator
		public ValueClassWithAnnotatedCreatorConstructor(@JsonProperty("name") String name,
				@JsonProperty("description") String description,
				@JsonProperty("price") BigDecimal price) {
			this.name = name;
			this.description = description;
			this.price = price;
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, description, price);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof ValueClassWithAnnotatedCreatorConstructor
					&& Objects.equals(name, ((ValueClassWithAnnotatedCreatorConstructor) obj).name)
			&& Objects.equals(description, ((ValueClassWithAnnotatedCreatorConstructor) obj).description)
			&& Objects.equals(price, ((ValueClassWithAnnotatedCreatorConstructor) obj).price);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("name", name).add("description", description)
					.add("price", price).toString();
		}
	}

	public static class ValueClassWithDelegateCreatorConstructor {
		public final String name;
		public final String description;
		public final BigDecimal price;

		@JsonCreator
		public ValueClassWithDelegateCreatorConstructor(Template template) {
			this.name = template.name;
			this.description = template.description;
			this.price = template.price;
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, description, price);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof ValueClassWithDelegateCreatorConstructor
					&& Objects.equals(name, ((ValueClassWithDelegateCreatorConstructor) obj).name)
			&& Objects.equals(description, ((ValueClassWithDelegateCreatorConstructor) obj).description)
			&& Objects.equals(price, ((ValueClassWithDelegateCreatorConstructor) obj).price);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("name", name).add("description", description)
					.add("price", price).toString();
		}

		public static class Template {
			public String name;
			public String description;
			public BigDecimal price;
		}
	}
}

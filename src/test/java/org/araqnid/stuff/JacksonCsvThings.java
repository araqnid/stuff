package org.araqnid.stuff;

import java.util.Map;
import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

public class JacksonCsvThings {
	@Test
	public void parses_csv_row_to_single_map_using_specified_schema() throws Exception {
		String csv = "1,Red\n2,Green\n3,Blue";
		ObjectMapper mapper = new CsvMapper();
		CsvSchema schema = CsvSchema.builder().addColumn("id").addColumn("name").build();
		ObjectReader reader = mapper.reader(Map.class).with(schema);
		assertThat(reader.readValue(csv), sameMapAs(ImmutableMap.of("id", "1", "name", "Red")));
	}

	@Test
	public void parses_csv_row_to_single_map_using_header_line() throws Exception {
		String csv = "id,name\n1,Red\n2,Green\n3,Blue";
		ObjectMapper mapper = new CsvMapper();
		CsvSchema schema = CsvSchema.emptySchema().withHeader();
		ObjectReader reader = mapper.reader(Map.class).with(schema);
		assertThat(reader.readValue(csv), sameMapAs(ImmutableMap.of("id", "1", "name", "Red")));
	}

	@Test
	public void parses_csv_row_to_object_using_annotations_for_schema() throws Exception {
		String csv = "1,Red\n2,Green\n3,Blue";
		CsvMapper mapper = new CsvMapper();
		CsvSchema schema = mapper.schemaFor(ColourData.class);
		ObjectReader reader = mapper.reader(ColourData.class).with(schema);
		assertThat(reader.readValue(csv), equalTo(new ColourData(1, "Red")));
	}

	@Test
	public void parses_csv_row_to_object_without_parameter_annotations() throws Exception {
		String csv = "1,Red\n2,Green\n3,Blue";
		CsvMapper mapper = new CsvMapper();
		mapper.registerModule(new ParameterNamesModule());
		CsvSchema schema = mapper.schemaFor(ColourDataSimpleConstructor.class);
		ObjectReader reader = mapper.reader(ColourDataSimpleConstructor.class).with(schema);
		assertThat(reader.readValue(csv), equalTo(new ColourDataSimpleConstructor(1, "Red")));
	}

	@Test
	public void parses_csv_row_to_object_skipping_header_line() throws Exception {
		String csv = "id,name\n1,Red\n2,Green\n3,Blue";
		CsvMapper mapper = new CsvMapper();
		CsvSchema schema = mapper.schemaFor(ColourData.class).withHeader();
		ObjectReader reader = mapper.reader(ColourData.class).with(schema);
		assertThat(reader.readValue(csv), equalTo(new ColourData(1, "Red")));
	}

	@Test
	public void parses_csv_to_object_iterator() throws Exception {
		String csv = "id,name\n1,Red\n2,Green\n3,Blue";
		CsvMapper mapper = new CsvMapper();
		CsvSchema schema = mapper.schemaFor(ColourData.class).withHeader();
		ObjectReader reader = mapper.reader(ColourData.class).with(schema);
		try (MappingIterator<ColourData> iter = reader.readValues(csv)) {
			assertThat(iter.nextValue(), equalTo(new ColourData(1, "Red")));
			assertThat(iter.nextValue(), equalTo(new ColourData(2, "Green")));
			assertThat(iter.nextValue(), equalTo(new ColourData(3, "Blue")));
		}
	}

	@Test
	public void parses_csv_to_object_list() throws Exception {
		String csv = "id,name\n1,Red\n2,Green\n3,Blue";
		CsvMapper mapper = new CsvMapper();
		CsvSchema schema = mapper.schemaFor(ColourData.class).withHeader();
		ObjectReader reader = mapper.reader(ColourData.class).with(schema);
		try (MappingIterator<ColourData> iter = reader.readValues(csv)) {
			assertThat(iter.readAll(),
					contains(new ColourData(1, "Red"), new ColourData(2, "Green"), new ColourData(3, "Blue")));
		}
	}

	@Test
	public void writes_csv_row_from_map_using_specified_schema() throws Exception {
		CsvMapper mapper = new CsvMapper();
		CsvSchema schema = CsvSchema.builder().addColumn("id").addColumn("name").build();
		ObjectWriter writer = mapper.writer().with(schema);
		String csv = writer.writeValueAsString(ImmutableMap.of("id", "1", "name", "Red"));
		assertThat(csv, equalTo("1,Red\n"));
	}

	@Test
	public void writes_csv_row_from_object() throws Exception {
		CsvMapper mapper = new CsvMapper();
		CsvSchema schema = mapper.schemaFor(ColourData.class);
		ObjectWriter writer = mapper.writer().with(schema);
		String csv = writer.writeValueAsString(new ColourData(1, "Red"));
		assertThat(csv, equalTo("1,Red\n"));
	}

	@Test
	public void writes_csv_row_and_header_from_object() throws Exception {
		CsvMapper mapper = new CsvMapper();
		CsvSchema schema = mapper.schemaFor(ColourData.class).withHeader();
		ObjectWriter writer = mapper.writer().with(schema);
		String csv = writer.writeValueAsString(new ColourData(1, "Red"));
		assertThat(csv, equalTo("id,name\n1,Red\n"));
	}

	@Test
	public void writes_csv_from_object_list() throws Exception {
		CsvMapper mapper = new CsvMapper();
		CsvSchema schema = mapper.schemaFor(ColourData.class);
		ObjectWriter writer = mapper.writer().with(schema);
		String csv = writer.writeValueAsString(
				ImmutableList.of(new ColourData(1, "Red"), new ColourData(2, "Green"), new ColourData(3, "Blue")));
		assertThat(csv, equalTo("1,Red\n2,Green\n3,Blue\n"));
	}

	@JsonPropertyOrder({ "id", "name" })
	public static final class ColourData {
		public final int id;
		public final String name;

		public ColourData(@JsonProperty("id") int id, @JsonProperty("name") String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, name);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof ColourData)) return false;
			ColourData other = (ColourData) obj;
			return other.id == id && Objects.equals(other.name, name);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("id", id).add("name", name).toString();
		}
	}

	@JsonPropertyOrder({ "id", "name" })
	public static final class ColourDataSimpleConstructor {
		public final int id;
		public final String name;

		@JsonCreator
		public ColourDataSimpleConstructor(int id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, name);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof ColourDataSimpleConstructor)) return false;
			ColourDataSimpleConstructor other = (ColourDataSimpleConstructor) obj;
			return other.id == id && Objects.equals(other.name, name);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("id", id).add("name", name).toString();
		}
	}

	private static <K, V> Matcher<Map<K, V>> sameMapAs(Map<K, V> expected) {
		return new TypeSafeDiagnosingMatcher<Map<K, V>>() {
			@Override
			protected boolean matchesSafely(Map<K, V> item, Description mismatchDescription) {
				Map<K, V> x = ImmutableMap.copyOf(item);
				mismatchDescription.appendText("map was ").appendValue(x);
				return expected.equals(x);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("map ").appendValue(ImmutableMap.copyOf(expected));
			}
		};
	}
}

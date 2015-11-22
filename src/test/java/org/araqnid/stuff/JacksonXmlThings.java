package org.araqnid.stuff;

import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class JacksonXmlThings {
	@Test
	public void serializes_datum_to_xml() throws Exception {
		SimpleDatum datum = new SimpleDatum();
		ObjectMapper mapper = new XmlMapper();
		assertThat(mapper.writeValueAsString(datum), equalTo("<SimpleDatum><id/><name/></SimpleDatum>"));
	}

	@Test
	public void datum_values_populated_as_elements() throws Exception {
		SimpleDatum datum = new SimpleDatum();
		datum.id = "123";
		datum.name = "foo";
		ObjectMapper mapper = new XmlMapper();
		assertThat(mapper.writeValueAsString(datum),
				equalTo("<SimpleDatum><id>123</id><name>foo</name></SimpleDatum>"));
	}

	@Test
	public void annotation_allows_specifying_property_as_attribute() throws Exception {
		SimpleDatumWithAttributeId datum = new SimpleDatumWithAttributeId();
		datum.id = "123";
		datum.name = "foo";
		ObjectMapper mapper = new XmlMapper();
		assertThat(mapper.writeValueAsString(datum),
				equalTo("<SimpleDatumWithAttributeId id=\"123\"><name>foo</name></SimpleDatumWithAttributeId>"));
	}

	@Test
	public void writer_allows_root_element_name_to_be_specified() throws Exception {
		SimpleDatum datum = new SimpleDatum();
		ObjectMapper mapper = new XmlMapper();
		assertThat(mapper.writerFor(SimpleDatum.class).withRootName("datum").writeValueAsString(datum),
				equalTo("<datum><id/><name/></datum>"));
	}

	@Test
	public void serializes_datum_list_to_xml() throws Exception {
		SimpleDatum datum1 = new SimpleDatum();
		SimpleDatum datum2 = new SimpleDatum();
		SimpleData data = new SimpleData();
		data.data = ImmutableList.of(datum1, datum2);
		ObjectMapper mapper = new XmlMapper();
		assertThat(mapper.writeValueAsString(data), equalTo(Joiner.on("").join("<SimpleData><data>",
				"<data><id/><name/></data>", "<data><id/><name/></data>", "</data></SimpleData>")));
	}

	@Test
	public void annotation_can_disable_list_wrapping() throws Exception {
		SimpleDatum datum1 = new SimpleDatum();
		SimpleDatum datum2 = new SimpleDatum();
		SimpleUnwrappedData data = new SimpleUnwrappedData();
		data.data = ImmutableList.of(datum1, datum2);
		ObjectMapper mapper = new XmlMapper();
		assertThat(mapper.writeValueAsString(data), equalTo(Joiner.on("").join("<SimpleUnwrappedData>",
				"<data><id/><name/></data>", "<data><id/><name/></data>", "</SimpleUnwrappedData>")));
	}

	public static final class SimpleDatum {
		public String id;
		public String name;
	}

	public static final class SimpleDatumWithAttributeId {
		@JacksonXmlProperty(isAttribute = true)
		public String id;
		public String name;
	}

	public static final class SimpleData {
		public List<SimpleDatum> data;
	}

	public static final class SimpleUnwrappedData {
		@JacksonXmlElementWrapper(useWrapping = false)
		public List<SimpleDatum> data;
	}
}

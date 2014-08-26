package org.araqnid.stuff.test.integration;

import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.araqnid.stuff.config.ResteasyModule.JacksonContextResolver;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.AbstractModule;
import com.google.inject.Module;

import static org.araqnid.stuff.test.integration.HttpClientMatchers.ok;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithJsonContent;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonArray;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonNull;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonObject;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonString;
import static org.araqnid.stuff.testutil.RandomData.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.is;

public class JsonMarshallingTest extends IntegrationTest {
	@Override
	protected Module serverConfiguration() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(GuiceResteasyBootstrapServletContextListener.class).toInstance(new GuiceResteasyBootstrapServletContextListener() {
					@Override
					protected List<? extends Module> getModules(ServletContext context) {
						return ImmutableList.of(new AbstractModule() {
							@Override
							protected void configure() {
								bind(TestResource.class);
								bind(JacksonContextResolver.class);
							}
						});
					}
				});
			}
		};
	}

	@Test
	public void joda_datetime_is_marshalled_as_a_nice_string() throws Exception {
		try (CloseableHttpResponse response = doGet("/_api/test/datetime")) {
			assertThat(response,
					is(both(ok()).and(responseWithJsonContent(jsonString(like("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d(\\.\\d\\d\\d)?Z"))))));
		}
	}

	@Test
	public void joda_localdate_is_marshalled_as_a_nice_string() throws Exception {
		try (CloseableHttpResponse response = doGet("/_api/test/localdate")) {
			assertThat(response, is(both(ok()).and(responseWithJsonContent(jsonString(like("\\d\\d\\d\\d-\\d\\d-\\d\\d"))))));
		}
	}

	@Test
	public void joda_localtime_is_marshalled_as_a_nice_string() throws Exception {
		try (CloseableHttpResponse response = doGet("/_api/test/localtime")) {
			assertThat(response, is(both(ok()).and(responseWithJsonContent(jsonString(like("\\d\\d:\\d\\d:\\d\\d(\\.\\d\\d\\d)?"))))));
		}
	}

	@Test
	public void joda_localdatetime_is_marshalled_as_a_nice_string() throws Exception {
		try (CloseableHttpResponse response = doGet("/_api/test/localdatetime")) {
			assertThat(response,
					is(both(ok()).and(responseWithJsonContent(jsonString(like("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d(\\.\\d\\d\\d)?"))))));
		}
	}

	@Test
	public void present_optional_is_marshalled_directly() throws Exception {
		String value = randomString();
		try (CloseableHttpResponse response = doGet("/_api/test/optional/present/" + value)) {
			assertThat(response, is(both(ok()).and(responseWithJsonContent(jsonString(value)))));
		}
	}

	@Test
	public void absent_optional_is_marshalled_as_null() throws Exception {
		try (CloseableHttpResponse response = doGet("/_api/test/optional/absent")) {
			assertThat(response, is(both(ok()).and(responseWithJsonContent(jsonNull()))));
		}
	}

	@Test
	public void present_optional_property_is_marshalled_directly() throws Exception {
		String value = randomString();
		try (CloseableHttpResponse response = doGet("/_api/test/object-with-optional/present/" + value)) {
			assertThat(response, is(both(ok()).and(responseWithJsonContent(jsonObject().withProperty("value", jsonString(value))))));
		}
	}

	@Test
	public void absent_optional_property_is_marshalled_as_null() throws Exception {
		try (CloseableHttpResponse response = doGet("/_api/test/object-with-optional/absent")) {
			assertThat(response, is(both(ok()).and(responseWithJsonContent(jsonObject().withProperty("value", jsonNull())))));
		}
	}

	@Test
	public void multimap_is_returned_as_object_with_array_properties() throws Exception {
		String key1 = randomString();
		String key2 = randomString();
		String value1 = randomString();
		String value2 = randomString();
		try (CloseableHttpResponse response = doGet("/_api/test/multimap/" + Joiner.on('/').join(key1, value1, value2))) {
			assertThat(
					response,
					is(both(ok()).and(
							responseWithJsonContent(jsonObject().withProperty(key1, jsonArray().of(jsonString(value1), jsonString(value2)))))));
		}
		try (CloseableHttpResponse response = doGet("/_api/test/multimap/" + Joiner.on('/').join(key1, value1, key2, value2))) {
			assertThat(
					response,
					is(both(ok()).and(
							responseWithJsonContent(jsonObject().withProperty(key1, jsonArray().of(jsonString(value1))).withProperty(key2,
									jsonArray().of(jsonString(value2)))))));
		}
	}

	@Factory
	private static Matcher<String> like(final String patternSource) {
		return new TypeSafeDiagnosingMatcher<String>() {
			private final Pattern pattern = Pattern.compile(patternSource);

			@Override
			protected boolean matchesSafely(String item, Description mismatchDescription) {
				if (!pattern.matcher(item).matches()) {
					mismatchDescription.appendText("does not match: ").appendValue(item);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("matches ").appendValue(pattern.pattern());
			}
		};
	}

	@Path("test")
	@Produces("application/json")
	public static class TestResource {
		@GET
		@Path("datetime")
		public DateTime dateTime() {
			return new DateTime();
		}

		@GET
		@Path("localdate")
		public LocalDate localDate() {
			return new LocalDate();
		}

		@GET
		@Path("localtime")
		public LocalTime localTime() {
			return new LocalTime();
		}

		@GET
		@Path("localdatetime")
		public LocalDateTime localDateTime() {
			return new LocalDateTime();
		}

		@GET
		@Path("optional/present/{value}")
		public Optional<String> optional(@PathParam("value") String value) {
			return Optional.of(value);
		}

		@GET
		@Path("optional/absent")
		public Optional<String> optional() {
			return Optional.absent();
		}

		@GET
		@Path("object-with-optional/present/{value}")
		public ValueWithOptional valueWithOptional(@PathParam("value") String value) {
			return new ValueWithOptional(value);
		}

		@GET
		@Path("object-with-optional/absent")
		public ValueWithOptional valueWithOptional() {
			return new ValueWithOptional();
		}

		@GET
		@Path("multimap/{key}/{value1}/{value2}")
		public Multimap<String, String> multimapWithOneKey(@PathParam("key") String key, @PathParam("value1") String value1,
				@PathParam("value2") String value2) {
			return ImmutableMultimap.of(key, value1, key, value2);
		}

		@GET
		@Path("multimap/{key1}/{value1}/{key2}/{value2}")
		public Multimap<String, String> multimapWithTwoKeys(@PathParam("key1") String key1, @PathParam("key2") String key2,
				@PathParam("value1") String value1, @PathParam("value2") String value2) {
			return ImmutableMultimap.of(key1, value1, key2, value2);
		}
	}

	public static class ValueWithOptional {
		public final Optional<String> value;

		public ValueWithOptional() {
			this.value = Optional.absent();
		}

		public ValueWithOptional(String value) {
			this.value = Optional.of(value);
		}
	}
}
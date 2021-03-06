package org.araqnid.stuff.test.integration;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.araqnid.stuff.config.ResteasyModule;
import org.araqnid.stuff.test.SupplierClock;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static java.time.ZoneOffset.UTC;
import static org.araqnid.stuff.matchers.XmlMatchers.containsXpath;
import static org.araqnid.stuff.matchers.XmlMatchers.textAtXpath;
import static org.araqnid.stuff.matchers.XmlMatchers.textAtXpathIs;
import static org.araqnid.stuff.test.RandomData.randomString;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.ok;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithXmlContent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class XMLMarshallingTest {
	@Rule
	public final ServerRunner server = new ServerRunner(Collections::emptyMap, new AbstractModule() {
		@Override
		protected void configure() {
			bind(Clock.class).toInstance(new SupplierClock(() -> instant, UTC));
			bind(GuiceResteasyBootstrapServletContextListener.class).toInstance(new ResteasyBindings(new AbstractModule() {
				@Override
				protected void configure() {
					bind(TestResource.class);
				}

				@Provides
				public JacksonXMLProvider jacksonXml() {
					return new JacksonXMLProvider(ResteasyModule.XML_OBJECT_MAPPER);
				}
			}));
		}
	});

	@Parameters(name = "@{0}")
	public static Instant[] parameters() {
		ImmutableList<Instant> instants = ImmutableList.of(Instant.parse("2015-04-03T13:29:33.123456789Z"),
				Instant.parse("2015-04-03T13:29:33.123456000Z"), Instant.parse("2015-04-03T13:29:33.123000000Z"),
				Instant.parse("2015-04-03T13:29:33.000000000Z"), Instant.parse("2015-04-03T13:29:00.000000000Z"),
				Instant.parse("2015-04-03T13:00:00.000000000Z"));
		return instants.toArray(new Instant[instants.size()]);
	}

	@Parameter
	public Instant instant;

	@Test
	public void jdk_instant_is_marshalled_as_a_nice_string() throws Exception {
		assertThat(server.doGet("/_api/test/jdk/instant"), is(both(ok()).and(responseWithXmlContent(
				textAtXpath("/Instant", like("2015-04-03T13:\\d\\d:\\d\\d(\\.\\d\\d\\d(\\d\\d\\d(\\d\\d\\d)?)?)?Z"))))));
	}

	@Test
	public void jdk_localdate_is_marshalled_as_a_nice_string() throws Exception {
		assertThat(server.doGet("/_api/test/jdk/localdate"),
				is(both(ok()).and(responseWithXmlContent(textAtXpath("/LocalDate", like("\\d\\d\\d\\d-\\d\\d-\\d\\d"))))));
	}

	@Test
	public void jdk_localtime_is_marshalled_as_a_nice_string() throws Exception {
		assertThat(server.doGet("/_api/test/jdk/localtime"), is(both(ok()).and(responseWithXmlContent(
				textAtXpath("/LocalTime", like("\\d\\d:\\d\\d(:\\d\\d(\\.\\d\\d\\d(\\d\\d\\d(\\d\\d\\d)?)?)?)?"))))));
	}

	@Test
	public void jdk_localdatetime_is_marshalled_as_a_nice_string() throws Exception {
		assertThat(server.doGet("/_api/test/jdk/localdatetime"), is(both(ok()).and(responseWithXmlContent(
				textAtXpath("/LocalDateTime", like(
				"2015-04-03T13:\\d\\d(:\\d\\d(\\.\\d\\d\\d(\\d\\d\\d(\\d\\d\\d(\\d\\d\\d(\\d\\d\\d)?)?)?)?)?)?"))))));
	}

	@Test
	public void present_optional_property_is_marshalled_directly() throws Exception {
		String value = randomString();
		try (CloseableHttpResponse response = server.doGet("/_api/test/guava/object-with-optional/present/" + value)) {
			assertThat(response,
					is(both(ok()).and(responseWithXmlContent(textAtXpathIs("/ValueWithOptional/value", value)))));
		}
	}

	@Test
	public void absent_optional_property_is_marshalled_as_null() throws Exception {
		try (CloseableHttpResponse response = server.doGet("/_api/test/guava/object-with-optional/absent")) {
			assertThat(response,
					is(both(ok()).and(responseWithXmlContent(textAtXpathIs("/ValueWithOptional/value", "")))));
		}
	}

	@Test
	public void multimap_is_returned_as_elements_with_keys_as_names() throws Exception {
		String key1 = randomString();
		String key2 = randomString();
		String value1 = randomString();
		String value2 = randomString();
		try (CloseableHttpResponse response = server.doGet("/_api/test/guava/object-with-multimap/" + Joiner.on('/').join(key1, value1, value2))) {
			assertThat(response, is(both(ok()).and(responseWithXmlContent(
					allOf(containsXpath(String.format("/ValueWithMultimap/multimap/multimap/%s[text() = '%s']", key1, value1)),
							containsXpath(String.format("/ValueWithMultimap/multimap/multimap/%s[text() = '%s']", key1, value2)))))));
		}
		try (CloseableHttpResponse response = server.doGet("/_api/test/guava/object-with-multimap/" + Joiner.on('/').join(key1, value1, key2, value2))) {
			assertThat(response, is(both(ok()).and(responseWithXmlContent(
					allOf(textAtXpathIs("/ValueWithMultimap/multimap/multimap/" + key1, value1),
							textAtXpathIs("/ValueWithMultimap/multimap/multimap/" + key2, value2))))));
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

	@Path("_api/test")
	@Produces("application/xml")
	public static class TestResource {
		private final Clock clock;

		@Inject
		public TestResource(Clock clock) {
			this.clock = clock;
		}

		@GET
		@Path("jdk/instant")
		public java.time.Instant jdkInstant() {
			return java.time.Instant.now(clock);
		}

		@GET
		@Path("jdk/localdate")
		public java.time.LocalDate jdkLocalDate() {
			return java.time.LocalDate.now(clock);
		}

		@GET
		@Path("jdk/localtime")
		public java.time.LocalTime jdkLocalTime() {
			return java.time.LocalTime.now(clock);
		}

		@GET
		@Path("jdk/localdatetime")
		public java.time.LocalDateTime jdkLocalDateTime() {
			return java.time.LocalDateTime.now(clock);
		}

		@GET
		@Path("guava/object-with-optional/present/{value}")
		public ValueWithOptional valueWithOptional(@PathParam("value") String value) {
			return new ValueWithOptional(value);
		}

		@GET
		@Path("guava/object-with-optional/absent")
		public ValueWithOptional valueWithOptional() {
			return new ValueWithOptional();
		}

		@GET
		@Path("guava/object-with-multimap/{key}/{value1}/{value2}")
		public ValueWithMultimap multimapWithOneKey(@PathParam("key") String key,
				@PathParam("value1") String value1,
				@PathParam("value2") String value2) {
			return new ValueWithMultimap<>(ImmutableMultimap.of(key, value1, key, value2));
		}

		@GET
		@Path("guava/object-with-multimap/{key1}/{value1}/{key2}/{value2}")
		public ValueWithMultimap multimapWithTwoKeys(@PathParam("key1") String key1,
				@PathParam("key2") String key2,
				@PathParam("value1") String value1,
				@PathParam("value2") String value2) {
			return new ValueWithMultimap<>(ImmutableMultimap.of(key1, value1, key2, value2));
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

	public static class ValueWithMultimap<K, V> {
		public final Multimap<K, V> multimap;

		public ValueWithMultimap(Multimap<K, V> multimap) {
			this.multimap = multimap;
		}
	}
}

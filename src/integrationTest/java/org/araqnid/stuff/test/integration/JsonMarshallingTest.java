package org.araqnid.stuff.test.integration;

import static org.araqnid.stuff.test.integration.HttpClientMatchers.ok;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithJsonContent;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class JsonMarshallingTest {
	private CloseableHttpClient httpClient;
	private ServerRunner server = new ServerRunner(serverConfiguration());

	@Before
	public void startServer() throws Exception {
		server.start();
	}

	@After
	public void stopServer() throws Exception {
		server.stop();
	}

	@Before
	public void setUp() throws Exception {
		httpClient = HttpClientBuilder.create().build();
	}

	@After
	public void tearDown() throws Exception {
		if (httpClient != null) httpClient.close();
	}

	private static Module serverConfiguration() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(GuiceResteasyBootstrapServletContextListener.class).toInstance(
						new GuiceResteasyBootstrapServletContextListener() {
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

	private CloseableHttpResponse doGet(String path) throws URISyntaxException, IOException {
		Preconditions.checkArgument(path.startsWith("/"));
		return httpClient.execute(new HttpGet(server.uri("/_api" + path)));
	}

	@Test
	public void joda_datetime_is_marshalled_as_a_nice_string() throws Exception {
		assertThat(
				doGet("/test/datetime"),
				is(both(ok())
						.and(responseWithJsonContent(jsonString(like("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d(\\.\\d\\d\\d)?Z"))))));
	}

	@Test
	public void joda_localdate_is_marshalled_as_a_nice_string() throws Exception {
		assertThat(doGet("/test/localdate"),
				is(both(ok()).and(responseWithJsonContent(jsonString(like("\\d\\d\\d\\d-\\d\\d-\\d\\d"))))));
	}

	@Test
	public void joda_localtime_is_marshalled_as_a_nice_string() throws Exception {
		assertThat(doGet("/test/localtime"),
				is(both(ok()).and(responseWithJsonContent(jsonString(like("\\d\\d:\\d\\d:\\d\\d(\\.\\d\\d\\d)?"))))));
	}

	@Test
	public void joda_localdatetime_is_marshalled_as_a_nice_string() throws Exception {
		assertThat(
				doGet("/test/localdatetime"),
				is(both(ok())
						.and(responseWithJsonContent(jsonString(like("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d(\\.\\d\\d\\d)?"))))));
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
	}
}

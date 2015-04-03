package org.araqnid.stuff.test.integration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class IntegrationTest {
	private static final Instant DEFAULT_INSTANT = Instant.parse("2012-11-10T09:08:07.006005004Z");
	protected CloseableHttpClient httpClient;
	protected ServerRunner server = new ServerRunner();
	protected ManuallySetClock clock = new ManuallySetClock(DEFAULT_INSTANT, ZoneOffset.UTC);

	@Before
	public void startServer() throws Exception {
		server.addConfiguration(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Clock.class).toInstance(clock);
			}
		});
		server.addConfiguration(serverConfiguration());
		server.start();
	}

	@After
	public void stopServer() throws Exception {
		server.stop();
	}

	@Before
	public void setUp() throws Exception {
		httpClient = HttpClients.createDefault();
	}

	@After
	public void tearDown() throws Exception {
		if (httpClient != null) httpClient.close();
	}

	protected Module serverConfiguration() {
		return new AbstractModule() {
			@Override
			protected void configure() {
			}
		};
	}

	protected CloseableHttpResponse doGetWithHeaders(String path, Multimap<String, String> headers) throws IOException,
			URISyntaxException {
		HttpUriRequest request = new HttpGet(server.uri(path));
		for (Map.Entry<String, String> e : headers.entries()) {
			request.addHeader(e.getKey(), e.getValue());
		}
		return httpClient.execute(request);
	}

	protected CloseableHttpResponse doGet(String path) throws IOException, URISyntaxException {
		return doGetWithHeaders(path, ImmutableMultimap.<String,String>of());
	}

	protected CloseableHttpResponse doPostForm(String path, Map<String, String> headers,
			Map<String, String> formParameters) throws IOException, URISyntaxException {
		HttpPost request = new HttpPost(server.uri(path));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}
		List<NameValuePair> parameters = new ArrayList<>();
		for (Map.Entry<String, String> e : formParameters.entrySet()) {
			parameters.add(new BasicNameValuePair(e.getKey(), e.getValue()));
		}
		request.setEntity(new UrlEncodedFormEntity(parameters));
		return httpClient.execute(request);
	}
}

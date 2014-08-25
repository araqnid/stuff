package org.araqnid.stuff.test.integration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
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

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class IntegrationTest {
	protected CloseableHttpClient httpClient;
	protected ServerRunner server = new ServerRunner();

	@Before
	public void startServer() throws Exception {
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

	protected CloseableHttpResponse doGet(String path, Map<String, String> headers) throws IOException,
			URISyntaxException {
		HttpUriRequest request = new HttpGet(server.uri(path));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}
		return httpClient.execute(request);
	}

	protected CloseableHttpResponse doGet(String path) throws IOException, URISyntaxException {
		return doGet(path, Collections.<String, String> emptyMap());
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

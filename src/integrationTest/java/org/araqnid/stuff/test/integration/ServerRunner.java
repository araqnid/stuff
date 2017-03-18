package org.araqnid.stuff.test.integration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.apache.http.HttpException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.araqnid.stuff.config.AppModule;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

public class ServerRunner extends ExternalResource {
	private static final Logger LOG = LoggerFactory.getLogger(ServerRunner.class);
	private final Supplier<Map<String, String>> environmentSupplier;
	private final Module configuration;
	private Server server;
	private Injector injector;
	private int port;
	private CloseableHttpClient httpClient;

	public ServerRunner() {
		this(() -> ImmutableMap.of(), (binder) -> { });
	}

	public ServerRunner(Supplier<Map<String, String>> environmentSupplier, Module configuration) {
		this.environmentSupplier = environmentSupplier;
		this.configuration = configuration;
	}

	@Override
	protected void before() throws Throwable {
		injector = Guice.createInjector(Modules.override(new AppModule(environmentSupplier.get())).with(configuration));
		server = injector.getInstance(Server.class);
		server.start();
		port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
		httpClient = HttpClients.custom()
				.setRoutePlanner((target, request, context) -> {
					if (!target.getHostName().equals("localhost") || target.getPort() != port) {
						throw new HttpException("Not permitted to request off-server resources: " + request.getRequestLine().getUri());
					}
					return new HttpRoute(target);
				})
				.build();
	}

	@Override
	protected void after() {
		if (server != null) {
			try {
				server.stop();
			} catch (Exception e) {
				Throwables.throwIfUnchecked(e);
				throw new RuntimeException(e);
			}
		}
		if (httpClient != null) {
			try {
				httpClient.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public URI uri(String path) throws URISyntaxException {
		Preconditions.checkArgument(path.startsWith("/"));
		return new URI("http", null, "localhost", port, path, null, null);
	}

	public Injector getInjector() {
		return injector;
	}


	public CloseableHttpResponse doGetWithHeaders(String path, Multimap<String, String> headers) throws IOException,
			URISyntaxException {
		HttpUriRequest request = new HttpGet(uri(path));
		headers.forEach(request::addHeader);
		return httpClient.execute(request);
	}

	public CloseableHttpResponse doGet(String path) throws IOException, URISyntaxException {
		return doGetWithHeaders(path, ImmutableMultimap.of());
	}

	public CloseableHttpResponse doPostForm(String path, Multimap<String, String> headers,
											   Map<String, String> formParameters) throws IOException, URISyntaxException {
		HttpPost request = new HttpPost(uri(path));
		headers.forEach(request::addHeader);
		request.setEntity(new UrlEncodedFormEntity(formParameters.entrySet().stream()
				.map(e -> new BasicNameValuePair(e.getKey(), e.getValue()))
				.collect(toList())));
		return httpClient.execute(request);
	}

}

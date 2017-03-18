package org.araqnid.stuff.test.integration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.araqnid.stuff.config.AppModule;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public class ServerRunner {
	private Set<Module> additionalConfig = new HashSet<>();
	private Server server;
	private Injector injector;
	private int port;

	public void addConfiguration(Module module) {
		additionalConfig.add(module);
	}

	public void start() throws Exception {
		injector = Guice.createInjector(Modules.override(new AppModule()).with(Modules.combine(additionalConfig)));
		server = injector.getInstance(Server.class);
		server.start();
		port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
	}

	public void stop() throws Exception {
		if (server != null) server.stop();
	}

	public void reset() throws Exception {
	}

	public URI uri(String path) throws URISyntaxException {
		Preconditions.checkArgument(path.startsWith("/"));
		return new URI("http", null, "localhost", port, path, null, null);
	}

	public Injector getInjector() {
		return injector;
	}
}

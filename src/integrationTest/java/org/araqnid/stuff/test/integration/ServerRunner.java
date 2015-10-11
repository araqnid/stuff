package org.araqnid.stuff.test.integration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Singleton;

import org.araqnid.stuff.config.StandaloneAppConfig;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.ThreadPool;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.util.Modules;

public class ServerRunner {
	private static final ExecutorService SHARED_THREADS = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
			.setNameFormat("test-%d").setDaemon(true).build());
	private Set<Module> additionalConfig = new HashSet<>();
	private Server server;
	private Injector injector;
	private int port;

	public void addConfiguration(Module module) {
		additionalConfig.add(module);
	}

	public void start() throws Exception {
		AbstractModule jettyConfig = new AbstractModule() {
			@Override
			protected void configure() {
			}

			@Singleton
			@Provides
			public Server server(Handler handler) {
				Server server = new Server(new ThreadPool() {
					@Override
					public void execute(Runnable command) {
						SHARED_THREADS.execute(command);
					}

					@Override
					public void join() throws InterruptedException {
					}

					@Override
					public boolean isLowOnThreads() {
						return false;
					}

					@Override
					public int getThreads() {
						return 0;
					}

					@Override
					public int getIdleThreads() {
						return 0;
					}
				});
				server.setConnectors(new Connector[] { new ServerConnector(server) });
				server.setHandler(handler);
				return server;
			}

			@Provides
			public ServerConnector serverConnector(Server server) {
				return (ServerConnector) server.getConnectors()[0];
			}
		};
		injector = Guice.createInjector(Modules.override(new StandaloneAppConfig()).with(
				Iterables.<Module> concat(ImmutableSet.of(jettyConfig), additionalConfig)));
		server = injector.getInstance(Server.class);
		server.start();
		port = injector.getInstance(ServerConnector.class).getLocalPort();
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

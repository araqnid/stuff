package org.araqnid.stuff.test.integration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.araqnid.stuff.activity.ActivityEventSink;
import org.araqnid.stuff.config.StandaloneAppConfig;
import org.araqnid.stuff.test.integration.CollectActivityEvents.ActivityEventRecord;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

public class ServerRunner {
	private Set<Module> additionalConfig = new HashSet<>();
	private Server server;
	private Injector injector;
	private int port;
	private CollectActivityEvents collectActivityEvents = new CollectActivityEvents();

	public void addConfiguration(Module module) {
		additionalConfig.add(module);
	}

	public void start() throws Exception {
		AbstractModule jettyConfig = new AbstractModule() {
			@Override
			protected void configure() {
				bind(ActivityEventSink.class).toInstance(collectActivityEvents);
			}

			@Singleton
			@Provides
			public Server server(Handler handler) {
				Server server = new Server();
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
		collectActivityEvents.events.clear();
	}

	public URI uri(String path) throws URISyntaxException {
		Preconditions.checkArgument(path.startsWith("/"));
		return new URI("http", null, "localhost", port, path, null, null);
	}

	public Injector getInjector() {
		return injector;
	}

	public Iterable<CollectActivityEvents.ActivityEventRecord> activityEvents() {
		return collectActivityEvents.events;
	}

	public Iterable<CollectActivityEvents.ActivityEventRecord> activityEventsForRuid(final String ruid) {
		return Iterables.filter(collectActivityEvents.events,
				new Predicate<CollectActivityEvents.ActivityEventRecord>() {
					@Override
					public boolean apply(ActivityEventRecord input) {
						return input.ruid.equals(ruid);
					}
				});
	}
}

package org.araqnid.stuff.test.integration;

import org.araqnid.stuff.activity.ActivityEventSink;
import org.araqnid.stuff.config.StandaloneAppConfig;
import org.araqnid.stuff.test.integration.CollectActivityEvents.ActivityEventRecord;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

public class ServerRunner {
	private Server server;
	private Injector injector;
	private int port;
	private String baseUri;
	private CollectActivityEvents collectActivityEvents = new CollectActivityEvents();

	public void start() throws Exception {
		injector = Guice.createInjector(Modules.override(new StandaloneAppConfig()).with(new AbstractModule() {
			@Override
			protected void configure() {
				bind(ActivityEventSink.class).toInstance(collectActivityEvents);
			}

			@Singleton
			@Provides
			public Connector connector() {
				return new SelectChannelConnector();
			}
		}));
		server = injector.getInstance(Server.class);
		server.start();
		port = injector.getInstance(Connector.class).getLocalPort();
		baseUri = String.format("http://localhost:%d", port);
	}

	public void stop() throws Exception {
		if (server != null) server.stop();
	}

	public void reset() throws Exception {
		collectActivityEvents.events.clear();
	}

	public String appUri(String path) {
		Preconditions.checkArgument(path.startsWith("/"));
		return baseUri + path;
	}

	public Injector getInjector() {
		return injector;
	}

	public Iterable<CollectActivityEvents.ActivityEventRecord> activityEvents() {
		return collectActivityEvents.events;
	}

	public Iterable<CollectActivityEvents.ActivityEventRecord> activityEventsForRuid(final String ruid) {
		return Iterables.filter(collectActivityEvents.events, new Predicate<CollectActivityEvents.ActivityEventRecord>() {
			@Override
			public boolean apply(ActivityEventRecord input) {
				return input.ruid.equals(ruid);
			}
		});
	}
}

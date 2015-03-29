package org.araqnid.stuff;

import java.util.UUID;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.araqnid.stuff.config.ServerIdentity;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AppStartupBanner implements AppLifecycleEvent {
	private static final Logger LOG = LoggerFactory.getLogger(AppStartupBanner.class);
	private final Supplier<Integer> httpPort;
	private final AppVersion appVersion;
	private final UUID instanceId;

	@Inject
	public AppStartupBanner(final Server server, AppVersion appVersion, @ServerIdentity UUID instanceId) {
		this.httpPort = () -> httpPort(server);
		this.appVersion = appVersion;
		this.instanceId = instanceId;
	}

	@Override
	public void starting() {
	}

	@Override
	public void started() {
		LOG.info("Started instance {} (app version {}); listening for HTTP on {}", instanceId, appVersion.version,
				httpPort.get());
	}

	@Override
	public void stopping() {
	}

	@Override
	public void stopped() {
		LOG.info("Stopped instance {} (app version {})", instanceId, appVersion.version);
	}

	private static int httpPort(Server server) {
		ServerConnector connector = (ServerConnector) server.getConnectors()[0];
		return connector.getLocalPort();
	}
}

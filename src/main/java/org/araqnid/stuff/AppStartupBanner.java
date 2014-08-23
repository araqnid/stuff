package org.araqnid.stuff;

import java.util.UUID;

import org.araqnid.stuff.config.ServerIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class AppStartupBanner implements AppLifecycleEvent {
	private static final Logger LOG = LoggerFactory.getLogger(AppStartupBanner.class);
	private final int httpPort;
	private final AppVersion appVersion;
	private final UUID instanceId;

	@Inject
	public AppStartupBanner(@Named("http_port") int httpPort, AppVersion appVersion, @ServerIdentity UUID instanceId) {
		this.httpPort = httpPort;
		this.appVersion = appVersion;
		this.instanceId = instanceId;
	}

	@Override
	public void starting() {
	}

	@Override
	public void started() {
		LOG.info("Started instance {} (app version {}); listening for HTTP on {}", instanceId, appVersion.version,
				httpPort);
	}

	@Override
	public void stopping() {
	}

	@Override
	public void stopped() {
		LOG.info("Stopped instance {} (app version {})", instanceId, appVersion.version);
	}
}

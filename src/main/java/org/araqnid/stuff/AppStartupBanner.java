package org.araqnid.stuff;

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

	@Inject
	public AppStartupBanner(@Named("http_port") int httpPort, AppVersion appVersion) {
		this.httpPort = httpPort;
		this.appVersion = appVersion;
	}

	@Override
	public void starting() {
	}

	@Override
	public void started() {
		LOG.info("Started version {}; listening for HTTP on {}", appVersion.version, httpPort);
	}

	@Override
	public void stopping() {
	}

	@Override
	public void stopped() {
	}
}

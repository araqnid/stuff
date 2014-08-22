package org.araqnid.stuff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class EmbeddedAppStartupBanner implements AppLifecycleEvent {
	private static final Logger LOG = LoggerFactory.getLogger(EmbeddedAppStartupBanner.class);
	private final String contextPath;
	private final AppVersion appVersion;

	@Inject
	public EmbeddedAppStartupBanner(@Named("context_path") String contextPath, AppVersion appVersion) {
		this.contextPath = contextPath;
		this.appVersion = appVersion;
	}

	@Override
	public void starting() {
	}

	@Override
	public void started() {
		if (contextPath.isEmpty()) {
			LOG.info("Started version {}; at root context", appVersion.version);
		} else {
			LOG.info("Started version {}; at context path {}", appVersion.version, contextPath);
		}
	}

	@Override
	public void stopping() {
	}

	@Override
	public void stopped() {
		if (contextPath.isEmpty()) {
			LOG.info("Stopped version {}; at root context", appVersion.version);
		} else {
			LOG.info("Stopped version {}; at context path {}", appVersion.version, contextPath);
		}
	}
}

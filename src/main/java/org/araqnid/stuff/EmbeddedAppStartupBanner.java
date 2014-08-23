package org.araqnid.stuff;

import java.util.UUID;

import org.araqnid.stuff.config.ServerIdentity;
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
	private final UUID instanceId;

	@Inject
	public EmbeddedAppStartupBanner(@Named("context_path") String contextPath, AppVersion appVersion,
			@ServerIdentity UUID instanceId) {
		this.contextPath = contextPath;
		this.appVersion = appVersion;
		this.instanceId = instanceId;
	}

	@Override
	public void starting() {
	}

	@Override
	public void started() {
		if (contextPath.isEmpty()) {
			LOG.info("Started instance {} (app version {}); at root context", instanceId, appVersion.version);
		}
		else {
			LOG.info("Started instance {} (app version {}); at context path {}", instanceId, appVersion.version,
					contextPath);
		}
	}

	@Override
	public void stopping() {
	}

	@Override
	public void stopped() {
		if (contextPath.isEmpty()) {
			LOG.info("Stopped instance {} (app version {}); at root context", instanceId, appVersion.version);
		}
		else {
			LOG.info("Stopped instance {} (app version {}); at context path {}", instanceId, appVersion.version,
					contextPath);
		}
	}
}

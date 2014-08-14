package org.araqnid.stuff;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AppServicesManager {
	private static final Logger LOG = LoggerFactory.getLogger(AppServicesManager.class);
	private final Set<AppService> services;
	private final AppLifecycleEvent lifecycleEvents;

	@Inject
	public AppServicesManager(AppLifecycleEvent lifecycleEvents, Set<AppService> services) {
		this.lifecycleEvents = lifecycleEvents;
		this.services = services;
	}

	public void start() {
		lifecycleEvents.starting();
		LOG.info("Starting app services");
		for (AppService service : services) {
			LOG.info("Starting {}", service);
			service.start();
		}
		lifecycleEvents.started();
	}

	public void stop() {
		lifecycleEvents.stopping();
		LOG.info("Stopping app services");
		for (AppService service : services) {
			LOG.info("Stopping {}", service);
			service.stop();
		}
		lifecycleEvents.stopped();
	}
}

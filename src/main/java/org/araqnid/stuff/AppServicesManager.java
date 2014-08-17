package org.araqnid.stuff;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AppServicesManager {
	private final Set<AppService> services;
	private final AppLifecycleEvent lifecycleEvents;

	@Inject
	public AppServicesManager(AppLifecycleEvent lifecycleEvents, Set<AppService> services) {
		this.lifecycleEvents = lifecycleEvents;
		this.services = services;
	}

	public void start() {
		lifecycleEvents.starting();
		for (AppService service : services) {
			service.start();
		}
		lifecycleEvents.started();
	}

	public void stop() {
		lifecycleEvents.stopping();
		for (AppService service : services) {
			service.stop();
		}
		lifecycleEvents.stopped();
	}
}

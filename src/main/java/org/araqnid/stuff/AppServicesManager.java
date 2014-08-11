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
	private AppState state = AppState.CREATED;

	@Inject
	public AppServicesManager(Set<AppService> services) {
		this.services = services;
	}

	public void start() {
		setState(AppState.STARTING);
		LOG.info("Starting app services");
		for (AppService service : services) {
			LOG.info("Starting {}", service);
			service.start();
		}
		setState(AppState.STARTED);
	}

	public void stop() {
		setState(AppState.STOPPING);
		LOG.info("Stopping app services");
		for (AppService service : services) {
			LOG.info("Stopping {}", service);
			service.stop();
		}
		setState(AppState.STOPPED);
	}

	public synchronized AppState getState() {
		return state;
	}

	private synchronized void setState(AppState newState) {
		state = newState;
	}
}

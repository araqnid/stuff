package org.araqnid.stuff;

import java.util.Set;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AppServicesAdaptor extends AbstractIdleService {
	private final Set<AppService> services;

	@Inject
	public AppServicesAdaptor(Set<AppService> services) {
		this.services = services;
	}

	@Override
	protected void startUp() throws Exception {
		for (AppService svc : services) {
			svc.start();
		}
	}

	@Override
	protected void shutDown() throws Exception {
		for (AppService svc : services) {
			svc.stop();
		}
	}

}

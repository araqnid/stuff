package org.araqnid.stuff;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.google.inject.Inject;

@Path("info")
public class InfoResources {
	private final AppVersion appVersion;
	private final AppStateMonitor appStateMonitor;

	@Inject
	public InfoResources(AppVersion appVersion, AppStateMonitor appStateMonitor) {
		this.appVersion = appVersion;
		this.appStateMonitor = appStateMonitor;
	}

	@GET
	@Path("version")
	@Produces("application/json")
	public AppVersion getVersion() {
		return appVersion;
	}

	@GET
	@Path("state")
	@Produces("application/json")
	public AppState getAppState() {
		return appStateMonitor.getState();
	}
}

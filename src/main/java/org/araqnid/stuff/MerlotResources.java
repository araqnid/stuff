package org.araqnid.stuff;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.google.inject.Inject;

@Path("merlot")
public class MerlotResources {
	private final AppVersion appVersion;

	@Inject
	public MerlotResources(AppVersion appVersion) {
		this.appVersion = appVersion;
	}

	@GET
	@Produces("application/json")
	public Status fetchStatus() {
		return new Status(appVersion.version, null);
	}

	public static final class Status {
		public final String version;
		public final String userInfo;

		public Status(String version, String userInfo) {
			this.version = version;
			this.userInfo = userInfo;
		}
	}
}

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
		public final UserInfo userInfo;

		public Status(String version, UserInfo userInfo) {
			this.version = version;
			this.userInfo = userInfo;
		}
	}

	public static final class UserInfo {
		public final String commonName;
		public final String username;

		public UserInfo(String commonName, String username) {
			this.commonName = commonName;
			this.username = username;
		}
	}
}

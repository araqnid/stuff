package org.araqnid.stuff;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.araqnid.stuff.MerlotRepository.User;

import com.google.common.base.Optional;
import com.google.inject.Inject;

@Path("merlot")
public class MerlotResources {
	private final AppVersion appVersion;
	private final MerlotRepository repository;
	@CookieParam("ATKT")
	private UserTicket userTicket;

	@Inject
	public MerlotResources(AppVersion appVersion, MerlotRepository repository) {
		this.appVersion = appVersion;
		this.repository = repository;
	}

	@GET
	@Produces("application/json")
	public Status fetchStatus() {
		return new Status(appVersion.version, optionalUserInfo().orNull());
	}

	@POST
	@Path("sign-in")
	public void signIn() {
	}

	@POST
	@Path("sign-out")
	public void signOut() {
	}

	private Optional<UserInfo> optionalUserInfo() {
		if (userTicket == null) return Optional.absent();
		Optional<User> user = repository.findUserById(userTicket.userId);
		if (!user.isPresent()) throw new InvalidUserException(userTicket.userId);
		return Optional.of(new UserInfo(user.get().commonName, user.get().username));
	}

	public static final class Status {
		public final String version;
		public final UserInfo userInfo;

		public Status(String version, UserInfo userInfo) {
			this.version = version;
			this.userInfo = userInfo;
		}
	}

	public static final class InvalidUserException extends WebApplicationException {
		private static final long serialVersionUID = 2014082301L;

		public InvalidUserException(UUID userId) {
			super("Invalid user: " + userId, HttpServletResponse.SC_FORBIDDEN);
		}
	}

	public static final class UserTicket {
		private static final Pattern PATTERN = Pattern
				.compile("u([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}),x([0-9a-f]{8})");
		public final UUID userId;

		public UserTicket(UUID userId) {
			this.userId = userId;
		}

		public static UserTicket fromString(String ticket) {
			Matcher matcher = PATTERN.matcher(ticket);
			if (!matcher.matches()) throw new InvalidTicketException(ticket);
			return new UserTicket(UUID.fromString(matcher.group(1)));
		}

		public static final class InvalidTicketException extends WebApplicationException {
			private static final long serialVersionUID = 2014082301L;

			public InvalidTicketException(String ticket) {
				super("Invalid ticket: " + ticket, HttpServletResponse.SC_FORBIDDEN);
			}
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

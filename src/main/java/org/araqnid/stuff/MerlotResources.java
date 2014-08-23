package org.araqnid.stuff;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.araqnid.stuff.MerlotRepository.User;

import com.google.common.base.Optional;
import com.google.inject.Inject;

@Path("merlot")
public class MerlotResources {
	private final AppVersion appVersion;
	private final MerlotRepository repository;
	@CookieParam("ATKT")
	private UserTicket userTicket;
	private String cookiePath = null;
	private String cookieDomain = null;
	private String cookieComment = null;
	private int cookieMaxAge = -1;
	private boolean cookieSecure = false;

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
	public Response signIn(@FormParam("username") String username, @FormParam("password") Password password) {
		Optional<User> optionalUser = repository.findUserByName(username);
		if (!optionalUser.isPresent()) throw new NoSuchUsernameException(username);
		User user = optionalUser.get();
		if (!password.matches(user.password)) throw new InvalidPasswordException(username);
		return Response.noContent().cookie(newAuthTicket(user)).build();
	}

	@POST
	@Path("sign-out")
	public Response signOut() {
		if (userTicket == null) return Response.noContent().build();
		return Response.noContent().cookie(removeAuthTicket()).build();
	}

	private Optional<UserInfo> optionalUserInfo() {
		if (userTicket == null) return Optional.absent();
		Optional<User> user = repository.findUserById(userTicket.userId);
		if (!user.isPresent()) throw new InvalidUserException(userTicket.userId);
		return Optional.of(new UserInfo(user.get().commonName, user.get().username));
	}

	private NewCookie newAuthTicket(User user) {
		return new NewCookie("ATKT", new UserTicket(user.id).marshal(), cookiePath, cookieDomain, cookieComment, cookieMaxAge, cookieSecure);
	}

	private NewCookie removeAuthTicket() {
		return new NewCookie("ATKT", "", cookiePath, cookieDomain, cookieComment, 0, cookieSecure);
	}

	public static class Password {
		private final char[] data;

		public Password(String str) {
			data = str.toCharArray();
		}

		public boolean matches(char[] other) {
			if (data.length != other.length) return false;
			for (int i = 0; i < data.length; i++) {
				if (data[i] != other[i]) return false;
			}
			return true;
		}
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

	public static final class NoSuchUsernameException extends WebApplicationException {
		private static final long serialVersionUID = 2014082301L;

		public NoSuchUsernameException(String username) {
			super("Unknown user: " + username, HttpServletResponse.SC_FORBIDDEN);
		}
	}

	public static final class InvalidPasswordException extends WebApplicationException {
		private static final long serialVersionUID = 2014082301L;

		public InvalidPasswordException(String username) {
			super("Invalid password for " + username, HttpServletResponse.SC_FORBIDDEN);
		}
	}

	public static final class UserTicket {
		private static final Pattern PATTERN = Pattern
				.compile("u([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}),x([0-9a-f]{8})");
		public final UUID userId;

		public UserTicket(UUID userId) {
			this.userId = userId;
		}

		public String marshal() {
			return String.format("u%s,x00000000", userId);
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

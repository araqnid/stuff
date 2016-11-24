package org.araqnid.stuff;

import java.util.Optional;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("hello/{name}")
public class HelloResource {
	private final GreetingRepository greetings;
	private final HttpServletRequest request;
	@PathParam("name")
	private String name;

	@Inject
	public HelloResource(GreetingRepository greetings, HttpServletRequest request) {
		this.greetings = greetings;
		this.request = request;
	}

	@GET
	@Produces("text/plain")
	public String hello() {
		return Optional.ofNullable(greetings.find(name)).orElseGet(() -> "Hello " + name + ", your session is " + request.getSession().getId());
	}

	@PUT
	@Consumes("text/plain")
	public void receiveHello(String greeting) {
		greetings.save(name, greeting);
	}

	@DELETE
	public void deleteHello() {
		greetings.delete(name);
	}
}

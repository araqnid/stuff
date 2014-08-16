package org.araqnid.stuff;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.inject.Inject;

@Path("hello")
public class HelloResource {
	private final GreetingRepository greetings;

	@Inject
	public HelloResource(GreetingRepository greetings) {
		this.greetings = greetings;
	}

	@GET
	@Path("{name}")
	@Produces("text/plain")
	public String hello(@PathParam("name") final String name) {
		return Optional.fromNullable(greetings.find(name)).or(new Supplier<String>() {
			@Override
			public String get() {
				return "Hello " + name;
			}
		});
	}

	@PUT
	@Path("{name}")
	@Consumes("text/plain")
	public void receiveHello(@PathParam("name") String name, String greeting) {
		greetings.save(name, greeting);
	}

	@DELETE
	@Path("{name}")
	public void deleteHello(@PathParam("name") String name) {
		greetings.delete(name);
	}
}

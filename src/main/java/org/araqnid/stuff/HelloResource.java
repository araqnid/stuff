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

@Path("hello/{name}")
public class HelloResource {
	private final GreetingRepository greetings;
	@PathParam("name")
	private String name;

	@Inject
	public HelloResource(GreetingRepository greetings) {
		this.greetings = greetings;
	}

	@GET
	@Produces("text/plain")
	public String hello() {
		return Optional.fromNullable(greetings.find(name)).or(new Supplier<String>() {
			@Override
			public String get() {
				return "Hello " + name;
			}
		});
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

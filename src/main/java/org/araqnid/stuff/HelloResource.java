package org.araqnid.stuff;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("hello")
public class HelloResource {
	private static final Logger LOG = LoggerFactory.getLogger(HelloResource.class);

	@GET
	@Path("{name}")
	@Produces("text/plain")
	public String hello(@PathParam("name") String name) {
		return "Hello " + name;
	}

	@POST
	@Path("{name}")
	@Consumes("text/plain")
	public void receiveHello(@PathParam("name") String name, String greeting) {
		LOG.info("Received greeting {} for {}", greeting, name);
	}
}

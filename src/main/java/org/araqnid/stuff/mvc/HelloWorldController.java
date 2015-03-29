package org.araqnid.stuff.mvc;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.araqnid.stuff.config.ServerIdentity;

import com.google.inject.Inject;

@Path("mvc/helloworld")
public class HelloWorldController {
	private final String serverIdentity;

	@Inject
	public HelloWorldController(@ServerIdentity String serverIdentity) {
		this.serverIdentity = serverIdentity;
	}

	@GET
	@Produces("text/html")
	public View helloWorld() {
		return View.of("helloWorld")
				.put("message", "Hello World!")
				.put("server", serverIdentity)
				.build();
	}
}

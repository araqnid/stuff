package org.araqnid.stuff;

import javax.inject.Inject;

import com.google.common.util.concurrent.AbstractIdleService;
import org.eclipse.jetty.server.Server;

public class JettyService extends AbstractIdleService {
	private final Server jetty;

	@Inject
	public JettyService(Server jetty) {
		this.jetty = jetty;
	}

	@Override
	protected void startUp() throws Exception {
		jetty.start();
	}

	@Override
	protected void shutDown() throws Exception {
		jetty.stop();
	}
}

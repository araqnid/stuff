package org.araqnid.stuff;

import org.eclipse.jetty.server.Server;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;

public class JettyAppService extends AbstractIdleService {
	private final Server jetty;

	@Inject
	public JettyAppService(Server jetty) {
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

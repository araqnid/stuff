package org.araqnid.stuff;

import javax.inject.Inject;

import org.eclipse.jetty.server.Server;

import com.google.common.util.concurrent.AbstractIdleService;

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

package org.araqnid.stuff;

import org.eclipse.jetty.server.Server;

import com.google.inject.Inject;

public class JettyAppService implements AppService {
	private final Server jetty;

	@Inject
	public JettyAppService(Server jetty) {
		this.jetty = jetty;
	}

	@Override
	public void start() {
		try {
			jetty.start();
		} catch (Exception e) {
			throw new RuntimeException("Unable to start Jetty", e);
		}
	}

	@Override
	public void stop() {
		try {
			jetty.stop();
		} catch (Exception e) {
			throw new RuntimeException("Unable to stop Jetty", e);
		}
	}
}

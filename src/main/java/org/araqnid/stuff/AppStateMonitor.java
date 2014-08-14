package org.araqnid.stuff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class AppStateMonitor implements AppLifecycleEvent {
	private static final Logger LOG = LoggerFactory.getLogger(AppStateMonitor.class);
	private AppState state = AppState.CREATED;

	@Override
	public void starting() {
		setState(AppState.STARTING);
	}

	@Override
	public void started() {
		LOG.info("state monitor recieved started() event");
		setState(AppState.STARTED);
	}

	@Override
	public void stopping() {
		setState(AppState.STOPPING);
	}

	@Override
	public void stopped() {
		setState(AppState.STOPPED);
	}

	public synchronized AppState getState() {
		return state;
	}

	private synchronized void setState(AppState newState) {
		state = newState;
	}
}

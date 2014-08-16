package org.araqnid.stuff;

import com.google.inject.Singleton;

@Singleton
public class AppStateMonitor implements AppLifecycleEvent {
	private AppState state = AppState.CREATED;

	@Override
	public void starting() {
		setState(AppState.STARTING);
	}

	@Override
	public void started() {
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

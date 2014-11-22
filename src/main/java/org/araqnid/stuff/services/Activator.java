package org.araqnid.stuff.services;

import java.util.concurrent.Executor;

public interface Activator {
	public static abstract class ActivationListener {
		public void created() {
		}

		public void activated() {
		}

		public void deactivated() {
		}
	}

	void activate();

	void deactivate();

	void addActivationListener(ActivationListener listener, Executor executor);
}

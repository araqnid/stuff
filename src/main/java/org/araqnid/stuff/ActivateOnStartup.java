package org.araqnid.stuff;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.araqnid.stuff.services.Activator;

@Singleton
public class ActivateOnStartup implements AppLifecycleEvent {
	private final Set<Activator> activators;

	@Retention(RetentionPolicy.RUNTIME)
	@Qualifier
	public @interface OnStartup {
	}

	@Inject
	public ActivateOnStartup(@OnStartup Set<Activator> activators) {
		this.activators = activators;
	}

	@Override
	public void starting() {
	}

	@Override
	public void started() {
		for (Activator activator : activators) {
			activator.activate();
		}
	}

	@Override
	public void stopping() {
	}

	@Override
	public void stopped() {
	}
}

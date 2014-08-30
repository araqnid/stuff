package org.araqnid.stuff;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ActivateOnStartup implements AppLifecycleEvent {
	private final Set<Activator> activators;

	@Retention(RetentionPolicy.RUNTIME)
	@BindingAnnotation
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

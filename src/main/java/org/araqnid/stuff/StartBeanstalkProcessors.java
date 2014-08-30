package org.araqnid.stuff;

import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StartBeanstalkProcessors implements AppLifecycleEvent {
	private final Set<Activator> activators;

	@Inject
	public StartBeanstalkProcessors(Set<Service> allServices) {
		activators = ImmutableSet.copyOf(Iterables.transform(Sets.filter(allServices, new Predicate<Service>() {
			@Override
			public boolean apply(Service input) {
				if (!(input instanceof Activator)) return false;
				return true;
			}
		}), new Function<Service, Activator>() {
			@Override
			public Activator apply(Service input) {
				return (Activator) input;
			}
		}));
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

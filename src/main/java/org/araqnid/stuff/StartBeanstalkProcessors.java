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
	private final Set<ServiceActivator<?>> activators;

	@Inject
	public StartBeanstalkProcessors(Set<Service> allServices) {
		activators = ImmutableSet.copyOf(Iterables.transform(Sets.filter(allServices, new Predicate<Service>() {
			@Override
			public boolean apply(Service input) {
				if (!(input instanceof ServiceActivator<?>)) return false;
				return true;
			}
		}), new Function<Service, ServiceActivator<?>>() {
			@Override
			public ServiceActivator<?> apply(Service input) {
				return (ServiceActivator<?>) input;
			}
		}));
	}

	@Override
	public void starting() {
	}

	@Override
	public void started() {
		for (ServiceActivator<?> activator : activators) {
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

package org.araqnid.stuff.config;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;

import org.araqnid.stuff.ActivateOnStartup;
import org.araqnid.stuff.SometubeHandler;
import org.araqnid.stuff.activity.ActivityScopeControl;
import org.araqnid.stuff.messages.BeanstalkProcessor;
import org.araqnid.stuff.messages.BeanstalkProcessor.DeliveryTarget;
import org.araqnid.stuff.services.Activator;
import org.araqnid.stuff.services.ServiceActivator;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.ProviderWithDependencies;
import com.surftools.BeanstalkClient.Client;
import com.surftools.BeanstalkClientImpl.ClientImpl;

public final class RawBeanstalkModule extends AbstractModule {
	private static final TypeLiteral<ServiceActivator<BeanstalkProcessor>> BeanstalkProcessorActivator = new TypeLiteral<ServiceActivator<BeanstalkProcessor>>() {
	};
	private final Collection<TubeConfiguration> configurations = ImmutableSet.of(new TubeConfiguration("sometube",
			SometubeHandler.class));
	private final boolean autostart = false;

	@Override
	protected void configure() {
		Multibinder<Service> services = Multibinder.newSetBinder(binder(), Service.class);
		Multibinder<Activator> activateOnStartup = Multibinder.newSetBinder(binder(), Activator.class,
				ActivateOnStartup.OnStartup.class);

		for (final TubeConfiguration tube : configurations) {
			bind(Key.get(BeanstalkProcessor.class, tube.bindingAnnotation)).toProvider(
					new ProviderWithDependencies<BeanstalkProcessor>() {
						@Inject
						private Provider<Client> connectionProvider;
						@Inject
						private ActivityScopeControl scopeControl;
						private final Provider<? extends DeliveryTarget> targetProvider = binder().getProvider(
								tube.processorClass);

						@Override
						public Set<Dependency<?>> getDependencies() {
							return ImmutableSet.<Dependency<?>> of(Dependency.get(Key.get(Client.class)),
									Dependency.get(Key.get(tube.processorClass)));
						}

						@Override
						public BeanstalkProcessor get() {
							return new BeanstalkProcessor(connectionProvider, tube.name, scopeControl, targetProvider);
						}
					});
			bind(Key.get(BeanstalkProcessorActivator, tube.bindingAnnotation)).toProvider(
					new ProviderWithDependencies<ServiceActivator<BeanstalkProcessor>>() {
						private final Provider<BeanstalkProcessor> provider = binder().getProvider(
								Key.get(BeanstalkProcessor.class, tube.bindingAnnotation));

						@Override
						public Set<Dependency<?>> getDependencies() {
							return ImmutableSet.<Dependency<?>> of(Dependency.get(Key.get(BeanstalkProcessor.class,
									tube.bindingAnnotation)));
						}

						@Override
						public ServiceActivator<BeanstalkProcessor> get() {
							return new ServiceActivator<BeanstalkProcessor>(provider, autostart);
						}
					}).in(Singleton.class);
			services.addBinding().to(Key.get(BeanstalkProcessorActivator, tube.bindingAnnotation));
			activateOnStartup.addBinding().to(Key.get(BeanstalkProcessorActivator, tube.bindingAnnotation));
		}
	}

	@Provides
	public Client beanstalkClient() {
		ClientImpl client = new ClientImpl();
		client.setUniqueConnectionPerThread(false);
		return client;
	}

	private static class TubeConfiguration {
		public final String name;
		public final Class<? extends DeliveryTarget> processorClass;
		public final Annotation bindingAnnotation;

		public TubeConfiguration(String name, Class<? extends DeliveryTarget> processorClass) {
			this.name = name;
			this.processorClass = processorClass;
			this.bindingAnnotation = Names.named(name);
		}
	}
}

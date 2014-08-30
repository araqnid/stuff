package org.araqnid.stuff.config;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;

import org.araqnid.stuff.ActivateOnStartup;
import org.araqnid.stuff.Activator;
import org.araqnid.stuff.BeanstalkProcessor;
import org.araqnid.stuff.ServiceActivator;
import org.araqnid.stuff.SomeQueueProcessor;
import org.araqnid.stuff.activity.ActivityScopeControl;
import org.araqnid.stuff.activity.RequestActivity;
import org.araqnid.stuff.workqueue.SqlWorkQueue;
import org.araqnid.stuff.workqueue.WorkDispatcher;
import org.araqnid.stuff.workqueue.WorkProcessor;
import org.araqnid.stuff.workqueue.WorkQueueBeanstalkHandler;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.ProviderWithDependencies;
import com.surftools.BeanstalkClient.Client;

public final class WorkQueueModule extends AbstractModule {
	private final Collection<QueueConfiguration> configurations = ImmutableSet.of(new QueueConfiguration("somequeue",
			1, SomeQueueProcessor.class), new QueueConfiguration("otherqueue", 1, SomeQueueProcessor.class));
	private final boolean autostart = false;

	@Override
	protected void configure() {
		Multibinder<Service> services = Multibinder.newSetBinder(binder(), Service.class);
		Multibinder<Activator> activateOnStartup = Multibinder.newSetBinder(binder(), Activator.class,
				ActivateOnStartup.OnStartup.class);

		for (final QueueConfiguration queue : configurations) {
			bind(Key.get(WorkQueueBeanstalkHandler.class, queue.bindingAnnotation)).toProvider(
					new ProviderWithDependencies<WorkQueueBeanstalkHandler>() {
						@Inject
						private Provider<RequestActivity> requestActivityProvider;
						private Provider<? extends WorkProcessor> processorProvider = binder().getProvider(
								queue.processorClass);

						@Override
						public Set<Dependency<?>> getDependencies() {
							return ImmutableSet.<Dependency<?>> of(Dependency.get(Key.get(RequestActivity.class)),
									Dependency.get(Key.get(queue.processorClass)));
						}

						@Override
						public WorkQueueBeanstalkHandler get() {
							RequestActivity requestActivity = requestActivityProvider.get();
							SqlWorkQueue queueImpl = new SqlWorkQueue(queue.name, requestActivity);
							WorkDispatcher dispatcher = new WorkDispatcher(queueImpl, processorProvider.get());
							WorkQueueBeanstalkHandler beanstalkTarget = new WorkQueueBeanstalkHandler(queue.name,
									dispatcher, requestActivity);
							return beanstalkTarget;
						}
					});
			bind(Key.get(BeanstalkProcessor.class, queue.bindingAnnotation)).toProvider(
					new ProviderWithDependencies<BeanstalkProcessor>() {
						@Inject
						private Provider<Client> connectionProvider;
						@Inject
						private ActivityScopeControl scopeControl;
						private Provider<WorkQueueBeanstalkHandler> targetProvider = binder().getProvider(
								Key.get(WorkQueueBeanstalkHandler.class, queue.bindingAnnotation));

						@Override
						public Set<Dependency<?>> getDependencies() {
							return ImmutableSet.<Dependency<?>> of(Dependency.get(Key.get(Client.class)),
									Dependency.get(Key.get(ActivityScopeControl.class)),
									Dependency.get(Key.get(WorkQueueBeanstalkHandler.class, queue.bindingAnnotation)));
						}

						@Override
						public BeanstalkProcessor get() {
							return new BeanstalkProcessor(connectionProvider, queue.name, queue.threads, scopeControl,
									targetProvider);
						}
					});
			bind(Key.get(ServiceActivator.class, queue.bindingAnnotation)).toProvider(
					new Provider<ServiceActivator<BeanstalkProcessor>>() {
						private Provider<BeanstalkProcessor> provider = binder().getProvider(
								Key.get(BeanstalkProcessor.class, queue.bindingAnnotation));

						@Override
						public ServiceActivator<BeanstalkProcessor> get() {
							return new ServiceActivator<BeanstalkProcessor>(provider, autostart);
						}
					}).in(Singleton.class);
			services.addBinding().to(Key.get(ServiceActivator.class, queue.bindingAnnotation));
			activateOnStartup.addBinding().to(Key.get(ServiceActivator.class, queue.bindingAnnotation));
		}
	}

	private static class QueueConfiguration {
		public final String name;
		public final int threads;
		public final Class<? extends WorkProcessor> processorClass;
		public final Annotation bindingAnnotation;

		public QueueConfiguration(String name, int threads, Class<? extends WorkProcessor> processorClass) {
			this.name = name;
			this.threads = threads;
			this.processorClass = processorClass;
			this.bindingAnnotation = Names.named(name);
		}
	}
}

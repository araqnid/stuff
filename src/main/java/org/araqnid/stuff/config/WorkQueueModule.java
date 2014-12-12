package org.araqnid.stuff.config;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import org.araqnid.stuff.ActivateOnStartup;
import org.araqnid.stuff.AppVersion;
import org.araqnid.stuff.PostgresqlDataSourceProviderService;
import org.araqnid.stuff.SomeQueueProcessor;
import org.araqnid.stuff.activity.ActivityScopeControl;
import org.araqnid.stuff.activity.RequestActivity;
import org.araqnid.stuff.messages.BeanstalkProcessor;
import org.araqnid.stuff.messages.RedisProcessor;
import org.araqnid.stuff.services.Activator;
import org.araqnid.stuff.services.ServiceActivator;
import org.araqnid.stuff.workqueue.SqlWorkQueue;
import org.araqnid.stuff.workqueue.SqlWorkQueue.Accessor;
import org.araqnid.stuff.workqueue.WorkDispatcher;
import org.araqnid.stuff.workqueue.WorkProcessor;
import org.araqnid.stuff.workqueue.WorkQueue;
import org.araqnid.stuff.workqueue.WorkQueueBeanstalkHandler;
import org.araqnid.stuff.workqueue.WorkQueueRedisHandler;

import redis.clients.jedis.Jedis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.ProviderWithDependencies;
import com.surftools.BeanstalkClient.Client;

public final class WorkQueueModule extends AbstractModule {
	private final Collection<QueueConfiguration> beanstalk = ImmutableSet.of(new QueueConfiguration("somequeue",
			SomeQueueProcessor.class), new QueueConfiguration("otherqueue", SomeQueueProcessor.class));
	private final Collection<QueueConfiguration> redis = ImmutableSet.of(new QueueConfiguration("thisqueue",
			SomeQueueProcessor.class), new QueueConfiguration("thatqueue", SomeQueueProcessor.class));
	private final boolean autostart = false;

	@Override
	protected void configure() {
		Multibinder<Service> services = Multibinder.newSetBinder(binder(), Service.class);
		Multibinder<Activator> activateOnStartup = Multibinder.newSetBinder(binder(), Activator.class,
				ActivateOnStartup.OnStartup.class);

		for (final QueueConfiguration queue : Iterables.concat(beanstalk, redis)) {
			bind(WorkQueue.class).annotatedWith(queue.bindingAnnotation).toProvider(new Provider<SqlWorkQueue>() {
				@Inject
				private Provider<SqlWorkQueue.Accessor> accessorProvider;
				@Inject
				private ObjectMapper objectMapper;
				@Inject
				private AppVersion appVersion;
				@Inject
				@ServerIdentity
				private UUID instanceId;
				@Inject
				@ServerIdentity
				private String hostname;
				@Inject
				private Provider<RequestActivity> requestActivityProvider;

				@Override
				public SqlWorkQueue get() {
					Accessor accessor = accessorProvider.get();
					RequestActivity requestActivity = requestActivityProvider.get();
					return new SqlWorkQueue(queue.name, accessor, objectMapper, appVersion, instanceId, hostname, requestActivity);
				}
			});

			bind(WorkDispatcher.class).annotatedWith(queue.bindingAnnotation).toProvider(new ProviderWithDependencies<WorkDispatcher>() {
				private final Key<WorkQueue> queueKey = Key.get(WorkQueue.class, queue.bindingAnnotation);
				private final Provider<WorkQueue> workQueueProvider = binder().getProvider(queueKey);
				private final Provider<? extends WorkProcessor> processorProvider = binder().getProvider(queue.processorClass);
				@Inject
				private Provider<RequestActivity> requestActivityProvider;

				@Override
				public WorkDispatcher get() {
					WorkQueue workQueue = workQueueProvider.get();
					WorkProcessor queueProcessor = processorProvider.get();
					RequestActivity requestActivity = requestActivityProvider.get();
					return new WorkDispatcher(workQueue, queueProcessor, requestActivity);
				}

				@Override
				public Set<Dependency<?>> getDependencies() {
					return ImmutableSet.<Dependency<?>> builder()
							.addAll(Dependency.forInjectionPoints(InjectionPoint.forInstanceMethodsAndFields(getClass())))
							.add(Dependency.get(queueKey))
							.add(Dependency.get(Key.get(queue.processorClass)))
							.build();
				}
			});
		}

		for (final QueueConfiguration queue : beanstalk) {
			final TypeLiteral<BeanstalkProcessor<WorkQueueBeanstalkHandler>> processorType = new TypeLiteral<BeanstalkProcessor<WorkQueueBeanstalkHandler>>(){};
			final TypeLiteral<ServiceActivator<BeanstalkProcessor<WorkQueueBeanstalkHandler>>> activatorType = new TypeLiteral<ServiceActivator<BeanstalkProcessor<WorkQueueBeanstalkHandler>>>(){};
			bind(WorkQueueBeanstalkHandler.class).annotatedWith(queue.bindingAnnotation).toProvider(new ProviderWithDependencies<WorkQueueBeanstalkHandler>() {
				private Provider<WorkDispatcher> dispatcherProvider = binder().getProvider(Key.get(WorkDispatcher.class, queue.bindingAnnotation));

				@Override
				public Set<Dependency<?>> getDependencies() {
					return ImmutableSet.<Dependency<?>> of(Dependency.get(Key.get(WorkDispatcher.class, queue.bindingAnnotation)));
				}

				@Override
				public WorkQueueBeanstalkHandler get() {
					return new WorkQueueBeanstalkHandler(dispatcherProvider.get());
				}
			});
			bind(processorType).annotatedWith(queue.bindingAnnotation).toProvider(new ProviderWithDependencies<BeanstalkProcessor<WorkQueueBeanstalkHandler>>() {
				private final Key<WorkQueueBeanstalkHandler> handlerKey = Key.get(WorkQueueBeanstalkHandler.class, queue.bindingAnnotation);
				@Inject
				private Provider<Client> connectionProvider;
				@Inject
				private ActivityScopeControl scopeControl;
				private Provider<WorkQueueBeanstalkHandler> targetProvider = binder().getProvider(handlerKey);

				@Override
				public Set<Dependency<?>> getDependencies() {
					return ImmutableSet.<Dependency<?>> builder()
							.addAll(Dependency.forInjectionPoints(InjectionPoint.forInstanceMethodsAndFields(getClass())))
							.add(Dependency.get(handlerKey))
							.build();
				}

				@Override
				public BeanstalkProcessor<WorkQueueBeanstalkHandler> get() {
					return new BeanstalkProcessor<WorkQueueBeanstalkHandler>(connectionProvider, queue.name, scopeControl, targetProvider);
				}
			});
			bind(activatorType).annotatedWith(queue.bindingAnnotation).toProvider(new ProviderWithDependencies<ServiceActivator<BeanstalkProcessor<WorkQueueBeanstalkHandler>>>() {
				private final Key<BeanstalkProcessor<WorkQueueBeanstalkHandler>> consumerServiceKey = Key.get(processorType, queue.bindingAnnotation);
				private final Provider<BeanstalkProcessor<WorkQueueBeanstalkHandler>> provider = binder().getProvider(consumerServiceKey);

				@Override
				public ServiceActivator<BeanstalkProcessor<WorkQueueBeanstalkHandler>> get() {
					return new ServiceActivator<BeanstalkProcessor<WorkQueueBeanstalkHandler>>(provider, autostart);
				}

				@Override
				public Set<Dependency<?>> getDependencies() {
					return ImmutableSet.<Dependency<?>> of(Dependency.get(consumerServiceKey));
				}
			}).in(Singleton.class);

			final Key<ServiceActivator<BeanstalkProcessor<WorkQueueBeanstalkHandler>>> activatorKey = Key.get(activatorType, queue.bindingAnnotation);
			services.addBinding().to(activatorKey);
			activateOnStartup.addBinding().to(activatorKey);
		}

		for (final QueueConfiguration queue : redis) {
			final TypeLiteral<RedisProcessor<WorkQueueRedisHandler>> processorType = new TypeLiteral<RedisProcessor<WorkQueueRedisHandler>>(){};
			final TypeLiteral<ServiceActivator<RedisProcessor<WorkQueueRedisHandler>>> activatorType = new TypeLiteral<ServiceActivator<RedisProcessor<WorkQueueRedisHandler>>>(){};
			bind(WorkQueueRedisHandler.class).annotatedWith(queue.bindingAnnotation).toProvider(new ProviderWithDependencies<WorkQueueRedisHandler>() {
				private Provider<WorkDispatcher> dispatcherProvider = binder().getProvider(Key.get(WorkDispatcher.class, queue.bindingAnnotation));

				@Override
				public Set<Dependency<?>> getDependencies() {
					return ImmutableSet.<Dependency<?>> of(Dependency.get(Key.get(WorkDispatcher.class, queue.bindingAnnotation)));
				}

				@Override
				public WorkQueueRedisHandler get() {
					return new WorkQueueRedisHandler(dispatcherProvider.get());
				}
			});
			bind(processorType).annotatedWith(queue.bindingAnnotation).toProvider(new ProviderWithDependencies<RedisProcessor<WorkQueueRedisHandler>>() {
				private final Key<WorkQueueRedisHandler> handlerKey = Key.get(WorkQueueRedisHandler.class, queue.bindingAnnotation);
				private final Provider<WorkQueueRedisHandler> targetProvider = binder().getProvider(handlerKey);
				@Inject
				private Provider<Jedis> connectionProvider;
				@Inject
				private ActivityScopeControl scopeControl;

				@Override
				public Set<Dependency<?>> getDependencies() {
					return ImmutableSet.<Dependency<?>> builder()
							.addAll(Dependency.forInjectionPoints(InjectionPoint.forInstanceMethodsAndFields(getClass())))
							.add(Dependency.get(handlerKey))
							.build();
				}

				@Override
				public RedisProcessor<WorkQueueRedisHandler> get() {
					return new RedisProcessor<WorkQueueRedisHandler>(connectionProvider, queue.name, scopeControl, targetProvider);
				}
			});
			bind(activatorType).annotatedWith(queue.bindingAnnotation).toProvider(new ProviderWithDependencies<ServiceActivator<RedisProcessor<WorkQueueRedisHandler>>>() {
				private final Key<RedisProcessor<WorkQueueRedisHandler>> consumerServiceKey = Key.get(processorType, queue.bindingAnnotation);
				private final Provider<RedisProcessor<WorkQueueRedisHandler>> provider = binder().getProvider(consumerServiceKey);

				@Override
				public ServiceActivator<RedisProcessor<WorkQueueRedisHandler>> get() {
					return new ServiceActivator<RedisProcessor<WorkQueueRedisHandler>>(provider, autostart);
				}

				@Override
				public Set<Dependency<?>> getDependencies() {
					return ImmutableSet.<Dependency<?>> of(Dependency.get(consumerServiceKey));
				}
			}).in(Singleton.class);
			final Key<ServiceActivator<RedisProcessor<WorkQueueRedisHandler>>> activatorKey = Key.get(activatorType, queue.bindingAnnotation);
			services.addBinding().to(activatorKey);
			activateOnStartup.addBinding().to(activatorKey);
		}

		services.addBinding().to(PostgresqlDataSourceProviderService.class);
		bind(DataSource.class).toProvider(PostgresqlDataSourceProviderService.class);
		services.addBinding().to(SqlWorkQueue.Setup.class);
	}

	private static class QueueConfiguration {
		public final String name;
		public final Class<? extends WorkProcessor> processorClass;
		public final Annotation bindingAnnotation;

		public QueueConfiguration(String name, Class<? extends WorkProcessor> processorClass) {
			this.name = name;
			this.processorClass = processorClass;
			this.bindingAnnotation = Names.named(name);
		}
	}
}

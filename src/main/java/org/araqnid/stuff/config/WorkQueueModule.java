package org.araqnid.stuff.config;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.araqnid.stuff.ActivateOnStartup;
import org.araqnid.stuff.AppVersion;
import org.araqnid.stuff.HibernateService;
import org.araqnid.stuff.PostgresqlDataSourceProviderService;
import org.araqnid.stuff.SomeQueueProcessor;
import org.araqnid.stuff.messages.BeanstalkProcessor;
import org.araqnid.stuff.messages.RedisProcessor;
import org.araqnid.stuff.services.Activator;
import org.araqnid.stuff.services.ServiceActivator;
import org.araqnid.stuff.workqueue.HibernateWorkQueue;
import org.araqnid.stuff.workqueue.SqlWorkQueue;
import org.araqnid.stuff.workqueue.SqlWorkQueue.Accessor;
import org.araqnid.stuff.zedis.Zedis;
import org.araqnid.stuff.workqueue.WorkDispatcher;
import org.araqnid.stuff.workqueue.WorkProcessor;
import org.araqnid.stuff.workqueue.WorkQueue;
import org.araqnid.stuff.workqueue.WorkQueueBeanstalkHandler;
import org.araqnid.stuff.workqueue.WorkQueueRedisHandler;
import org.hibernate.SessionFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.ProviderWithDependencies;
import com.surftools.BeanstalkClient.Client;

public final class WorkQueueModule extends AbstractModule {
	private final Collection<QueueConfiguration> beanstalk = ImmutableSet.of(
			new QueueConfiguration("somequeue", SomeQueueProcessor.class),
			new QueueConfiguration("otherqueue", SomeQueueProcessor.class));
	private final Collection<QueueConfiguration> redis = ImmutableSet.of(
			new QueueConfiguration("thisqueue", SomeQueueProcessor.class),
			new QueueConfiguration("thatqueue", SomeQueueProcessor.class));
	private final boolean autostart = false;
	private boolean hibernate = true;

	@Override
	protected void configure() {
		Multibinder<Service> services = Multibinder.newSetBinder(binder(), Service.class);
		Multibinder<Activator> activateOnStartup = Multibinder.newSetBinder(binder(), Activator.class,
				ActivateOnStartup.OnStartup.class);

		for (final QueueConfiguration queue : Iterables.concat(beanstalk, redis)) {
			if (hibernate) {
				bind(WorkQueue.class).annotatedWith(queue.bindingAnnotation)
						.toProvider(new Provider<HibernateWorkQueue>() {
							@Inject
							private SessionFactory sessionFactory;

							@Override
							public HibernateWorkQueue get() {
								return new HibernateWorkQueue(queue.name, sessionFactory);
							}
						});
			}
			else {
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

					@Override
					public SqlWorkQueue get() {
						Accessor accessor = accessorProvider.get();
						return new SqlWorkQueue(queue.name, accessor, objectMapper, appVersion, instanceId, hostname);
					}
				});
			}

			bind(WorkDispatcher.class).annotatedWith(queue.bindingAnnotation)
					.toProvider(new ProviderWithDependencies<WorkDispatcher>() {
						private final Key<WorkQueue> queueKey = Key.get(WorkQueue.class, queue.bindingAnnotation);
						private final Provider<WorkQueue> workQueueProvider = binder().getProvider(queueKey);
						private final Provider<? extends WorkProcessor> processorProvider = binder()
								.getProvider(queue.processorClass);

						@Override
						public WorkDispatcher get() {
							WorkQueue workQueue = workQueueProvider.get();
							WorkProcessor queueProcessor = processorProvider.get();
							return new WorkDispatcher(workQueue, queueProcessor);
						}

						@Override
						public Set<Dependency<?>> getDependencies() {
							return ImmutableSet.<Dependency<?>> builder()
									.addAll(Dependency
											.forInjectionPoints(InjectionPoint.forInstanceMethodsAndFields(getClass())))
									.add(Dependency.get(queueKey)).add(Dependency.get(Key.get(queue.processorClass)))
									.build();
						}
					});
		}

		for (final QueueConfiguration queue : beanstalk) {
			final TypeLiteral<BeanstalkProcessor<WorkQueueBeanstalkHandler>> processorType = new TypeLiteral<BeanstalkProcessor<WorkQueueBeanstalkHandler>>() {
			};
			final TypeLiteral<ServiceActivator<BeanstalkProcessor<WorkQueueBeanstalkHandler>>> activatorType = new TypeLiteral<ServiceActivator<BeanstalkProcessor<WorkQueueBeanstalkHandler>>>() {
			};
			bind(WorkQueueBeanstalkHandler.class).annotatedWith(queue.bindingAnnotation)
					.toProvider(new ProviderWithDependencies<WorkQueueBeanstalkHandler>() {
						private Provider<WorkDispatcher> dispatcherProvider = binder()
								.getProvider(Key.get(WorkDispatcher.class, queue.bindingAnnotation));

						@Override
						public Set<Dependency<?>> getDependencies() {
							return ImmutableSet.<Dependency<?>> of(
									Dependency.get(Key.get(WorkDispatcher.class, queue.bindingAnnotation)));
						}

						@Override
						public WorkQueueBeanstalkHandler get() {
							return new WorkQueueBeanstalkHandler(dispatcherProvider.get());
						}
					});
			bind(processorType).annotatedWith(queue.bindingAnnotation)
					.toProvider(new ProviderWithDependencies<BeanstalkProcessor<WorkQueueBeanstalkHandler>>() {
						private final Key<WorkQueueBeanstalkHandler> handlerKey = Key
								.get(WorkQueueBeanstalkHandler.class, queue.bindingAnnotation);
						@Inject
						private Provider<Client> connectionProvider;
						private Provider<WorkQueueBeanstalkHandler> targetProvider = binder().getProvider(handlerKey);

						@Override
						public Set<Dependency<?>> getDependencies() {
							return ImmutableSet.<Dependency<?>> builder()
									.addAll(Dependency
											.forInjectionPoints(InjectionPoint.forInstanceMethodsAndFields(getClass())))
									.add(Dependency.get(handlerKey)).build();
						}

						@Override
						public BeanstalkProcessor<WorkQueueBeanstalkHandler> get() {
							return new BeanstalkProcessor<WorkQueueBeanstalkHandler>(connectionProvider, queue.name,
									targetProvider);
						}
					});
			bind(activatorType).annotatedWith(queue.bindingAnnotation).toProvider(
					new ProviderWithDependencies<ServiceActivator<BeanstalkProcessor<WorkQueueBeanstalkHandler>>>() {
						private final Key<BeanstalkProcessor<WorkQueueBeanstalkHandler>> consumerServiceKey = Key
								.get(processorType, queue.bindingAnnotation);
						private final Provider<BeanstalkProcessor<WorkQueueBeanstalkHandler>> provider = binder()
								.getProvider(consumerServiceKey);

						@Override
						public ServiceActivator<BeanstalkProcessor<WorkQueueBeanstalkHandler>> get() {
							return new ServiceActivator<BeanstalkProcessor<WorkQueueBeanstalkHandler>>(provider,
									autostart);
						}

						@Override
						public Set<Dependency<?>> getDependencies() {
							return ImmutableSet.<Dependency<?>> of(Dependency.get(consumerServiceKey));
						}
					}).in(Singleton.class);

			final Key<ServiceActivator<BeanstalkProcessor<WorkQueueBeanstalkHandler>>> activatorKey = Key
					.get(activatorType, queue.bindingAnnotation);
			services.addBinding().to(activatorKey);
			activateOnStartup.addBinding().to(activatorKey);
		}

		for (final QueueConfiguration queue : redis) {
			final TypeLiteral<ServiceActivator<RedisProcessor>> activatorType = new TypeLiteral<ServiceActivator<RedisProcessor>>() {
			};
			bind(WorkQueueRedisHandler.class).annotatedWith(queue.bindingAnnotation)
					.toProvider(new ProviderWithDependencies<WorkQueueRedisHandler>() {
						private Provider<WorkDispatcher> dispatcherProvider = binder()
								.getProvider(Key.get(WorkDispatcher.class, queue.bindingAnnotation));

						@Override
						public Set<Dependency<?>> getDependencies() {
							return ImmutableSet.<Dependency<?>> of(
									Dependency.get(Key.get(WorkDispatcher.class, queue.bindingAnnotation)));
						}

						@Override
						public WorkQueueRedisHandler get() {
							return new WorkQueueRedisHandler(dispatcherProvider.get());
						}
					});
			bind(RedisProcessor.class).annotatedWith(queue.bindingAnnotation)
					.toProvider(new ProviderWithDependencies<RedisProcessor>() {
						private final Key<WorkQueueRedisHandler> handlerKey = Key.get(WorkQueueRedisHandler.class,
								queue.bindingAnnotation);
						private final Provider<WorkQueueRedisHandler> targetProvider = binder().getProvider(handlerKey);
						@Inject
						private Provider<Zedis> connectionProvider;

						@Override
						public Set<Dependency<?>> getDependencies() {
							return ImmutableSet.<Dependency<?>> builder()
									.addAll(Dependency
											.forInjectionPoints(InjectionPoint.forInstanceMethodsAndFields(getClass())))
									.add(Dependency.get(handlerKey)).build();
						}

						@Override
						public RedisProcessor get() {
							return new RedisProcessor(connectionProvider, queue.name,
									data -> targetProvider.get().deliver(data));
						}
					});
			bind(activatorType).annotatedWith(queue.bindingAnnotation)
					.toProvider(new ProviderWithDependencies<ServiceActivator<RedisProcessor>>() {
						private final Key<RedisProcessor> consumerServiceKey = Key.get(RedisProcessor.class,
								queue.bindingAnnotation);
						private final Provider<RedisProcessor> provider = binder().getProvider(consumerServiceKey);

						@Override
						public ServiceActivator<RedisProcessor> get() {
							return new ServiceActivator<RedisProcessor>(provider, autostart);
						}

						@Override
						public Set<Dependency<?>> getDependencies() {
							return ImmutableSet.<Dependency<?>> of(Dependency.get(consumerServiceKey));
						}
					}).in(Singleton.class);
			final Key<ServiceActivator<RedisProcessor>> activatorKey = Key.get(activatorType, queue.bindingAnnotation);
			services.addBinding().to(activatorKey);
			activateOnStartup.addBinding().to(activatorKey);
		}

		services.addBinding().to(PostgresqlDataSourceProviderService.class);
		bind(DataSource.class).toProvider(PostgresqlDataSourceProviderService.class);
		// services.addBinding().to(SqlWorkQueue.Setup.class);
		services.addBinding().to(HibernateService.class).in(Singleton.class);
		bind(SessionFactory.class).toProvider(new Provider<SessionFactory>() {
			@Inject
			private HibernateService hibernateService;

			@Override
			public SessionFactory get() {
				return hibernateService.getProxy();
			}
		});
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

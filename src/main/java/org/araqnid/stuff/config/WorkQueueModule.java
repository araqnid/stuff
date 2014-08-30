package org.araqnid.stuff.config;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;

import javax.sql.DataSource;

import org.araqnid.stuff.ActivateOnStartup;
import org.araqnid.stuff.Activator;
import org.araqnid.stuff.BeanstalkProcessor;
import org.araqnid.stuff.PostgresqlDataSourceProviderService;
import org.araqnid.stuff.RedisProcessor;
import org.araqnid.stuff.ServiceActivator;
import org.araqnid.stuff.SomeQueueProcessor;
import org.araqnid.stuff.activity.ActivityScopeControl;
import org.araqnid.stuff.activity.RequestActivity;
import org.araqnid.stuff.workqueue.SqlWorkQueue;
import org.araqnid.stuff.workqueue.WorkDispatcher;
import org.araqnid.stuff.workqueue.WorkProcessor;
import org.araqnid.stuff.workqueue.WorkQueueBeanstalkHandler;
import org.araqnid.stuff.workqueue.WorkQueueRedisHandler;

import redis.clients.jedis.Jedis;

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

public final class WorkQueueModule extends AbstractModule {
	private static final TypeLiteral<ServiceActivator<BeanstalkProcessor>> BeanstalkProcessorActivator = new TypeLiteral<ServiceActivator<BeanstalkProcessor>>() {
	};
	private static final TypeLiteral<ServiceActivator<RedisProcessor>> RedisProcessorActivator = new TypeLiteral<ServiceActivator<RedisProcessor>>() {
	};
	private final Collection<QueueConfiguration> beanstalk = ImmutableSet.of(new QueueConfiguration("somequeue", 1,
			SomeQueueProcessor.class), new QueueConfiguration("otherqueue", 1, SomeQueueProcessor.class));
	private final Collection<QueueConfiguration> redis = ImmutableSet.of(new QueueConfiguration("thisqueue", 1,
			SomeQueueProcessor.class), new QueueConfiguration("thatqueue", 1, SomeQueueProcessor.class));
	private final boolean autostart = false;

	@Override
	protected void configure() {
		Multibinder<Service> services = Multibinder.newSetBinder(binder(), Service.class);
		Multibinder<Activator> activateOnStartup = Multibinder.newSetBinder(binder(), Activator.class,
				ActivateOnStartup.OnStartup.class);

		for (final QueueConfiguration queue : beanstalk) {
			final Key<WorkQueueBeanstalkHandler> handlerKey = Key.get(WorkQueueBeanstalkHandler.class,
					queue.bindingAnnotation);
			final Key<BeanstalkProcessor> consumerServiceKey = Key.get(BeanstalkProcessor.class,
					queue.bindingAnnotation);
			final Key<ServiceActivator<BeanstalkProcessor>> activatorKey = Key.get(BeanstalkProcessorActivator,
					queue.bindingAnnotation);
			bind(handlerKey).toProvider(new ProviderWithDependencies<WorkQueueBeanstalkHandler>() {
				@Inject
				private Provider<RequestActivity> requestActivityProvider;
				@Inject
				private DataSource dataSource;
				private Provider<? extends WorkProcessor> processorProvider = binder()
						.getProvider(queue.processorClass);

				@Override
				public Set<Dependency<?>> getDependencies() {
					return ImmutableSet.<Dependency<?>> of(Dependency.get(Key.get(RequestActivity.class)),
							Dependency.get(Key.get(DataSource.class)), Dependency.get(Key.get(queue.processorClass)));
				}

				@Override
				public WorkQueueBeanstalkHandler get() {
					RequestActivity requestActivity = requestActivityProvider.get();
					SqlWorkQueue queueImpl = new SqlWorkQueue(queue.name, requestActivity, dataSource);
					WorkDispatcher dispatcher = new WorkDispatcher(queueImpl, processorProvider.get(), requestActivity);
					WorkQueueBeanstalkHandler beanstalkTarget = new WorkQueueBeanstalkHandler(dispatcher);
					return beanstalkTarget;
				}
			});
			bind(consumerServiceKey).toProvider(new ProviderWithDependencies<BeanstalkProcessor>() {
				@Inject
				private Provider<Client> connectionProvider;
				@Inject
				private ActivityScopeControl scopeControl;
				private Provider<WorkQueueBeanstalkHandler> targetProvider = binder().getProvider(handlerKey);

				@Override
				public Set<Dependency<?>> getDependencies() {
					return ImmutableSet.<Dependency<?>> of(Dependency.get(Key.get(Client.class)),
							Dependency.get(Key.get(ActivityScopeControl.class)), Dependency.get(handlerKey));
				}

				@Override
				public BeanstalkProcessor get() {
					return new BeanstalkProcessor(connectionProvider, queue.name, queue.threads, scopeControl,
							targetProvider);
				}
			});
			bind(activatorKey).toProvider(new Provider<ServiceActivator<BeanstalkProcessor>>() {
				private Provider<BeanstalkProcessor> provider = binder().getProvider(consumerServiceKey);

				@Override
				public ServiceActivator<BeanstalkProcessor> get() {
					return new ServiceActivator<BeanstalkProcessor>(provider, autostart);
				}
			}).in(Singleton.class);
			services.addBinding().to(activatorKey);
			activateOnStartup.addBinding().to(activatorKey);
		}

		for (final QueueConfiguration queue : redis) {
			final Key<WorkQueueRedisHandler> handlerKey = Key.get(WorkQueueRedisHandler.class, queue.bindingAnnotation);
			final Key<RedisProcessor> consumerServiceKey = Key.get(RedisProcessor.class, queue.bindingAnnotation);
			final Key<ServiceActivator<RedisProcessor>> activatorKey = Key.get(RedisProcessorActivator,
					queue.bindingAnnotation);
			bind(handlerKey).toProvider(new ProviderWithDependencies<WorkQueueRedisHandler>() {
				@Inject
				private Provider<RequestActivity> requestActivityProvider;
				@Inject
				private DataSource dataSource;
				private Provider<? extends WorkProcessor> processorProvider = binder()
						.getProvider(queue.processorClass);

				@Override
				public Set<Dependency<?>> getDependencies() {
					return ImmutableSet.<Dependency<?>> of(Dependency.get(Key.get(RequestActivity.class)),
							Dependency.get(Key.get(DataSource.class)), Dependency.get(Key.get(queue.processorClass)));
				}

				@Override
				public WorkQueueRedisHandler get() {
					RequestActivity requestActivity = requestActivityProvider.get();
					SqlWorkQueue queueImpl = new SqlWorkQueue(queue.name, requestActivity, dataSource);
					WorkDispatcher dispatcher = new WorkDispatcher(queueImpl, processorProvider.get(), requestActivity);
					return new WorkQueueRedisHandler(dispatcher);
				}
			});
			bind(consumerServiceKey).toProvider(new ProviderWithDependencies<RedisProcessor>() {
				@Inject
				private Provider<Jedis> connectionProvider;
				@Inject
				private ActivityScopeControl scopeControl;
				private Provider<WorkQueueRedisHandler> targetProvider = binder().getProvider(handlerKey);

				@Override
				public Set<Dependency<?>> getDependencies() {
					return ImmutableSet.<Dependency<?>> of(Dependency.get(Key.get(Client.class)),
							Dependency.get(Key.get(ActivityScopeControl.class)), Dependency.get(handlerKey));
				}

				@Override
				public RedisProcessor get() {
					return new RedisProcessor(connectionProvider, queue.name, scopeControl, targetProvider);
				}
			});
			bind(activatorKey).toProvider(new Provider<ServiceActivator<RedisProcessor>>() {
				private Provider<RedisProcessor> provider = binder().getProvider(consumerServiceKey);

				@Override
				public ServiceActivator<RedisProcessor> get() {
					return new ServiceActivator<RedisProcessor>(provider, autostart);
				}
			}).in(Singleton.class);
			services.addBinding().to(activatorKey);
			activateOnStartup.addBinding().to(activatorKey);
		}

		services.addBinding().to(PostgresqlDataSourceProviderService.class);
		bind(DataSource.class).toProvider(PostgresqlDataSourceProviderService.class);
	}

	@Provides
	public Jedis jedis() {
		return new Jedis("localhost");
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

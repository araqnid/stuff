package org.araqnid.stuff.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.Set;

import org.araqnid.stuff.ActivateOnStartup;
import org.araqnid.stuff.Activator;
import org.araqnid.stuff.BeanstalkProcessor;
import org.araqnid.stuff.RedisEventLoader;
import org.araqnid.stuff.ServiceActivator;
import org.araqnid.stuff.SpooledEventHandler;
import org.araqnid.stuff.SpooledEventProcessor;
import org.araqnid.stuff.SpooledEventSpooler;
import org.araqnid.stuff.activity.ActivityScopeControl;

import redis.clients.jedis.Jedis;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.ProviderWithDependencies;
import com.google.inject.util.Types;
import com.surftools.BeanstalkClient.Client;

public class SpooledEventsModule extends AbstractModule {
	@Override
	protected void configure() {
		install(new SpooledQueueModule("spooledqueue"));
		activateServiceOnStartup(SpooledEventProcessor.class, Names.named("spooledqueue"));
	}

	private <S extends Service> void activateServiceOnStartup(Class<S> serviceClass, Annotation bindingAnnotation) {
		Multibinder<Service> services = Multibinder.newSetBinder(binder(), Service.class);
		Multibinder<Activator> activateOnStartup = Multibinder.newSetBinder(binder(), Activator.class,
				ActivateOnStartup.OnStartup.class);

		ParameterizedType activatorType = Types.newParameterizedType(ServiceActivator.class, serviceClass);
		final Key<S> serviceKey = Key.get(serviceClass, bindingAnnotation);
		@SuppressWarnings("unchecked")
		final Key<ServiceActivator<S>> activatorKey = (Key<ServiceActivator<S>>) Key.get(activatorType,
				bindingAnnotation);
		bind(activatorKey).toProvider(new ProviderWithDependencies<ServiceActivator<S>>() {
			private Provider<S> serviceProvider = binder().getProvider(serviceKey);

			@Override
			public ServiceActivator<S> get() {
				return new ServiceActivator<S>(serviceProvider, false);
			}

			@Override
			public Set<Dependency<?>> getDependencies() {
				return ImmutableSet.<Dependency<?>> of(Dependency.get(serviceKey));
			}
		}).in(Singleton.class);
		services.addBinding().to(activatorKey);
		activateOnStartup.addBinding().to(activatorKey);
	}

	public static class SpooledQueueModule extends PrivateModule {
		private final String queueName;

		public SpooledQueueModule(String queueName) {
			this.queueName = queueName;
		}

		@Override
		protected void configure() {
			bind(SpooledEventProcessor.class).annotatedWith(Names.named(queueName)).toProvider(
					new Provider<SpooledEventProcessor>() {
						@Inject
						private Provider<RedisEventLoader> loader;
						@Inject
						private Provider<BeanstalkProcessor> processor;

						@Override
						public SpooledEventProcessor get() {
							return new SpooledEventProcessor(loader.get(), processor.get());
						}
					});
			expose(Key.get(SpooledEventProcessor.class, Names.named(queueName)));
		}

		@Provides
		public RedisEventLoader loader(Provider<Jedis> redisProvider,
				RedisEventLoader.EventTarget eventTarget,
				ActivityScopeControl scopeControl) {
			return new RedisEventLoader(eventTarget, redisProvider, queueName + ".spool", scopeControl, 1000);
		}

		@Provides
		public BeanstalkProcessor processor(Provider<Client> connectionProvider,
				ActivityScopeControl scopeControl,
				Provider<BeanstalkProcessor.DeliveryTarget> deliveryTarget) {
			return new BeanstalkProcessor(connectionProvider, queueName + ".incoming", scopeControl, deliveryTarget);
		}

		@Provides
		public RedisEventLoader.EventTarget loaderEventTarget(final SpooledEventHandler handler) {
			return new RedisEventLoader.EventTarget() {
				@Override
				public void processEvent(String payload) {
					handler.handleEvent(payload);
				}
			};
		}

		@Provides
		public SpooledEventSpooler spooler(Provider<Jedis> redisProvider) {
			return new SpooledEventSpooler(redisProvider, queueName + ".spool");
		}

		@Provides
		public BeanstalkProcessor.DeliveryTarget processorDeliveryTarget(final SpooledEventHandler handler,
				final SpooledEventSpooler spooler) {
			return new BeanstalkProcessor.DeliveryTarget() {
				@Override
				public boolean deliver(byte[] data) {
					String string = new String(data, Charsets.UTF_8);
					spooler.spool(string);
					handler.handleEvent(string);
					return true;
				}
			};
		}
	}
}

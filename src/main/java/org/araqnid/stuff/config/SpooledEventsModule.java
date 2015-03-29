package org.araqnid.stuff.config;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.araqnid.stuff.ActivateOnStartup;
import org.araqnid.stuff.RedisEventLoader;
import org.araqnid.stuff.activity.ActivityScopeControl;
import org.araqnid.stuff.messages.BeanstalkProcessor;
import org.araqnid.stuff.messages.DispatchingMessageHandler;
import org.araqnid.stuff.messages.DispatchingMessageHandler.EventHandler;
import org.araqnid.stuff.messages.MessageHandler;
import org.araqnid.stuff.messages.RedisEventSpooler;
import org.araqnid.stuff.messages.SomeEventHandler;
import org.araqnid.stuff.services.Activator;
import org.araqnid.stuff.services.ServiceActivator;
import org.araqnid.stuff.services.SpooledEventProcessor;

import redis.clients.jedis.Jedis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.ProviderWithDependencies;
import com.google.inject.util.Types;
import com.surftools.BeanstalkClient.Client;

public class SpooledEventsModule extends AbstractModule {
	@Override
	protected void configure() {
		registerQueueProcessor("spooledqueue", Key.get(DispatchingMessageHandler.class));
	}

	@Provides
	public DispatchingMessageHandler dispatchingMessageHandler(ObjectMapper objectMapper,
			SomeEventHandler someEventHandler) {
		ImmutableMap<String, EventHandler<?>> eventTypeMap = ImmutableMap
				.<String, DispatchingMessageHandler.EventHandler<?>> of("test", someEventHandler);
		return new DispatchingMessageHandler(objectMapper, eventTypeMap);
	}

	private <T extends MessageHandler> void registerQueueProcessor(String queueName, Key<T> handlerKey) {
		SpooledQueueProvider<T> spooledQueueProvider = new SpooledQueueProvider<T>(false, queueName + ".incoming",
				queueName + ".spool", 1000, binder().getProvider(handlerKey), Dependency.get(handlerKey));
		bindLateService(SpooledEventProcessor.class, Names.named(queueName), spooledQueueProvider);
	}

	public static class SpooledQueueProvider<T extends MessageHandler> implements
			ProviderWithDependencies<ServiceActivator<SpooledEventProcessor>> {
		private final boolean activateOnStartup;
		private final String tubeName;
		private final String redisKeyName;
		private final int redisLoaderPageSize;
		private final Provider<T> messageHandlerProvider;
		private final Dependency<T> messageHandlerDependency;
		@Inject
		private ActivityScopeControl scopeControl;
		@Inject
		private Provider<Jedis> redisConnectionProvider;
		@Inject
		private Provider<Client> beanstalkConnectionProvider;

		public SpooledQueueProvider(boolean activateOnStartup,
				String tubeName,
				String redisKeyName,
				int redisLoaderPageSize,
				Provider<T> messageHandlerProvider,
				Dependency<T> messageHandlerDependency) {
			this.activateOnStartup = activateOnStartup;
			this.tubeName = tubeName;
			this.redisKeyName = redisKeyName;
			this.redisLoaderPageSize = redisLoaderPageSize;
			this.messageHandlerProvider = messageHandlerProvider;
			this.messageHandlerDependency = messageHandlerDependency;
		}

		@Override
		public Set<Dependency<?>> getDependencies() {
			Set<Dependency<?>> staticDependencies = Dependency.forInjectionPoints(InjectionPoint
					.forInstanceMethodsAndFields(SpooledQueueProvider.class));
			Set<Dependency<?>> dynamicDependencies = ImmutableSet.<Dependency<?>> of(messageHandlerDependency);
			return ImmutableSet.copyOf(Sets.union(staticDependencies, dynamicDependencies));
		}

		@Override
		public ServiceActivator<SpooledEventProcessor> get() {
			return new ServiceActivator<>(new Provider<SpooledEventProcessor>() {
				@Override
				public SpooledEventProcessor get() {
					return spooledEventProcessor();
				}
			}, activateOnStartup);
		}

		private SpooledEventProcessor spooledEventProcessor() {
			return new SpooledEventProcessor(loader(), processor());
		}

		private Service loader() {
			return new RedisEventLoader(redisEventTarget(), redisConnectionProvider, redisKeyName, scopeControl,
					redisLoaderPageSize);
		}

		private Service processor() {
			return new BeanstalkProcessor<BeanstalkSpoolingMessageHandlerAdaptor<T>>(beanstalkConnectionProvider,
					tubeName, scopeControl, new Provider<BeanstalkSpoolingMessageHandlerAdaptor<T>>() {
						@Override
						public BeanstalkSpoolingMessageHandlerAdaptor<T> get() {
							return new BeanstalkSpoolingMessageHandlerAdaptor<T>(messageHandler(), spooler());
						}
					});
		}

		private T messageHandler() {
			return messageHandlerProvider.get();
		}

		private RedisEventLoader.EventTarget redisEventTarget() {
			return new RedisMessageHandlerAdaptor<T>(messageHandler());
		}

		private RedisEventSpooler spooler() {
			return new RedisEventSpooler(redisConnectionProvider, redisKeyName);
		}
	}

	private <S extends Service> void bindLateService(Class<S> serviceClass,
			Annotation bindingAnnotation,
			Provider<ServiceActivator<S>> provider) {
		@SuppressWarnings("unchecked")
		Key<ServiceActivator<S>> key = (Key<ServiceActivator<S>>) Key.get(
				Types.newParameterizedType(ServiceActivator.class, serviceClass), bindingAnnotation);
		bind(key).toProvider(provider).in(Singleton.class);
		services().addBinding().to(key);
		activateOnStartup().addBinding().to(key);
	}

	private Multibinder<Activator> activateOnStartup() {
		return Multibinder.newSetBinder(binder(), Activator.class, ActivateOnStartup.OnStartup.class);
	}

	private Multibinder<Service> services() {
		return Multibinder.newSetBinder(binder(), Service.class);
	}

	public static class RedisMessageHandlerAdaptor<T extends MessageHandler> implements RedisEventLoader.EventTarget {
		private final T handler;

		public RedisMessageHandlerAdaptor(T handler) {
			this.handler = handler;
		}

		@Override
		public void processEvent(String payload) {
			handler.handleMessage(payload);
		}
	}

	public static class BeanstalkSpoolingMessageHandlerAdaptor<T extends MessageHandler> implements
			BeanstalkProcessor.DeliveryTarget {
		private final T handler;
		private final RedisEventSpooler spooler;

		public BeanstalkSpoolingMessageHandlerAdaptor(T handler, RedisEventSpooler spooler) {
			this.handler = handler;
			this.spooler = spooler;
		}

		@Override
		public boolean deliver(byte[] data) {
			String string = new String(data, Charsets.UTF_8);
			spooler.spool(string);
			handler.handleMessage(string);
			return true;
		}
	}
}

package org.araqnid.stuff.config;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Function;
import javax.inject.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Reflection;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.ProviderWithDependencies;
import org.araqnid.stuff.HelloResource;
import org.araqnid.stuff.InfoResources;
import org.araqnid.stuff.MerlotResources;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.FilterDispatcher;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public final class ResteasyModule extends AbstractModule {
	@VisibleForTesting
	public static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new GuavaModule())
			.registerModule(new Jdk8Module())
			.registerModule(new AfterburnerModule())
			.registerModule(new JavaTimeModule())
			.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	@VisibleForTesting
	public static final XmlMapper XML_OBJECT_MAPPER;

	static {
		XML_OBJECT_MAPPER = new XmlMapper();

		XML_OBJECT_MAPPER.registerModule(new GuavaModule())
				.registerModule(new Jdk8Module())
				.registerModule(new AfterburnerModule())
				.registerModule(new JavaTimeModule())
				.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		JaxbAnnotationIntrospector jaxbIntrospector = new JaxbAnnotationIntrospector(XML_OBJECT_MAPPER.getTypeFactory());
		AnnotationIntrospectorPair ser = new AnnotationIntrospectorPair(jaxbIntrospector, XML_OBJECT_MAPPER
				.getSerializationConfig().getAnnotationIntrospector());
		AnnotationIntrospectorPair deser = new AnnotationIntrospectorPair(jaxbIntrospector, XML_OBJECT_MAPPER
				.getDeserializationConfig().getAnnotationIntrospector());
		XML_OBJECT_MAPPER.setAnnotationIntrospectors(ser, deser);
	}

	@Override
	protected void configure() {
		bind(HelloResource.class);
		bind(InfoResources.class);
		bind(MerlotResources.class);

		install(new GenericProviders(FilterDispatcher.class));
	}

	@Provides
	public JacksonJsonProvider jacksonJson() {
		return new JacksonJsonProvider(JSON_OBJECT_MAPPER);
	}

	@Provides
	public JacksonXMLProvider jacksonXml() {
		return new JacksonXMLProvider(XML_OBJECT_MAPPER);
	}

	public static class GenericProviders extends AbstractModule {
		private final Class<?> dispatcherSource;

		GenericProviders(Class<?> dispatcherSource) {
			this.dispatcherSource = dispatcherSource;
		}

		@Override
		protected void configure() {
			if (FilterDispatcher.class.isAssignableFrom(dispatcherSource)) {
				//noinspection unchecked
				Key<? extends FilterDispatcher> key = Key.get((Class<FilterDispatcher>) dispatcherSource);
				bind(Dispatcher.class).toProvider(new DispatcherSource<>(key, FilterDispatcher::getDispatcher));
			}
			else if (HttpServletDispatcher.class.isAssignableFrom(dispatcherSource)) {
				//noinspection unchecked
				Key<? extends HttpServletDispatcher> key = Key.get((Class<HttpServletDispatcher>) dispatcherSource);
				bind(Dispatcher.class).toProvider(new DispatcherSource<>(key, HttpServletDispatcher::getDispatcher));
			}
			else {
				throw new IllegalStateException("Don't know how to get Dispatcher from " + dispatcherSource);
			}

			bindResteasyContextData(javax.ws.rs.core.Request.class);
			bindResteasyContextData(javax.ws.rs.core.HttpHeaders.class);
			bindResteasyContextData(javax.ws.rs.core.UriInfo.class);
			bindResteasyContextData(javax.ws.rs.core.SecurityContext.class);
		}

		@Provides
		public Registry dispatcher(Dispatcher dispatcher) {
			return dispatcher.getRegistry();
		}

		@Provides
		public ResteasyProviderFactory providerFactory(Dispatcher dispatcher) {
			return dispatcher.getProviderFactory();
		}

		private <T> void bindResteasyContextData(Class<T> clazz) {
			bind(clazz).toInstance(Reflection.newProxy(clazz, new AbstractInvocationHandler() {
				@Override
				protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {
					T target = ResteasyProviderFactory.getContextData(clazz);
					return method.invoke(target, args);
				}

				@Override
				public String toString() {
					return "ResteasyContextData proxy for " + clazz.getName();
				}
			}));
		}

		private class DispatcherSource<T> implements ProviderWithDependencies<Dispatcher> {
			private final Provider<? extends T> provider;
			private final Function<? super T, ? extends Dispatcher> extract;
			private final Set<Dependency<?>> dependencies;

			public DispatcherSource(Key<T> key, Function<? super T, ? extends Dispatcher> extract) {
				this.provider = binder().getProvider(key);
				this.dependencies = ImmutableSet.of(Dependency.get(key));
				this.extract = extract;
			}

			@Override
			public Dispatcher get() {
				return extract.apply(provider.get());
			}

			@Override
			public Set<Dependency<?>> getDependencies() {
				return dependencies;
			}
		}
	}
}

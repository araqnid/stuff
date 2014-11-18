package org.araqnid.stuff.config;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.guice.GuiceAnnotationIntrospector;
import com.fasterxml.jackson.module.guice.GuiceInjectableValues;
import com.google.inject.Exposed;
import com.google.inject.Injector;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class JacksonModule extends PrivateModule {
	@Override
	protected void configure() {
		Multibinder<com.fasterxml.jackson.databind.Module> jacksonModules = Multibinder.newSetBinder(binder(),
				com.fasterxml.jackson.databind.Module.class);
		jacksonModules.addBinding().to(JodaModule.class);
		jacksonModules.addBinding().to(GuavaModule.class);
		jacksonModules.addBinding().to(NamingJacksonModule.class);
	}

	@Provides
	@Singleton
	@Exposed
	public ObjectMapper objectMapper(Set<com.fasterxml.jackson.databind.Module> modules, Injector injector) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModules(modules);
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

		// copied from ObjectMapperModule.ObjectMapperProvider
		final GuiceAnnotationIntrospector guiceIntrospector = new GuiceAnnotationIntrospector();
		mapper.setInjectableValues(new GuiceInjectableValues(injector));
		mapper.setAnnotationIntrospectors(new AnnotationIntrospectorPair(guiceIntrospector, mapper
				.getSerializationConfig().getAnnotationIntrospector()), new AnnotationIntrospectorPair(
				guiceIntrospector, mapper.getDeserializationConfig().getAnnotationIntrospector()));

		return mapper;
	}
}

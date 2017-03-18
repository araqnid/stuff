package org.araqnid.stuff.config;

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
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.araqnid.stuff.HelloResource;
import org.araqnid.stuff.InfoResources;
import org.araqnid.stuff.MerlotResources;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
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
	}

	@Provides
	public JacksonJsonProvider jacksonJson() {
		return new JacksonJsonProvider(JSON_OBJECT_MAPPER);
	}

	@Provides
	public JacksonXMLProvider jacksonXml() {
		return new JacksonXMLProvider(XML_OBJECT_MAPPER);
	}

	@Provides
	public Dispatcher dispatcher(HttpServlet30Dispatcher servlet) {
		return servlet.getDispatcher();
	}

	@Provides
	public Registry dispatcher(Dispatcher dispatcher) {
		return dispatcher.getRegistry();
	}

	@Provides
	public ResteasyProviderFactory providerFactory(Dispatcher dispatcher) {
		return dispatcher.getProviderFactory();
	}

	@Provides
	public javax.ws.rs.core.Request request() {
		return ResteasyProviderFactory.getContextData(javax.ws.rs.core.Request.class);
	}

	@Provides
	public javax.ws.rs.core.HttpHeaders httpHeaders() {
		return ResteasyProviderFactory.getContextData(javax.ws.rs.core.HttpHeaders.class);
	}

	@Provides
	public javax.ws.rs.core.UriInfo uriInfo() {
		return ResteasyProviderFactory.getContextData(javax.ws.rs.core.UriInfo.class);
	}

	@Provides
	public javax.ws.rs.core.SecurityContext securityContext() {
		return ResteasyProviderFactory.getContextData(javax.ws.rs.core.SecurityContext.class);
	}
}

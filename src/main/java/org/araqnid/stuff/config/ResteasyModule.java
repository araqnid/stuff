package org.araqnid.stuff.config;

import javax.inject.Inject;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.araqnid.stuff.HelloResource;
import org.araqnid.stuff.InfoResources;
import org.araqnid.stuff.MerlotResources;
import org.araqnid.stuff.mvc.HelloWorldController;
import org.araqnid.stuff.mvc.JspViewRenderer;
import org.araqnid.stuff.resteasy.ResteasyJackson2Provider;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.jaxrs.xml.JacksonJaxbXMLProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public final class ResteasyModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(HelloResource.class);
		bind(InfoResources.class);
		bind(MerlotResources.class);
		bind(HelloWorldController.class);
		bind(JacksonContextResolver.class);
		bind(JacksonXmlContextResolver.class);
		bind(JspViewRenderer.class);
		bind(ResteasyJackson2Provider.class);
		bind(JacksonJaxbXMLProvider.class);
		bindConstant().annotatedWith(MvcPathPattern.class).to("/WEB-INF/mvc/%s.jsp");
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

	@Provider
	public static class JacksonContextResolver implements ContextResolver<ObjectMapper> {
		private final javax.inject.Provider<ObjectMapper> objectMapper;

		@Inject
		public JacksonContextResolver(javax.inject.Provider<ObjectMapper> objectMapper) {
			this.objectMapper = objectMapper;
		}

		@Override
		public ObjectMapper getContext(Class<?> type) {
			return objectMapper.get();
		}
	}

	@Provider
	public static class JacksonXmlContextResolver implements ContextResolver<XmlMapper> {
		private final javax.inject.Provider<XmlMapper> objectMapper;

		@Inject
		public JacksonXmlContextResolver(javax.inject.Provider<XmlMapper> objectMapper) {
			this.objectMapper = objectMapper;
		}

		@Override
		public XmlMapper getContext(Class<?> type) {
			return objectMapper.get();
		}
	}
}

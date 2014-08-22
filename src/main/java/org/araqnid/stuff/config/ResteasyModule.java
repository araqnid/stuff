package org.araqnid.stuff.config;

import org.araqnid.stuff.HelloResource;
import org.araqnid.stuff.InfoResources;
import org.araqnid.stuff.MerlotResources;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public final class ResteasyModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(HelloResource.class);
		bind(InfoResources.class);
		bind(MerlotResources.class);
	}

	@Provides
	public Dispatcher dispatcher(HttpServletDispatcher servlet) {
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
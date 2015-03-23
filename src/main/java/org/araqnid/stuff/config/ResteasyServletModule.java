package org.araqnid.stuff.config;

import org.araqnid.stuff.ServerIdentityFilter;
import org.araqnid.stuff.activity.RequestActivityFilter;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;

public final class ResteasyServletModule extends ServletModule {
	@Override
	protected void configureServlets() {
		bind(HttpServlet30Dispatcher.class).in(Singleton.class);
		serve("/*").with(HttpServlet30Dispatcher.class);
		filter("/*").through(RequestActivityFilter.class);
		filter("/*").through(ServerIdentityFilter.class);
	}
}

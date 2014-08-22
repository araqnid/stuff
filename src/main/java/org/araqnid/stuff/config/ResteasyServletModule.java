package org.araqnid.stuff.config;

import org.araqnid.stuff.ServerIdentityFilter;
import org.araqnid.stuff.activity.RequestActivityFilter;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;

public final class ResteasyServletModule extends ServletModule {
	@Override
	protected void configureServlets() {
		bind(HttpServletDispatcher.class).in(Singleton.class);
		serve("/*").with(HttpServletDispatcher.class);
		filter("/*").through(RequestActivityFilter.class);
		filter("/*").through(ServerIdentityFilter.class);
	}
}
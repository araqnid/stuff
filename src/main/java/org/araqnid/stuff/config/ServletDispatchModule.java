package org.araqnid.stuff.config;

import org.araqnid.stuff.RootServlet;
import org.araqnid.stuff.ServerIdentityFilter;
import org.araqnid.stuff.activity.ActivityFilter;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

import com.google.inject.servlet.ServletModule;

public class ServletDispatchModule extends ServletModule {
	@Override
	protected void configureServlets() {
		serve("/").with(RootServlet.class);
		serve("/_api/*").with(HttpServlet30Dispatcher.class);
		serve("/mvc/*").with(HttpServlet30Dispatcher.class);
		serve("/hello/*").with(HttpServlet30Dispatcher.class);
		filter("/*").through(ServerIdentityFilter.class);
		filter("/*").through(ActivityFilter.class);
	}
}

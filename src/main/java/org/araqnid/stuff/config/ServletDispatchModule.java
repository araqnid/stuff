package org.araqnid.stuff.config;

import com.google.inject.servlet.ServletModule;
import org.araqnid.stuff.ServerIdentityFilter;
import org.araqnid.stuff.activity.ActivityFilter;
import org.jboss.resteasy.plugins.server.servlet.Filter30Dispatcher;

public class ServletDispatchModule extends ServletModule {
	@Override
	protected void configureServlets() {
		filter("/*").through(Filter30Dispatcher.class);
		filter("/*").through(ServerIdentityFilter.class);
		filter("/*").through(ActivityFilter.class);
	}
}

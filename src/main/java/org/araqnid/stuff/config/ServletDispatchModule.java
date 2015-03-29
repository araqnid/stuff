package org.araqnid.stuff.config;

import org.apache.jasper.servlet.JspServlet;
import org.araqnid.stuff.RootServlet;
import org.araqnid.stuff.ServerIdentityFilter;
import org.araqnid.stuff.activity.RequestActivityFilter;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

import com.google.inject.servlet.ServletModule;

public class ServletDispatchModule extends ServletModule {
	@Override
	protected void configureServlets() {
		serve("/").with(RootServlet.class);
		serve("*.jsp").with(JspServlet.class);
		serve("/_api/*").with(HttpServlet30Dispatcher.class);
		serve("/mvc/*").with(HttpServlet30Dispatcher.class);
		filter("/*").through(RequestActivityFilter.class);
		filter("/*").through(ServerIdentityFilter.class);
	}
}

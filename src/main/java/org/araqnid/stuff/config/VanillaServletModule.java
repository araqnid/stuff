package org.araqnid.stuff.config;

import org.araqnid.stuff.RootServlet;
import org.araqnid.stuff.ServerIdentityFilter;
import org.araqnid.stuff.activity.RequestActivityFilter;

import com.google.inject.servlet.ServletModule;

public final class VanillaServletModule extends ServletModule {
	@Override
	protected void configureServlets() {
		serve("/").with(RootServlet.class);
		filter("/*").through(RequestActivityFilter.class);
		filter("/*").through(ServerIdentityFilter.class);
	}
}
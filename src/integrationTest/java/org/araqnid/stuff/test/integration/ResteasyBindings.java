package org.araqnid.stuff.test.integration;

import java.util.List;
import javax.servlet.ServletContext;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;

public class ResteasyBindings extends GuiceResteasyBootstrapServletContextListener {
	private final Module module;

	ResteasyBindings(Module module) {
		this.module = module;
	}

	@Override
	protected List<? extends Module> getModules(ServletContext context) {
		return ImmutableList.of(module);
	}
}

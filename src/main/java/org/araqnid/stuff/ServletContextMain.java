package org.araqnid.stuff;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.araqnid.stuff.config.EmbeddedWebappConfig;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

public class ServletContextMain implements ServletContextListener {
	private Injector injector;
	private GuiceResteasyBootstrapServletContextListener delegate;
	private AppServicesManager servicesManager;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		injector = Guice.createInjector(Stage.PRODUCTION, 
				new EmbeddedWebappConfig(sce.getServletContext()));
		delegate = injector.getInstance(GuiceResteasyBootstrapServletContextListener.class);
		delegate.contextInitialized(sce);
		servicesManager = injector.getInstance(AppServicesManager.class);
		servicesManager.start();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		if (servicesManager != null) servicesManager.stop();
		if (delegate != null) delegate.contextDestroyed(sce);
		injector = null;
	}
}

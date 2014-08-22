package org.araqnid.stuff;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.araqnid.stuff.config.EmbeddedWebappConfig;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

public class ServletContextMain implements ServletContextListener {
	private static final Logger LOG = LoggerFactory.getLogger(ServletContextMain.class);
	private Injector injector;
	private GuiceResteasyBootstrapServletContextListener delegate;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		LOG.info("Initialising context: {}", sce.getServletContext().getContextPath());
		injector = Guice.createInjector(Stage.PRODUCTION, 
				new EmbeddedWebappConfig());
		delegate = injector.getInstance(GuiceResteasyBootstrapServletContextListener.class);
		delegate.contextInitialized(sce);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		LOG.info("Destroying context: {}", sce.getServletContext().getContextPath());
		if (delegate != null) delegate.contextDestroyed(sce);
		injector = null;
	}
}

package org.araqnid.stuff;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.araqnid.stuff.activity.ActivityScope;
import org.araqnid.stuff.config.AppConfig;
import org.araqnid.stuff.config.AppConfig.ResteasyModule;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class ServletContextMain implements ServletContextListener {
	private static final Logger LOG = LoggerFactory.getLogger(ServletContextMain.class);
	private Injector injector;
	private GuiceResteasyBootstrapServletContextListener delegate;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		LOG.info("Initialising context: {}", sce.getServletContext().getContextPath());
		injector = Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
			@Override
			protected void configure() {
				bindConstant().annotatedWith(Names.named("http_port")).to(0);
				Multibinder.newSetBinder(binder(), ScheduledJobController.JobDefinition.class);
				bind(GuiceResteasyBootstrapServletContextListener.class).toInstance(new GuiceResteasyBootstrapServletContextListener() {
					@Override
					protected List<? extends Module> getModules(ServletContext context) {
						return ImmutableList.of(new ResteasyModule());
					}
				});
			}
		}, new AppConfig.CoreModule(), new ActivityScope.Module(), new AppConfig.SynchronousActivityEventsModule(),
				new AppConfig.JettyModule.ResteasyContextModule.WebModule());
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

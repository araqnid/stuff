package org.araqnid.stuff;

import java.util.Set;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.araqnid.stuff.config.EmbeddedWebappConfig;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;

public class ServletContextMain implements ServletContextListener {
	private static final Key<Set<ServletContextListener>> CONTEXT_LISTENERS = Key.get(new TypeLiteral<Set<ServletContextListener>>() {});
	private Injector injector;
	private Set<ServletContextListener> delegates;
	private AppServicesManager servicesManager;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		injector = Guice.createInjector(Stage.PRODUCTION, new EmbeddedWebappConfig(sce.getServletContext()));
		delegates = injector.getInstance(CONTEXT_LISTENERS);
		for (ServletContextListener delegate : delegates) {
			delegate.contextInitialized(sce);
		}
		servicesManager = injector.getInstance(AppServicesManager.class);
		servicesManager.start();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		if (injector == null) return;
		if (delegates != null) {
			for (ServletContextListener delegate : delegates) {
				delegate.contextDestroyed(sce);
			}
		}
		if (servicesManager != null) servicesManager.stop();
	}
}

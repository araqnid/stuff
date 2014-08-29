package org.araqnid.stuff;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.araqnid.stuff.config.EmbeddedWebappConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;

public class ServletContextMain implements ServletContextListener {
	private static final Logger LOG = LoggerFactory.getLogger(ServletContextMain.class);
	private static final Key<Set<ServletContextListener>> CONTEXT_LISTENERS = Key
			.get(new TypeLiteral<Set<ServletContextListener>>() {
			});
	private Injector injector;
	private Set<ServletContextListener> delegates;
	private ServiceManager serviceManager;
	private AppLifecycleEvent lifecycleEvents;

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		injector = Guice.createInjector(Stage.PRODUCTION, new EmbeddedWebappConfig(sce.getServletContext()));
		delegates = injector.getInstance(CONTEXT_LISTENERS);
		for (ServletContextListener delegate : delegates) {
			delegate.contextInitialized(sce);
		}
		final SettableFuture<Boolean> completion = SettableFuture.create();
		serviceManager = injector.getInstance(ServiceManager.class);
		lifecycleEvents = injector.getInstance(AppLifecycleEvent.class);
		serviceManager.addListener(new ServiceManager.Listener() {
			@Override
			public void healthy() {
				lifecycleEvents.started();
				completion.set(true);
			}

			@Override
			public void failure(Service service) {
				LOG.error("Failed to start service: {}", service);
				completion.set(false);
			}

			@Override
			public void stopped() {
				lifecycleEvents.stopped();
			}
		});
		lifecycleEvents.starting();
		serviceManager.startAsync();
		try {
			if (!completion.get()) throw new RuntimeException("Application services did not all start");
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Failure waiting for completion of startup", e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		if (injector == null) return;
		if (delegates != null) {
			for (ServletContextListener delegate : delegates) {
				delegate.contextDestroyed(sce);
			}
		}
		if (serviceManager != null) {
			lifecycleEvents.stopping();
			try {
				serviceManager.stopAsync().awaitStopped(30, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				LOG.error("Timeout waiting for services to stop", e);
			}
		}
	}
}

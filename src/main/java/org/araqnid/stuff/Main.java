package org.araqnid.stuff;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.araqnid.stuff.config.StandaloneAppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

public class Main {
	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		final Injector injector = Guice.createInjector(Stage.PRODUCTION, new StandaloneAppConfig());
		final ServiceManager serviceManager = injector.getInstance(ServiceManager.class);
		final AppLifecycleEvent lifecycleEvents = injector.getInstance(AppLifecycleEvent.class);
		serviceManager.addListener(new ServiceManager.Listener() {
			@Override
			public void healthy() {
				lifecycleEvents.started();
			}

			@Override
			public void failure(Service service) {
				LOG.error("Failed to start service: {}", service);
				System.exit(1);
			}

			@Override
			public void stopped() {
				lifecycleEvents.stopped();
			}
		});
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				lifecycleEvents.stopping();
				try {
					serviceManager.stopAsync().awaitStopped(30, TimeUnit.SECONDS);
				} catch (TimeoutException e) {
					LOG.warn("Timeout waiting for shutdown to complete", e);
				}
			}
		}, "GracefulShutdownThread"));
		lifecycleEvents.starting();
		serviceManager.startAsync();
	}
}

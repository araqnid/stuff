package org.araqnid.stuff;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import org.araqnid.stuff.config.AppModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		final Injector injector = Guice.createInjector(Stage.PRODUCTION, new AppModule());
		final ServiceManager serviceManager = injector.getInstance(ServiceManager.class);
		serviceManager.addListener(new ServiceManager.Listener() {
			@Override
			public void failure(Service service) {
				LOG.error("Failed to start service: {}", service, service.failureCause());
				System.exit(1);
			}
		});
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				serviceManager.stopAsync().awaitStopped(30, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				LOG.warn("Timeout waiting for shutdown to complete", e);
			}
		}, "shutdown"));
		serviceManager.startAsync();
	}
}

package org.araqnid.stuff;

import org.araqnid.stuff.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

public class Main {
	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		final Injector injector = Guice.createInjector(Stage.PRODUCTION, new AppConfig());
		final AppServicesManager servicesManager = injector.getInstance(AppServicesManager.class);
		try {
			LOG.info("Starting app services");
			servicesManager.start();
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					LOG.info("Starting graceful shutdown");
					servicesManager.stop();
				}
			}, "GracefulShutdownThread"));
		} catch (Exception e) {
			LOG.error("Exception starting app", e);
			System.exit(1);
		}
	}
}

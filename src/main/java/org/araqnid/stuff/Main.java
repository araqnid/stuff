package org.araqnid.stuff;

import org.araqnid.stuff.config.AppConfig;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

public class Main {
	public static void main(String[] args) {
		final Injector injector = Guice.createInjector(Stage.PRODUCTION, new AppConfig());
		final AppServicesManager servicesManager = injector.getInstance(AppServicesManager.class);
		servicesManager.start();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				servicesManager.stop();
			}
		}, "GracefulShutdownThread"));
	}
}

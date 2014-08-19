package org.araqnid.stuff.config;

import java.util.Random;

import org.araqnid.stuff.config.AppConfig;
import org.eclipse.jetty.server.Connector;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

public class AppConfigTest {
	@Test
	public void injector_can_be_built_from_app_config() {
		Guice.createInjector(Stage.PRODUCTION, new AppConfig(ImmutableMap.<String, String> of()));
	}

	@Test
	public void jetty_connector_uses_default_port_if_no_environment_setting() {
		Injector injector = Guice.createInjector(new AppConfig(ImmutableMap.<String, String> of()));
		Connector connector = injector.getInstance(Connector.class);
		Assert.assertEquals(61000, connector.getPort());
	}

	@Test
	public void jetty_connector_takes_port_from_environment() {
		int port = new Random().nextInt(0xffff);
		Injector injector = Guice.createInjector(new AppConfig(ImmutableMap.<String, String> of("PORT",
				String.valueOf(port))));
		Connector connector = injector.getInstance(Connector.class);
		Assert.assertEquals(port, connector.getPort());
	}
}

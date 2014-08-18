package org.araqnid.stuff;

import org.araqnid.stuff.config.AppConfig;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

public class BrowserTestFrame {
	public WebDriver driver;
	public Injector injector;
	public Server server;
	public int localPort;
	public String baseUri;

	public void setUp() throws Exception {
		Module module = Modules.override(new AppConfig()).with(new AbstractModule() {
			@Override
			protected void configure() {
			}

			@Provides
			@Singleton
			public Connector connector() {
				return new SelectChannelConnector();
			}
		});
		injector = Guice.createInjector(module);
		server = injector.getInstance(Server.class);
		server.start();
		localPort = injector.getInstance(Connector.class).getLocalPort();
		baseUri = "http://localhost:" + localPort + "/";
		driver = new FirefoxDriver();
	}

	public void tearDown() throws Exception {
		if (server != null) server.stop();
		if (driver != null) driver.quit();
	}

	public WebDriverWait waitFor(int timeout) {
		return new WebDriverWait(driver, timeout);
	}
}


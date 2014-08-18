package org.araqnid.stuff;

import org.araqnid.stuff.config.AppConfig;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

public class DefaultPageTest {
	private WebDriver driver;
	private Injector injector;
	private Server server;
	private int localPort;
	private String baseUri;

	@Before
	public void setUpApplication() throws Exception {
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
	}

	@Before
	public void setUpSelenium() throws Exception {
		driver = new FirefoxDriver();
	}

	@After
	public void tearDownApplication() throws Exception {
		if (server != null) server.stop();
	}

	@After
	public void tearDownSelenium() throws Exception {
		if (driver != null) driver.quit();
	}

	@Test
	public void shows_main_page_with_application_version() {
		driver.get(baseUri);

		(new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
			@Override
			public Boolean apply(WebDriver input) {
				WebElement elt = input.findElement(By.cssSelector("#info .info-version"));
				return !elt.getText().matches("App version is");
			}
		});
	}

	@Test
	public void shows_main_page_with_application_state() {
		driver.get(baseUri);

		(new WebDriverWait(driver, 10)).until(new ExpectedCondition<Boolean>() {
			@Override
			public Boolean apply(WebDriver input) {
				WebElement elt = input.findElement(By.cssSelector("#info .info-version"));
				return !elt.getText().matches("App state is.*RUNNING");
			}
		});
	}
}

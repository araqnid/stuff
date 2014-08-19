package org.araqnid.stuff;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.araqnid.stuff.config.AppConfig;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.base.Splitter;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

public class BrowserTestFrame {
	private final String ourHostName;
	private final Provider<RemoteWebDriver> driverProvider;
	public RemoteWebDriver driver;
	public Injector injector;
	public Server server;
	public int localPort;
	public String baseUri;

	public static final BrowserTestFrame asInEnvironment() {
		String value = System.getProperty("browserTestEnvironment");
		if (value == null) return new BrowserTestFrame();
		Iterator<String> parts = Splitter.on(Pattern.compile("\\s+")).split(value).iterator();
		String host = parts.next();
		int port = Integer.valueOf(parts.next());
		String browser = parts.next();
		String ourHostName = parts.next();
		return new BrowserTestFrame(host, port, parseBrowserCapabilities(browser), ourHostName);
	}

	public static Capabilities parseBrowserCapabilities(String browser) {
		Pattern iePattern = Pattern.compile("IE(\\d+)", Pattern.CASE_INSENSITIVE);
		Matcher ieMatcher = iePattern.matcher(browser);
		if (ieMatcher.matches()) {
			DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();
			capabilities.setVersion(ieMatcher.group(1));
			return capabilities;
		}
		if (browser.equalsIgnoreCase("firefox")) {
			DesiredCapabilities capabilities = DesiredCapabilities.firefox();
			return capabilities;
		}
		throw new IllegalArgumentException("Unparseable browser capabilities: " + browser);
	}

	public BrowserTestFrame() {
		driverProvider = new Provider<RemoteWebDriver>() {
			@Override
			public RemoteWebDriver get() {
				return new FirefoxDriver();
			}
		};
		ourHostName = "localhost";
	}

	public BrowserTestFrame(String browserIp, int browserPort, final Capabilities capabilities, String ourHostName) {
		this.ourHostName = ourHostName;
		final URL url;
		try {
			url = new URL(String.format("http://%s:%d/wd/hub", browserIp, browserPort));
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Unable to make ip/port into URL", e);
		}
		driverProvider = new Provider<RemoteWebDriver>() {
			@Override
			public RemoteWebDriver get() {
				return new RemoteWebDriver(url, capabilities);
			}
		};
	}

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
		baseUri = "http://" + ourHostName + ":" + localPort + "/";
		driver = driverProvider.get();
	}

	public void tearDown() throws Exception {
		if (server != null) server.stop();
		if (driver != null) driver.quit();
	}

	public WebDriverWait waitFor(int timeout) {
		return new WebDriverWait(driver, timeout);
	}
}

package org.araqnid.stuff;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;

public class DefaultPageTest {
	private static BrowserTestFrame browser;
	private int timeout = 2;

	@BeforeClass
	public static void setUp() throws Exception {
		browser = new BrowserTestFrame();
		browser.setUp();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		browser.tearDown();
	}

	@Test
	public void shows_main_page_with_application_version() {
		browser.driver.get(browser.baseUri);

		waitForElementPresent("#info .info-version.loaded");
		assertElementPresent("#info .info-version.loaded");
	}

	@Test
	public void shows_main_page_with_application_state() {
		browser.driver.get(browser.baseUri);

		waitForElementPresent("#info .info-state.loaded");
		assertElementPresent("#info .info-state.loaded");
	}

	private void waitForElementPresent(final String cssSelector) {
		browser.waitFor(timeout).until(new ExpectedCondition<Boolean>() {
			@Override
			public Boolean apply(WebDriver input) {
				return !input.findElements(By.cssSelector(cssSelector)).isEmpty();
			}
		});
	}

	private void assertElementPresent(final String cssSelector) {
		Assert.assertTrue(cssSelector, !browser.driver.findElements(By.cssSelector(cssSelector)).isEmpty());
	}
}

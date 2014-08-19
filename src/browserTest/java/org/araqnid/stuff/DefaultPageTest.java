package org.araqnid.stuff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

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
		browser = BrowserTestFrame.asInEnvironment();
		browser.setUp();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		browser.tearDown();
	}

	@Test
	public void shows_main_page_with_application_version() {
		browser.driver.get(browser.baseUri);

		waitForElementPresent("#info.completed");
		assertElementPresent("#info .info-version.loaded");
		assertThat(textAt("#info .info-version"), containsString("App version is"));
	}

	@Test
	public void shows_main_page_with_application_state() {
		browser.driver.get(browser.baseUri);

		waitForElementPresent("#info.completed");
		assertElementPresent("#info .info-state.loaded");
		assertThat(textAt("#info .info-state"), containsString("App state is"));
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

	private String textAt(final String cssSelector) {
		return browser.driver.findElement(By.cssSelector(cssSelector)).getText();
	}
}

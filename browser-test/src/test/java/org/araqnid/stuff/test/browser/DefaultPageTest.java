package org.araqnid.stuff.test.browser;

import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@Ignore
public class DefaultPageTest {
	private static BrowserTestFrame browser;
	private int timeout = 2;

	@BeforeClass
	public static void setUpFrame() throws Exception {
		browser = BrowserTestFrame.asInEnvironment();
		browser.setUp();
	}

	@AfterClass
	public static void tearDownFrame() throws Exception {
		browser.tearDown();
	}

	@Before
	public void setUp() {
		browser.driver.get(browser.baseUri);
		waitForElementPresent("#info.completed");
	}

	@Test
	public void shows_main_page_with_application_version() {
		assertElementPresent("#info .info-version.loaded");
		assertThat(textAt("#info .info-version"), containsString("App version is"));
	}

	@Test
	public void shows_main_page_with_application_state() {
		assertElementPresent("#info .info-state.loaded");
		assertThat(textAt("#info .info-state"), containsString("App state is"));
	}

	@Test
	public void javascript_script_accessible() {
		assertThat(browser.driver.executeScript("return true"), Matchers.<Object>equalTo(Boolean.TRUE));
	}

	@Test
	public void short_jquery_not_exposed_in_global_scope() {
		assertThat(browser.driver.executeScript("return !window.$"), Matchers.<Object>equalTo(Boolean.TRUE));
	}

	@Test
	public void long_jquery_not_exposed_in_global_scope() {
		assertThat(browser.driver.executeScript("return !window.jQuery"), Matchers.<Object>equalTo(Boolean.TRUE));
	}

	@Test
	public void short_lodash_not_exposed_in_global_scope() {
		assertThat(browser.driver.executeScript("return !window._"), Matchers.<Object>equalTo(Boolean.TRUE));
	}

	@Test
	public void long_lodash_not_exposed_in_global_scope() {
		assertThat(browser.driver.executeScript("return !window.lodash"), Matchers.<Object>equalTo(Boolean.TRUE));
	}

	private void waitForElementPresent(final String cssSelector) {
		browser.waitFor(timeout).until(new ExpectedCondition<Boolean>() {
			@Override
			public Boolean apply(WebDriver input) {
				return !input.findElements(By.cssSelector(cssSelector)).isEmpty();
			}
			@Override
			public String toString() {
				return "waitForElementPresent(" + cssSelector + ")";
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

package org.araqnid.stuff;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class DefaultPageTest {
	private BrowserTestFrame browser;

	@Before
	public void setUp() throws Exception {
		browser = new BrowserTestFrame();
		browser.setUp();
	}

	@After
	public void tearDown() throws Exception {
		browser.tearDown();
	}

	@Test
	public void shows_main_page_with_application_version() {
		browser.driver.get(browser.baseUri);

		(new WebDriverWait(browser.driver, 10)).until(new ExpectedCondition<Boolean>() {
			@Override
			public Boolean apply(WebDriver input) {
				WebElement elt = input.findElement(By.cssSelector("#info .info-version"));
				return !elt.getText().matches("App version is");
			}
		});
	}

	@Test
	public void shows_main_page_with_application_state() {
		browser.driver.get(browser.baseUri);

		(new WebDriverWait(browser.driver, 10)).until(new ExpectedCondition<Boolean>() {
			@Override
			public Boolean apply(WebDriver input) {
				WebElement elt = input.findElement(By.cssSelector("#info .info-version"));
				return !elt.getText().matches("App state is.*RUNNING");
			}
		});
	}
}

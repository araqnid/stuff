package org.araqnid.stuff.test.integration;

import static org.araqnid.stuff.test.integration.HttpClientMatchers.ok;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithHtmlContent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.StringContainsInOrder.stringContainsInOrder;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.araqnid.stuff.config.ServerIdentity;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.inject.Key;

public class HelloWorldControllerIntegrationTest extends IntegrationTest {
	@Test
	public void says_hello_world() throws Exception {
		String serverIdentity = server.getInjector().getInstance(Key.get(String.class, ServerIdentity.class));
		try (CloseableHttpResponse response = doGet("/mvc/helloworld")) {
			assertThat(
					response,
					is(allOf(ok(), responseWithHtmlContent(stringContainsInOrder(ImmutableList.of(
							"<h1>MVC hello world</h1>", "<p>Hello World!</p>", "<p>server identity: " + serverIdentity
									+ "</p>"))))));
		}
	}
}

package org.araqnid.stuff.test.integration;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Test;

import static org.araqnid.stuff.test.integration.HttpClientMatchers.ok;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithTextContent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class JspIntegrationTest extends IntegrationTest {
	@Test
	public void jsp_page_runs_scriptlet() throws Exception {
		try (CloseableHttpResponse response = doGet("/test_scriptlet.jsp")) {
			assertThat(response, is(allOf(ok(), responseWithTextContent(containsString("This page has a scriptlet")))));
		}
	}
}

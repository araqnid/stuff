package org.araqnid.stuff.test.integration;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import static org.araqnid.stuff.test.integration.HttpClientMatchers.ok;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithTextContent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.StringContainsInOrder.stringContainsInOrder;

public class JspIntegrationTest extends IntegrationTest {
	@Test
	public void jsp_page_runs_scriptlet() throws Exception {
		try (CloseableHttpResponse response = doGet("/test_scriptlet.jsp")) {
			assertThat(response, is(allOf(ok(), responseWithTextContent(containsString("This page has a scriptlet")))));
		}
	}

	@Test
	public void jsp_page_uses_jstl_tag() throws Exception {
		try (CloseableHttpResponse response = doGet("/test_jstl.jsp")) {
			assertThat(response, is(allOf(ok(), responseWithTextContent(containsString("This page uses JSTL")))));
		}
	}

	@Test
	public void jsp_page_uses_custom_tag() throws Exception {
		try (CloseableHttpResponse response = doGet("/test_jsp_tag.jsp")) {
			assertThat(
					response,
					is(allOf(ok(), responseWithTextContent(stringContainsInOrder(ImmutableList.of("This page uses",
							"<span id=\"test\">a custom tag</span>"))))));
		}
	}

	@Test
	public void jsp_page_uses_tag_file() throws Exception {
		try (CloseableHttpResponse response = doGet("/test_jsp_tag_file.jsp")) {
			assertThat(
					response,
					is(allOf(ok(), responseWithTextContent(stringContainsInOrder(ImmutableList.of("This page uses",
							"<span id=\"test\">a tag file</span>"))))));
		}
	}
}

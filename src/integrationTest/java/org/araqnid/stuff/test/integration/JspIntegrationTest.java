package org.araqnid.stuff.test.integration;

import java.util.UUID;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.araqnid.stuff.config.ServerIdentity;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.inject.Key;

import static org.araqnid.stuff.test.integration.HttpClientMatchers.ok;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithTextContent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
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
	public void jsp_page_uses_injectible_tag() throws Exception {
		UUID serverIdentity = server.getInjector().getInstance(Key.get(UUID.class, ServerIdentity.class));
		try (CloseableHttpResponse response = doGet("/test_jsp_injectible_tag.jsp")) {
			assertThat(response,
					is(allOf(ok(), responseWithTextContent(containsString("Server identity is " + serverIdentity)))));
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

	@Test
	public void jsp_page_uses_embedded_tag() throws Exception {
		try (CloseableHttpResponse response = doGet("/test_jsp_embedded_tag.jsp")) {
			assertThat(
					response,
					is(allOf(ok(), responseWithTextContent(stringContainsInOrder(ImmutableList.of("This page uses",
							"<span id=\"test\">an embedded tag</span>"))))));
		}
	}
}

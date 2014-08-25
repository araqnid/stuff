package org.araqnid.stuff.test.integration;

import static org.araqnid.stuff.test.integration.HttpClientMatchers.ok;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithJsonContent;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonAny;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonObject;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.TreeNode;

public class InfoResourcesIntegrationTest extends IntegrationTest {
	@Test
	public void version_resource() throws Exception {
		assertThat(
				doGet("/_api/info/version"),
				is(allOf(
						ok(),
						responseWithJsonContent(jsonObject()
								.withProperty("title", Matchers.notNullValue(TreeNode.class))
								.withProperty("vendor", Matchers.notNullValue(TreeNode.class))
								.withProperty("version", Matchers.notNullValue(TreeNode.class))))));
	}

	@Test
	@Ignore
	public void health_resource() throws Exception {
		assertThat(doGet("/_api/info/health"), is(allOf(ok(), responseWithJsonContent(jsonString(any(String.class))))));
	}

	@Test
	public void state_resource() throws Exception {
		assertThat(doGet("/_api/info/state"), is(allOf(ok(), responseWithJsonContent(jsonString(any(String.class))))));
	}

	@Test
	public void routing_resource() throws Exception {
		assertThat(doGet("/_api/info/routing"), is(allOf(ok(), responseWithJsonContent(jsonAny()))));
	}
}

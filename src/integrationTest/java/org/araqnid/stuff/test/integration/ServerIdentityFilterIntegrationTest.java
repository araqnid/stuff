package org.araqnid.stuff.test.integration;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Rule;
import org.junit.Test;

import static org.araqnid.stuff.test.integration.HttpClientMatchers.headerWithValue;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithHeader;
import static org.araqnid.stuff.test.integration.MiscMatchers.likeAUUID;
import static org.araqnid.stuff.test.integration.MiscMatchers.twoParts;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;

public class ServerIdentityFilterIntegrationTest {
	@Rule
	public final ServerRunner server = new ServerRunner();

	@Test
	public void server_response_includes_server_identity() throws Exception {
		try (CloseableHttpResponse response = server.doGet("/")) {
			assertThat(response, responseWithHeader("X-Server-Identity", headerWithValue(twoParts(any(String.class), likeAUUID()))));
		}
	}
}
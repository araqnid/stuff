package org.araqnid.stuff.test.integration;

import static org.araqnid.stuff.test.integration.HttpClientMatchers.forbidden;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.newCookie;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.ok;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.removeCookie;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithJsonContent;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonNull;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonObject;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonString;
import static org.araqnid.stuff.testutil.RandomData.randomEmailAddress;
import static org.araqnid.stuff.testutil.RandomData.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class MerlotResourcesIntegrationTest {
	private CloseableHttpClient httpClient;
	private ServerRunner server = new ServerRunner();

	@Before
	public void startServer() throws Exception {
		server.start();
	}

	@After
	public void stopServer() throws Exception {
		server.stop();
	}

	@Before
	public void setUp() throws Exception {
		httpClient = HttpClientBuilder.create().build();
	}

	@After
	public void tearDown() throws Exception {
		if (httpClient != null) httpClient.close();
	}

	@Test
	public void status_resource_with_no_cookie() throws Exception {
		CloseableHttpResponse response = doGet("/_api/merlot/");
		assertThat(response.getStatusLine(), is(ok()));
		assertThat(
				response,
				is(responseWithJsonContent(jsonObject().withProperty("userInfo", jsonNull()).withProperty("version",
						jsonNull()))));
		response.close();
	}

	@Ignore
	@Test
	public void status_resource_with_auth_cookie() throws Exception {
		String userId = UUID.randomUUID().toString();
		String userCN = randomString("User");
		String username = randomEmailAddress();
		setupUser(userId, userCN, username, randomString());
		CloseableHttpResponse response = doGet("/_api/merlot/", ImmutableMap.of("Cookie", "ATKT=" + authTicket(userId)));
		assertThat(response.getStatusLine(), is(ok()));
		assertThat(
				response,
				is(responseWithJsonContent(jsonObject().withProperty(
						"userInfo",
						jsonObject().withProperty("commonName", jsonString(userCN)).withProperty("username",
								jsonString(username))).withProperty("version", jsonNull()))));
		response.close();
	}

	@Ignore
	@Test
	public void status_resource_with_invalid_auth_cookie() throws Exception {
		CloseableHttpResponse response = doGet("/_api/merlot/", ImmutableMap.of("Cookie", "ATKT=xyzzy"));
		assertThat(response.getStatusLine(), is(forbidden()));
		response.close();
	}

	@Ignore
	@Test
	public void sign_in_creates_auth_ticket() throws Exception {
		String userId = UUID.randomUUID().toString();
		String userCN = randomString("User");
		String username = randomEmailAddress();
		String password = randomString();
		setupUser(userId, userCN, username, password);
		CloseableHttpResponse response = doPostForm("/_api/merlot/sign-in", ImmutableMap.<String, String> of(),
				ImmutableMap.<String, String> of("username", username, "password", password));
		assertThat(response.getStatusLine(), is(ok()));
		assertThat(response.getFirstHeader("Set-Cookie"), is(newCookie("ATKT", equalTo(authTicket(userId)))));
		response.close();
	}

	@Ignore
	@Test
	public void sign_in_with_wrong_password_is_forbidden() throws Exception {
		String userId = UUID.randomUUID().toString();
		String userCN = randomString("User");
		String username = randomEmailAddress();
		String password = randomString();
		setupUser(userId, userCN, username, password);
		CloseableHttpResponse response = doPostForm("/_api/merlot/sign-in", ImmutableMap.<String, String> of(),
				ImmutableMap.<String, String> of("username", username, "password", "!" + password));
		assertThat(response.getStatusLine(), is(forbidden()));
		response.close();
	}

	@Ignore
	@Test
	public void sign_out_removes_cookie() throws Exception {
		CloseableHttpResponse response = doPostForm("/_api/merlot/sign-out", ImmutableMap.<String, String> of(),
				ImmutableMap.<String, String> of());
		assertThat(response.getStatusLine(), is(ok()));
		assertThat(response.getFirstHeader("Set-Cookie"), is(removeCookie("ATKT")));
		response.close();
	}

	private void setupUser(String userId, String userCN, String username, String password) {
		// TODO Auto-generated method stub
	}

	private CloseableHttpResponse doGet(String path, Map<String, String> headers) throws IOException {
		HttpGet request = new HttpGet(server.appUri(path));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}
		return httpClient.execute(request);
	}

	private CloseableHttpResponse doPostForm(String path, Map<String, String> headers,
			Map<String, String> formParameters) throws IOException {
		HttpPost request = new HttpPost(server.appUri(path));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}
		List<NameValuePair> parameters = new ArrayList<>();
		for (Map.Entry<String, String> e : formParameters.entrySet()) {
			parameters.add(new BasicNameValuePair(e.getKey(), e.getValue()));
		}
		request.setEntity(new UrlEncodedFormEntity(parameters));
		return httpClient.execute(request);
	}

	private CloseableHttpResponse doGet(String path) throws IOException {
		return doGet(path, Collections.<String, String> emptyMap());
	}

	private static String authTicket(String userId) {
		return "u" + userId + ",x00000000";
	}

}

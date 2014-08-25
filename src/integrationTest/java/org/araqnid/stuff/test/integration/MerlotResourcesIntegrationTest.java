package org.araqnid.stuff.test.integration;

import static org.araqnid.stuff.test.integration.HttpClientMatchers.forbidden;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.newCookie;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.ok;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.removeCookie;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithCookies;
import static org.araqnid.stuff.test.integration.HttpClientMatchers.responseWithJsonContent;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonAny;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonNull;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonObject;
import static org.araqnid.stuff.test.integration.JsonMatchers.jsonString;
import static org.araqnid.stuff.testutil.RandomData.randomEmailAddress;
import static org.araqnid.stuff.testutil.RandomData.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.URISyntaxException;
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
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.araqnid.stuff.MerlotRepository;
import org.junit.After;
import org.junit.Before;
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
		httpClient = HttpClients.createMinimal();
	}

	@After
	public void tearDown() throws Exception {
		if (httpClient != null) httpClient.close();
	}

	@Test
	public void status_resource_with_no_cookie() throws Exception {
		CloseableHttpResponse response = doGet("/_api/merlot/");
		assertThat(
				response,
				is(both(ok()).and(
						responseWithJsonContent(jsonObject().withProperty("userInfo", jsonNull()).withProperty(
								"version", jsonNull())))));
		response.close();
	}

	@Test
	public void status_resource_with_auth_cookie() throws Exception {
		String userCN = randomString("User");
		String username = randomEmailAddress();
		UUID userId = setupUser(userCN, username, randomString().toCharArray());
		CloseableHttpResponse response = doGet("/_api/merlot/", ImmutableMap.of("Cookie", "ATKT=" + authTicket(userId)));
		assertThat(
				response,
				is(both(ok()).and(
						responseWithJsonContent(jsonObject().withProperty(
								"userInfo",
								jsonObject().withProperty("commonName", jsonString(userCN)).withProperty("username",
										jsonString(username))).withProperty("version", jsonNull())))));
		response.close();
	}

	@Test
	public void status_resource_with_invalid_auth_cookie() throws Exception {
		CloseableHttpResponse response = doGet("/_api/merlot/", ImmutableMap.of("Cookie", "ATKT=xyzzy"));
		assertThat(response, is(forbidden()));
		response.close();
	}

	@Test
	public void status_resource_with_valid_auth_cookie_for_invalid_user() throws Exception {
		String userCN = randomString("User");
		String username = randomEmailAddress();
		char[] password = randomString().toCharArray();
		UUID userId = setupAndDeleteUser(userCN, username, password);
		CloseableHttpResponse response = doGet("/_api/merlot/", ImmutableMap.of("Cookie", "ATKT=" + authTicket(userId)));
		assertThat(
				response,
				is(both(ok()).and(
						responseWithJsonContent(jsonObject().withProperty("userInfo", jsonNull()).withProperty(
								"version", jsonAny())))));
		response.close();
	}

	@Test
	public void sign_in_creates_auth_ticket() throws Exception {
		String userCN = randomString("User");
		String username = randomEmailAddress();
		char[] password = randomString().toCharArray();
		UUID userId = setupUser(userCN, username, password);
		CloseableHttpResponse response = doPostForm("/_api/merlot/sign-in", ImmutableMap.<String, String> of(),
				ImmutableMap.<String, String> of("username", username, "password", new String(password)));
		assertThat(response, is(both(ok()).and(responseWithCookies(newCookie("ATKT", equalTo(authTicket(userId)))))));
		response.close();
	}

	@Test
	public void sign_in_for_deleted_user_is_forbidden() throws Exception {
		String userCN = randomString("User");
		String username = randomEmailAddress();
		char[] password = randomString().toCharArray();
		setupAndDeleteUser(userCN, username, password);
		CloseableHttpResponse response = doPostForm("/_api/merlot/sign-in", ImmutableMap.<String, String> of(),
				ImmutableMap.<String, String> of("username", username, "password", new String(password)));
		assertThat(response, is(forbidden()));
		response.close();
	}

	@Test
	public void sign_in_with_wrong_password_is_forbidden() throws Exception {
		String userCN = randomString("User");
		String username = randomEmailAddress();
		char[] password = randomString().toCharArray();
		setupUser(userCN, username, password);
		CloseableHttpResponse response = doPostForm("/_api/merlot/sign-in", ImmutableMap.<String, String> of(),
				ImmutableMap.<String, String> of("username", username, "password", "!" + new String(password)));
		assertThat(response, is(forbidden()));
		response.close();
	}

	@Test
	public void sign_out_removes_cookie() throws Exception {
		String userCN = randomString("User");
		String username = randomEmailAddress();
		UUID userId = setupUser(userCN, username, randomString().toCharArray());
		CloseableHttpResponse response = doPostForm("/_api/merlot/sign-out",
				ImmutableMap.of("Cookie", "ATKT=" + authTicket(userId)), ImmutableMap.<String, String> of());
		System.err.println("found " + response.getFirstHeader("Set-Cookie"));
		assertThat(response, is(both(ok()).and(responseWithCookies(removeCookie("ATKT")))));
		response.close();
	}

	@Test
	public void sign_out_with_no_cookie_is_a_no_op() throws Exception {
		CloseableHttpResponse response = doPostForm("/_api/merlot/sign-out", ImmutableMap.<String, String> of(),
				ImmutableMap.<String, String> of());
		assertThat(response, is(both(ok()).and(responseWithCookies())));
		response.close();
	}

	private UUID setupUser(String userCN, String username, char[] password) {
		MerlotRepository repo = server.getInjector().getInstance(MerlotRepository.class);
		MerlotRepository.User user = repo.createUser(username, userCN, password);
		return user.id;
	}

	private UUID setupAndDeleteUser(String userCN, String username, char[] password) {
		MerlotRepository repo = server.getInjector().getInstance(MerlotRepository.class);
		MerlotRepository.User user = repo.createUser(username, userCN, password);
		repo.deleteUser(user.id);
		return user.id;
	}

	private CloseableHttpResponse doGet(String path, Map<String, String> headers) throws IOException,
			URISyntaxException {
		HttpGet request = new HttpGet(server.uri(path));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}
		return httpClient.execute(request);
	}

	private CloseableHttpResponse doPostForm(String path, Map<String, String> headers,
			Map<String, String> formParameters) throws IOException, URISyntaxException {
		HttpPost request = new HttpPost(server.uri(path));
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

	private CloseableHttpResponse doGet(String path) throws IOException, URISyntaxException {
		return doGet(path, Collections.<String, String> emptyMap());
	}

	private static String authTicket(UUID userId) {
		return "u" + userId + ",x00000000";
	}

}

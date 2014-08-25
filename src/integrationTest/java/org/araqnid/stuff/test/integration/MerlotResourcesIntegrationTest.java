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

import java.util.UUID;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.araqnid.stuff.MerlotRepository;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class MerlotResourcesIntegrationTest extends IntegrationTest {
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

	private static String authTicket(UUID userId) {
		return "u" + userId + ",x00000000";
	}
}

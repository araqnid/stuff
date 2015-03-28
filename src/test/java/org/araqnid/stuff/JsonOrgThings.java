package org.araqnid.stuff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

public class JsonOrgThings {
	@Test
	public void put_one_scalar() {
		JSONObject obj = new JSONObject();
		obj.put("a", 1);
		assertThat(obj, isSameJSONObject("{a:1}"));
	}

	@Test
	public void put_two_scalars() {
		JSONObject obj = new JSONObject();
		obj.put("a", 1);
		obj.put("a", 2);
		assertThat(obj, isSameJSONObject("{a:2}"));
	}

	@Test
	public void put_one_array() {
		JSONObject obj = new JSONObject();
		obj.put("a", new JSONArray(ImmutableList.of(1, 2)));
		assertThat(obj, isSameJSONObject("{a:[1, 2]}"));
	}

	@Test
	public void put_two_arrays() {
		JSONObject obj = new JSONObject();
		obj.put("a", new JSONArray(ImmutableList.of(1, 2)));
		obj.put("a", new JSONArray(ImmutableList.of(3, 4)));
		assertThat(obj, isSameJSONObject("{a:[3, 4]}"));
	}

	@Test
	public void append_one_scalar() {
		JSONObject obj = new JSONObject();
		obj.append("a", 1);
		assertThat(obj, isSameJSONObject("{a:[1]}"));
	}

	@Test
	public void append_two_scalars() {
		JSONObject obj = new JSONObject();
		obj.append("a", 1);
		obj.append("a", 2);
		assertThat(obj, isSameJSONObject("{a:[1,2]}"));
	}

	@Test
	public void append_one_array() {
		JSONObject obj = new JSONObject();
		obj.append("a", new JSONArray(ImmutableList.of(1, 2)));
		assertThat(obj, isSameJSONObject("{a:[[1,2]]}"));
	}

	@Test
	public void append_two_arrays() {
		JSONObject obj = new JSONObject();
		obj.append("a", new JSONArray(ImmutableList.of(1, 2)));
		obj.append("a", new JSONArray(ImmutableList.of(3, 4)));
		assertThat(obj, isSameJSONObject("{a:[[1,2],[3,4]]}"));
	}

	@Test
	public void accumulate_one_scalar() {
		JSONObject obj = new JSONObject();
		obj.accumulate("a", 1);
		assertThat(obj, isSameJSONObject("{a:1}"));
	}

	@Test
	public void accumulate_two_scalars() {
		JSONObject obj = new JSONObject();
		obj.accumulate("a", 1);
		obj.accumulate("a", 2);
		assertThat(obj, isSameJSONObject("{a:[1,2]}"));
	}

	@Test
	public void accumulate_one_array() {
		JSONObject obj = new JSONObject();
		obj.accumulate("a", new JSONArray(ImmutableList.of(1, 2)));
		assertThat(obj, isSameJSONObject("{a:[[1,2]]}"));
	}

	@Test
	public void accumulate_two_arrays() {
		JSONObject obj = new JSONObject();
		obj.accumulate("a", new JSONArray(ImmutableList.of(1, 2)));
		obj.accumulate("a", new JSONArray(ImmutableList.of(3, 4)));
		assertThat(obj, isSameJSONObject("{a:[[1,2],[3,4]]}"));
	}

	private static Matcher<JSONObject> isSameJSONObject(String expectedJsonSource) {
		return new TypeSafeDiagnosingMatcher<JSONObject>() {
			private final Matcher<String> jsonMatcher = isSameJSON(expectedJsonSource);

			@Override
			protected boolean matchesSafely(JSONObject item, Description mismatchDescription) {
				String json = item.toString();
				jsonMatcher.describeMismatch(json, mismatchDescription);
				return jsonMatcher.matches(json);
			}

			@Override
			public void describeTo(Description description) {
				description.appendDescriptionOf(jsonMatcher);
			}
		};
	}

	private static Matcher<String> isSameJSON(String expectedJsonSource) {
		return new TypeSafeDiagnosingMatcher<String>() {
			private final ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
					.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

			private final JsonNode expectedJson;
			{
				try {
					expectedJson = objectMapper.readTree(expectedJsonSource);
				} catch (IOException e) {
					throw new IllegalArgumentException("Invalid reference JSON", e);
				}
			}
			private final Matcher<JsonNode> matcher = equalTo(expectedJson);

			@Override
			protected boolean matchesSafely(String item, Description mismatchDescription) {
				JsonNode actualJson;
				try {
					actualJson = objectMapper.readTree(item);
				} catch (IOException e) {
					mismatchDescription.appendText("Invalid JSON: ").appendValue(e);
					return false;
				}
				matcher.describeMismatch(actualJson, mismatchDescription);
				return matcher.matches(actualJson);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("JSON ").appendValue(expectedJsonSource);
			}
		};
	}
}

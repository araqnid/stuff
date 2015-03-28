package org.araqnid.stuff;

import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonEquivalenceMatchers {
	public static Matcher<JSONObject> equivalentJSONObject(String expectedJsonSource) {
		return new JsonRepresentationMatcher<JSONObject>(expectedJsonSource) {
			@Override
			protected String jsonToString(JSONObject item) {
				return item.toString();
			}
		};
	}

	public static Matcher<JsonNode> equivalentJsonNode(String expectedJsonSource) {
		return new JsonRepresentationMatcher<JsonNode>(expectedJsonSource) {
			@Override
			protected String jsonToString(JsonNode item) {
				return item.toString();
			}
		};
	}

	private static abstract class JsonRepresentationMatcher<T> extends TypeSafeDiagnosingMatcher<T> {
		private final Matcher<String> jsonMatcher;

		public JsonRepresentationMatcher(String expectedJsonSource) {
			jsonMatcher = equivalentTo(expectedJsonSource);
		}

		@Override
		protected boolean matchesSafely(T item, Description mismatchDescription) {
			String json = jsonToString(item);
			jsonMatcher.describeMismatch(json, mismatchDescription);
			return jsonMatcher.matches(json);
		}

		@Override
		public void describeTo(Description description) {
			description.appendDescriptionOf(jsonMatcher);
		}

		protected abstract String jsonToString(T item);
	}

	public static Matcher<String> equivalentTo(String expectedJsonSource) {
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

	private JsonEquivalenceMatchers() {
	}
}

package org.araqnid.stuff;

import java.io.IOException;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

import static org.hamcrest.Matchers.equalTo;

public final class JsonEquivalenceMatchers {
	private static final ObjectMapper STRICT_MAPPER = new ObjectMapper();
	private static final ObjectMapper LAX_MAPPER = new ObjectMapper().enable(
			JsonParser.Feature.ALLOW_SINGLE_QUOTES).enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);

	public static Matcher<JsonNode> equivalentJsonNode(String expectedJsonSource) {
		return new JsonRepresentationMatcher<JsonNode>(expectedJsonSource) {
			@Override
			protected String jsonToString(JsonNode item) {
				try {
					return STRICT_MAPPER.writeValueAsString(item);
				} catch (JsonProcessingException e) {
					throw Throwables.propagate(e);
				}
			}
		};
	}

	public static abstract class JsonRepresentationMatcher<T> extends TypeSafeDiagnosingMatcher<T> {
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
			private final JsonNode expectedJson;
			{
				try {
					expectedJson = LAX_MAPPER.readTree(expectedJsonSource);
				} catch (IOException e) {
					throw new IllegalArgumentException("Invalid reference JSON", e);
				}
			}
			private final Matcher<JsonNode> matcher = equalTo(expectedJson);

			@Override
			protected boolean matchesSafely(String item, Description mismatchDescription) {
				JsonNode actualJson;
				try {
					actualJson = STRICT_MAPPER.readTree(item);
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

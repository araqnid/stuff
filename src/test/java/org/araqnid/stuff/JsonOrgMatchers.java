package org.araqnid.stuff;

import org.hamcrest.Matcher;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonOrgMatchers {
	public static Matcher<JSONObject> equivalentJSONObject(String expectedJsonSource) {
		return new JsonEquivalenceMatchers.JsonRepresentationMatcher<JSONObject>(expectedJsonSource) {
			@Override
			protected String jsonToString(JSONObject item) {
				return item.toString();
			}
		};
	}

	public static Matcher<JSONArray> equivalentJSONArray(String expectedJsonSource) {
		return new JsonEquivalenceMatchers.JsonRepresentationMatcher<JSONArray>(expectedJsonSource) {
			@Override
			protected String jsonToString(JSONArray item) {
				return item.toString();
			}
		};
	}

	public static Matcher<JSONObject> jsonlibObject(Matcher<? extends JsonNode> matcher) {
		return new JsonStructureMatchers.JsonRepresentationMatcher<JSONObject>(matcher) {
			@Override
			protected String jsonToString(JSONObject item) {
				return item.toString();
			}
		};
	}

	public static Matcher<JSONArray> jsonlibArray(Matcher<? extends JsonNode> matcher) {
		return new JsonStructureMatchers.JsonRepresentationMatcher<JSONArray>(matcher) {
			@Override
			protected String jsonToString(JSONArray item) {
				return item.toString();
			}
		};
	}


}

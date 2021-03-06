package org.araqnid.stuff.matchers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.araqnid.stuff.matchers.JsonOrgMatchers.equivalentJSONArray;
import static org.araqnid.stuff.matchers.JsonOrgMatchers.equivalentJSONObject;
import static org.araqnid.stuff.matchers.JsonOrgMatchers.jsonlibArray;
import static org.araqnid.stuff.matchers.JsonOrgMatchers.jsonlibObject;
import static org.araqnid.stuff.matchers.JsonStructureMatchers.jsonArray;
import static org.araqnid.stuff.matchers.JsonStructureMatchers.jsonNumber;
import static org.araqnid.stuff.matchers.JsonStructureMatchers.jsonObject;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class JsonOrgMatchersTest {

	@Test
	public void matches_jsonlib_object_for_equivalence() {
		assertThat(new JSONObject().put("a", 1), equivalentJSONObject("{a : 1 }"));
	}

	@Test
	public void matches_jsonlib_array_for_equivalence() {
		assertThat(new JSONArray().put(1), equivalentJSONArray("[1]"));
	}

	@Test
	public void matches_jsonlib_object_structurally() throws Exception {
		assertThat(new JSONObject("{ a : 1 }"), is(jsonlibObject(jsonObject().withProperty("a", 1))));
	}

	@Test
	public void matches_jsonlib_array_structurally() throws Exception {
		assertThat(new JSONArray("[1, 2, 3]"),
				is(jsonlibArray(jsonArray().of(jsonNumber(1), jsonNumber(2), jsonNumber(3)))));
	}
}

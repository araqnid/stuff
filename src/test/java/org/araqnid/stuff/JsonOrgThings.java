package org.araqnid.stuff;

import static org.araqnid.stuff.JsonOrgMatchers.equivalentJSONObject;
import static org.hamcrest.MatcherAssert.assertThat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class JsonOrgThings {
	@Test
	public void put_one_scalar() {
		JSONObject obj = new JSONObject();
		obj.put("a", 1);
		assertThat(obj, equivalentJSONObject("{a:1}"));
	}

	@Test
	public void put_two_scalars() {
		JSONObject obj = new JSONObject();
		obj.put("a", 1);
		obj.put("a", 2);
		assertThat(obj, equivalentJSONObject("{a:2}"));
	}

	@Test
	public void put_one_array() {
		JSONObject obj = new JSONObject();
		obj.put("a", new JSONArray(ImmutableList.of(1, 2)));
		assertThat(obj, equivalentJSONObject("{a:[1, 2]}"));
	}

	@Test
	public void put_two_arrays() {
		JSONObject obj = new JSONObject();
		obj.put("a", new JSONArray(ImmutableList.of(1, 2)));
		obj.put("a", new JSONArray(ImmutableList.of(3, 4)));
		assertThat(obj, equivalentJSONObject("{a:[3, 4]}"));
	}

	@Test
	public void append_one_scalar() {
		JSONObject obj = new JSONObject();
		obj.append("a", 1);
		assertThat(obj, equivalentJSONObject("{a:[1]}"));
	}

	@Test
	public void append_two_scalars() {
		JSONObject obj = new JSONObject();
		obj.append("a", 1);
		obj.append("a", 2);
		assertThat(obj, equivalentJSONObject("{a:[1,2]}"));
	}

	@Test
	public void append_one_array() {
		JSONObject obj = new JSONObject();
		obj.append("a", new JSONArray(ImmutableList.of(1, 2)));
		assertThat(obj, equivalentJSONObject("{a:[[1,2]]}"));
	}

	@Test
	public void append_two_arrays() {
		JSONObject obj = new JSONObject();
		obj.append("a", new JSONArray(ImmutableList.of(1, 2)));
		obj.append("a", new JSONArray(ImmutableList.of(3, 4)));
		assertThat(obj, equivalentJSONObject("{a:[[1,2],[3,4]]}"));
	}

	@Test
	public void accumulate_one_scalar() {
		JSONObject obj = new JSONObject();
		obj.accumulate("a", 1);
		assertThat(obj, equivalentJSONObject("{a:1}"));
	}

	@Test
	public void accumulate_two_scalars() {
		JSONObject obj = new JSONObject();
		obj.accumulate("a", 1);
		obj.accumulate("a", 2);
		assertThat(obj, equivalentJSONObject("{a:[1,2]}"));
	}

	@Test
	public void accumulate_one_array() {
		JSONObject obj = new JSONObject();
		obj.accumulate("a", new JSONArray(ImmutableList.of(1, 2)));
		assertThat(obj, equivalentJSONObject("{a:[[1,2]]}"));
	}

	@Test
	public void accumulate_two_arrays() {
		JSONObject obj = new JSONObject();
		obj.accumulate("a", new JSONArray(ImmutableList.of(1, 2)));
		obj.accumulate("a", new JSONArray(ImmutableList.of(3, 4)));
		assertThat(obj, equivalentJSONObject("{a:[[1,2],[3,4]]}"));
	}
}

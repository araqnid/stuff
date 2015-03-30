package org.araqnid.stuff;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;

import static org.araqnid.stuff.JsonStructureMatchers.json;
import static org.araqnid.stuff.JsonStructureMatchers.jsonArray;
import static org.araqnid.stuff.JsonStructureMatchers.jsonBoolean;
import static org.araqnid.stuff.JsonStructureMatchers.jsonDouble;
import static org.araqnid.stuff.JsonStructureMatchers.jsonInt;
import static org.araqnid.stuff.JsonStructureMatchers.jsonLong;
import static org.araqnid.stuff.JsonStructureMatchers.jsonNull;
import static org.araqnid.stuff.JsonStructureMatchers.jsonNumber;
import static org.araqnid.stuff.JsonStructureMatchers.jsonObject;
import static org.araqnid.stuff.JsonStructureMatchers.jsonString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class JsonStructureMatchersTest {
	@Test
	public void matches_integer() {
		assertThat("123", is(json(jsonInt(123))));
	}

	@Test
	public void matches_integer_with_matcher() {
		assertThat("123", is(json(jsonInt(equalTo(123)))));
	}

	@Test
	public void matches_long() {
		assertThat("123123123123", is(json(jsonLong(123123123123L))));
	}

	@Test
	public void matches_long_with_matcher() {
		assertThat("123123123123", is(json(jsonLong(equalTo(123123123123L)))));
	}

	@Test
	public void matches_double() {
		assertThat("1.5", is(json(jsonDouble(1.5))));
	}

	@Test
	public void matches_double_approximately() {
		assertThat("1.5", is(json(jsonDouble(1.4, 0.2))));
	}

	@Test
	public void matches_double_with_matcher() {
		assertThat("1.5", is(json(jsonDouble(closeTo(1.4, 0.2)))));
	}

	@Test
	public void rejects_floating_point_when_matching_integer() {
		assertThat("1.5", is(json(not(jsonInt(1)))));
	}

	@Test
	public void matches_integer_as_numeric() {
		assertThat("1", is(json(jsonNumber(1))));
	}

	@Test
	public void matches_double_as_numeric_with_matcher() {
		assertThat("1.5", is(json(jsonNumber(closeTo(1.5, 0.01)))));
	}

	@Test
	public void matches_integer_as_numeric_with_matcher() {
		assertThat("1", is(json(jsonNumber(closeTo(1.0, 0.01)))));
	}

	@Test
	public void matches_boolean() {
		assertThat("true", is(json(jsonBoolean(true))));
	}

	@Test
	public void matches_boolean_with_matcher() {
		assertThat("true", is(json(jsonBoolean(equalTo(true)))));
	}

	@Test
	public void matches_string() {
		assertThat("\"foo\"", is(json(jsonString("foo"))));
	}

	@Test
	public void matches_string_with_matcher() {
		assertThat("\"foo\"", is(json(jsonString(equalTo("foo")))));
	}

	@Test
	public void rejects_empty_string() {
		assertThat("", is(not(json(anySubclassOf(TreeNode.class)))));
	}

	@Test
	public void rejects_laxly_formatted_input() {
		assertThat("{ foo: 1 }", is(not(json(anySubclassOf(JsonNode.class)))));
	}

	@Test
	public void matches_empty_object() {
		assertThat("{}", is(json(jsonObject())));
	}

	@Test
	public void traps_unexpected_property_by_default() {
		assertThat("{ \"a\": 1 }", is(not(json(jsonObject()))));
	}

	@Test
	public void allows_unexpected_property_if_requested() {
		assertThat("{ \"a\": 1 }", is(json(jsonObject().withAnyFields())));
	}

	@Test
	public void matches_property_with_integer_value() {
		assertThat("{ \"a\": 1 }", is(json(jsonObject().withProperty("a", 1))));
	}

	@Test
	public void matches_property_with_string_value() {
		assertThat("{ \"a\": \"foo\" }", is(json(jsonObject().withProperty("a", "foo"))));
	}

	@Test
	public void matches_property_with_boolean_value() {
		assertThat("{ \"a\": true }", is(json(jsonObject().withProperty("a", true))));
	}

	@Test
	public void matches_property_with_double_value() {
		assertThat("{ \"a\": 1.0 }", is(json(jsonObject().withProperty("a", 1.0))));
	}

	@Test
	public void matches_property_with_null_value_using_matcher() {
		assertThat("{ \"a\": null }", is(json(jsonObject().withProperty("a", jsonNull()))));
	}

	@Test
	public void matches_property_using_stringify() {
		assertThat("{ \"a\": 1 }", is(json(jsonObject().withPropertyLike("a", "1"))));
	}

	@Test
	public void matches_property_using_json_equivalence() {
		assertThat("{ \"a\": { \"aa\" : 1 } }", is(json(jsonObject().withPropertyJSON("a", "{'aa':1}"))));
	}

	@Test
	public void matches_properties_in_any_order() {
		assertThat(
				"{ \"a\": 1, \"b\": 2 }",
				is(json(both(jsonObject().withProperty("a", 1).withProperty("b", 2)).and(
						jsonObject().withProperty("b", 2).withProperty("a", 1)))));
	}

	@Test
	public void matches_empty_array() {
		assertThat("[]", is(json(jsonArray())));
	}

	@Test
	public void rejects_array_with_unexpected_contents() {
		assertThat("[1]", is(json(not(jsonArray()))));
	}

	@Test
	public void matches_array_of_integers() {
		assertThat("[1,2,3]", is(json(jsonArray().of(jsonInt(1), jsonInt(2), jsonInt(3)))));
	}

	@Test
	public void rejects_array_with_contents_out_of_order() {
		assertThat("[1,2,3]", is(json(not(jsonArray().of(jsonInt(3), jsonInt(2), jsonInt(1))))));
	}

	@Test
	public void matches_array_of_integers_in_any_order() {
		assertThat("[1,2,3]", is(json(jsonArray().inAnyOrder(jsonInt(3), jsonInt(2), jsonInt(1)))));
	}

	@Test
	public void matches_array_containing_at_least_specified_items() {
		assertThat(
				"[1,2,3]",
				is(json(allOf(jsonArray().including(jsonInt(1)), jsonArray().including(jsonInt(2)), jsonArray()
						.including(jsonInt(3))))));
	}

	private static <T> Matcher<? extends T> anySubclassOf(Class<T> clazz) {
		return new TypeSafeDiagnosingMatcher<T>() {

			@Override
			protected boolean matchesSafely(T item, Description mismatchDescription) {
				mismatchDescription.appendText("was a ").appendValue(item.getClass());
				return clazz.isAssignableFrom(item.getClass());
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("something extending ").appendValue(clazz);
			}
		};
	}
}

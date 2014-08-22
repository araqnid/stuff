package org.araqnid.stuff.test.integration;

import static org.hamcrest.Matchers.equalTo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Sets;

public final class JsonMatchers {
	private JsonMatchers() {
	}

	public static ObjectNodeMatcher jsonObject() {
		return new ObjectNodeMatcher();
	}

	public static class ObjectNodeMatcher extends TypeSafeDiagnosingMatcher<ObjectNode> {
		private final Map<String, Matcher<? extends TreeNode>> propertyMatchers = new LinkedHashMap<>();

		@Override
		protected boolean matchesSafely(ObjectNode item, Description mismatchDescription) {
			Set<String> remainingFieldNames = Sets.newHashSet(item.fieldNames());
			for (Map.Entry<String, Matcher<? extends TreeNode>> e : propertyMatchers.entrySet()) {
				if (!item.has(e.getKey())) {
					mismatchDescription.appendText(e.getKey()).appendText(" was not present");
					return false;
				}
				TreeNode value = item.get(e.getKey());
				if (!e.getValue().matches(value)) {
					mismatchDescription.appendText(e.getKey()).appendText(": ");
					e.getValue().describeMismatch(value, mismatchDescription);
					return false;
				}
				remainingFieldNames.remove(e.getKey());
			}
			if (!remainingFieldNames.isEmpty()) {
				mismatchDescription.appendText("unexpected properties: ").appendValue(remainingFieldNames);
				return false;
			}
			return true;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("JSON object { ");
			boolean first = true;
			for (Map.Entry<String, Matcher<? extends TreeNode>> e : propertyMatchers.entrySet()) {
				if (first) {
					first = false;
				}
				else {
					description.appendText(", ");
				}
				description.appendValue(e.getKey()).appendText(": ").appendDescriptionOf(e.getValue());
			}
			description.appendText(" }");
		}

		public ObjectNodeMatcher withProperty(String key, Matcher<? extends TreeNode> value) {
			propertyMatchers.put(key, value);
			return this;
		}
	}

	public static Matcher<TextNode> jsonString(String value) {
		return jsonString(equalTo(value));
	}

	public static Matcher<TextNode> jsonString(final Matcher<String> valueMatcher) {
		return new TypeSafeDiagnosingMatcher<TextNode>() {
			@Override
			protected boolean matchesSafely(TextNode item, Description mismatchDescription) {
				if (!valueMatcher.matches(item.asText())) {
					mismatchDescription.appendText("text was: ").appendValue(item.asText());
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("JSON text ").appendDescriptionOf(valueMatcher);
			}
		};
	}

	public static Matcher<TreeNode> jsonAny() {
		return new TypeSafeDiagnosingMatcher<TreeNode>() {
			@Override
			protected boolean matchesSafely(TreeNode item, Description mismatchDescription) {
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("JSON");
			}
		};
	}

	public static Matcher<NullNode> jsonNull() {
		return new TypeSafeDiagnosingMatcher<NullNode>() {
			@Override
			protected boolean matchesSafely(NullNode item, Description mismatchDescription) {
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("JSON null");
			}
		};
	}
}

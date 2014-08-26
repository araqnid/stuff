package org.araqnid.stuff.test.integration;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hamcrest.collection.IsIterableContainingInOrder;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Sets;

import static org.hamcrest.Matchers.equalTo;

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

	public static class ArrayNodeMatcher extends TypeSafeDiagnosingMatcher<ArrayNode> {
		private Matcher<Iterable<? extends TreeNode>> contentsMatcher = Matchers.emptyIterable();

		@Override
		protected boolean matchesSafely(final ArrayNode item, Description mismatchDescription) {
			Iterable<JsonNode> iterable = new Iterable<JsonNode>() {
				@Override
				public Iterator<JsonNode> iterator() {
					return item.elements();
				}
			};
			if (!contentsMatcher.matches(iterable)) {
				contentsMatcher.describeMismatch(iterable, mismatchDescription);
				return false;
			}
			return true;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("JSON array");
			if (contentsMatcher != null) {
				description.appendText(" containing ").appendDescriptionOf(contentsMatcher);
			}
		}

		@SafeVarargs
		public final ArrayNodeMatcher of(Matcher<? extends TreeNode>... nodeMatchers) {
			if (nodeMatchers.length == 0) {
				contentsMatcher = Matchers.emptyIterable();
				return this;
			}
			@SuppressWarnings({ "unchecked", "rawtypes" })
			List<Matcher<? super TreeNode>> matcherList = (List) Arrays.asList(nodeMatchers);
			contentsMatcher = IsIterableContainingInOrder.contains(matcherList);
			return this;
		}

		@SafeVarargs
		public final ArrayNodeMatcher inAnyOrder(Matcher<? extends TreeNode>... nodeMatchers) {
			if (nodeMatchers.length == 0) {
				contentsMatcher = Matchers.emptyIterable();
				return this;
			}
			@SuppressWarnings({ "unchecked", "rawtypes" })
			List<Matcher<? super TreeNode>> matcherList = (List) Arrays.asList(nodeMatchers);
			contentsMatcher = IsIterableContainingInAnyOrder.containsInAnyOrder(matcherList);
			return this;
		}

		public ArrayNodeMatcher including(final Matcher<? extends TreeNode> nodeMatcher) {
			contentsMatcher = new TypeSafeDiagnosingMatcher<Iterable<? extends TreeNode>>() {
				@Override
				protected boolean matchesSafely(Iterable<? extends TreeNode> item, Description mismatchDescription) {
					Iterator<? extends TreeNode> iterator = item.iterator();
					if (!iterator.hasNext()) {
						mismatchDescription.appendText("array was empty");
						return false;
					}
					while (iterator.hasNext()) {
						TreeNode node = iterator.next();
						if (nodeMatcher.matches(node)) return true;
					}
					mismatchDescription.appendText("not matched: ").appendDescriptionOf(nodeMatcher);
					return false;
				}

				@Override
				public void describeTo(Description description) {
					description.appendDescriptionOf(nodeMatcher);
				}
			};
			return this;
		}
	}

	@Factory
	public static ArrayNodeMatcher jsonArray() {
		return new ArrayNodeMatcher();
	}
}

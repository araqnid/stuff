package org.araqnid.stuff;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hamcrest.collection.IsIterableContainingInOrder;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Sets;

import static org.hamcrest.Matchers.equalTo;

public final class JsonStructureMatchers {
	public static Matcher<String> json(Matcher<? extends TreeNode> matcher) {
		return new TypeSafeDiagnosingMatcher<String>() {
			private final ObjectMapper objectMapper = new ObjectMapper();

			@Override
			protected boolean matchesSafely(String item, Description mismatchDescription) {
				JsonNode treeNode;
				try {
					treeNode = objectMapper.readTree(item);
				} catch (IOException e) {
					mismatchDescription.appendText("Invalid JSON: ").appendValue(e);
					return false;
				}
				matcher.describeMismatch(treeNode, mismatchDescription);
				return matcher.matches(treeNode);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("JSON ").appendDescriptionOf(matcher);
			}
		};
	}

	public static ObjectNodeMatcher jsonObject() {
		return new ObjectNodeMatcher();
	}

	public static class ObjectNodeMatcher extends TypeSafeDiagnosingMatcher<ObjectNode> {
		private final Map<String, Matcher<? extends TreeNode>> propertyMatchers = new LinkedHashMap<>();
		private boolean failOnUnexpectedProperties = true;

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
			if (failOnUnexpectedProperties && !remainingFieldNames.isEmpty()) {
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

		public ObjectNodeMatcher withProperty(String key, int value) {
			propertyMatchers.put(key, jsonInt(value));
			return this;
		}

		public ObjectNodeMatcher withProperty(String key, double value) {
			propertyMatchers.put(key, jsonDouble(value));
			return this;
		}

		public ObjectNodeMatcher withProperty(String key, boolean value) {
			propertyMatchers.put(key, jsonBoolean(value));
			return this;
		}

		public ObjectNodeMatcher withProperty(String key, String value) {
			propertyMatchers.put(key, jsonString(value));
			return this;
		}

		public ObjectNodeMatcher withPropertyLike(String key, String apparentValue) {
			propertyMatchers.put(key, jsonNodeStringifyingAs(apparentValue));
			return this;
		}

		public ObjectNodeMatcher withAnyFields() {
			failOnUnexpectedProperties = false;
			return this;
		}
	}

	public static Matcher<TextNode> jsonString(String value) {
		return jsonString(equalTo(value));
	}

	public static Matcher<TextNode> jsonString(Matcher<String> valueMatcher) {
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

	public static Matcher<NumericNode> jsonNumber(long n) {
		return new TypeSafeDiagnosingMatcher<NumericNode>() {
			@Override
			protected boolean matchesSafely(NumericNode item, Description mismatchDescription) {
				if (!item.canConvertToLong()) {
					mismatchDescription.appendText("not an integer value ").appendValue(item);
				}
				if (item.asLong() != n) {
					mismatchDescription.appendText("long value was ").appendValue(item.asInt());
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("JSON number(long) ").appendValue(n);
			}
		};
	}

	public static Matcher<NumericNode> jsonNumber(Matcher<Double> matcher) {
		return new TypeSafeDiagnosingMatcher<NumericNode>() {
			@Override
			protected boolean matchesSafely(NumericNode item, Description mismatchDescription) {
				double value = item.asDouble();
				mismatchDescription.appendText("numeric value ");
				matcher.describeMismatch(value, mismatchDescription);
				return matcher.matches(value);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("JSON numeric ").appendDescriptionOf(matcher);
			}
		};
	}

	public static Matcher<IntNode> jsonInt(final int n) {
		return new TypeSafeDiagnosingMatcher<IntNode>() {
			@Override
			protected boolean matchesSafely(IntNode item, Description mismatchDescription) {
				if (item.asInt() != n) {
					mismatchDescription.appendText("integer value was ").appendValue(item.asInt());
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("JSON int ").appendValue(n);
			}
		};
	}

	public static Matcher<LongNode> jsonLong(final long n) {
		return new TypeSafeDiagnosingMatcher<LongNode>() {
			@Override
			protected boolean matchesSafely(LongNode item, Description mismatchDescription) {
				if (item.asLong() != n) {
					mismatchDescription.appendText("long value was ").appendValue(item.asLong());
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("JSON long ").appendValue(n);
			}
		};
	}

	public static Matcher<DoubleNode> jsonDouble(final double n) {
		return new TypeSafeDiagnosingMatcher<DoubleNode>() {
			@Override
			protected boolean matchesSafely(DoubleNode item, Description mismatchDescription) {
				if (item.asDouble() != n) {
					mismatchDescription.appendText("double value was ").appendValue(item.asDouble());
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("JSON double ").appendValue(n);
			}
		};
	}

	public static Matcher<BooleanNode> jsonBoolean(final boolean b) {
		return new TypeSafeDiagnosingMatcher<BooleanNode>() {
			@Override
			protected boolean matchesSafely(BooleanNode item, Description mismatchDescription) {
				if (item.asBoolean() != b) {
					mismatchDescription.appendText("boolean value was ").appendValue(item.asBoolean());
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("JSON boolean ").appendValue(b);
			}
		};
	}

	public static Matcher<JsonNode> jsonNodeStringifyingAs(String str) {
		return new TypeSafeDiagnosingMatcher<JsonNode>() {
			@Override
			protected boolean matchesSafely(JsonNode item, Description mismatchDescription) {
				String itemString = item.toString();
				if (!itemString.equals(str)) {
					mismatchDescription.appendText("node is ").appendValue(itemString);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendValue(str);
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

	public static ArrayNodeMatcher jsonArray() {
		return new ArrayNodeMatcher();
	}

	private JsonStructureMatchers() {
	}
}

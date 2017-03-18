package org.araqnid.stuff.matchers;

import java.util.ArrayList;
import java.util.List;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.Text;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public final class XmlMatchers {
	public static Matcher<Document> containsXpath(String xpathExpr) {
		return new TypeSafeDiagnosingMatcher<Document>() {
			@Override
			protected boolean matchesSafely(Document doc, Description mismatchDescription) {
				Nodes nodes = doc.query(xpathExpr);
				mismatchDescription.appendText("xpath did not match any nodes");
				return nodes.size() > 0;
			}
			@Override
			public void describeTo(Description description) {
				description.appendText("document matching xpath ").appendValue(xpathExpr);
			}
		};
	}
	public static Matcher<Document> textAtXpathIs(String xpathExpr, String text) {
		return textAtXpath(xpathExpr, is(equalTo(text)));
	}

	public static Matcher<Document> textAtXpath(String xpathExpr, Matcher<String> textMatcher) {
		return new TypeSafeDiagnosingMatcher<Document>() {
			@Override
			protected boolean matchesSafely(Document doc, Description mismatchDescription) {
				Nodes selected = doc.query(xpathExpr);
				if (selected.size() == 0) {
					mismatchDescription.appendText("did not match any nodes");
					return false;
				}
				if (selected.size() > 1) {
					mismatchDescription.appendText("matched multiple nodes ").appendValue(selected);
					return false;
				}
				Node node = selected.get(0);
				if (node instanceof Element) {
					List<String> containedElements = new ArrayList<>();
					for (int i = 0; i < node.getChildCount(); i++) {
						Node childNode = node.getChild(i);
						if (childNode instanceof Element) {
							containedElements.add(((Element) childNode).getLocalName());
						}
					}
					if (!containedElements.isEmpty()) {
						mismatchDescription.appendText("matched non-leaf element containing ");
						mismatchDescription.appendValue(containedElements);
						return false;
					}
				} else if (!(node instanceof Text)) {
					mismatchDescription.appendText("was not a text node: ").appendValue(node);
					return false;
				}
				mismatchDescription.appendText(xpathExpr).appendText(" ");
				textMatcher.describeMismatch(node.getValue(), mismatchDescription);
				return textMatcher.matches(node.getValue());
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("xpath ").appendValue(xpathExpr).appendText(" ")
						.appendDescriptionOf(textMatcher);
			}
		};
	}

	private XmlMatchers() {
	}
}

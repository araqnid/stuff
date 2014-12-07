package org.araqnid.stuff.jsp;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.ValidationMessage;

public class ThingTagInfo extends TagExtraInfo {
	private static final Pattern UUID_PATTERN = Pattern
			.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

	@Override
	public ValidationMessage[] validate(TagData data) {
		List<ValidationMessage> messages = new ArrayList<>();
		Object idString = data.getAttribute("id");
		if (idString != TagData.REQUEST_TIME_VALUE) {
			Matcher matcher = UUID_PATTERN.matcher((String) idString);
			if (!matcher.matches()) {
				messages.add(new ValidationMessage(null, "Not a valid UUID"));
			}
		}
		return messages.toArray(new ValidationMessage[messages.size()]);
	}
}

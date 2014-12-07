package org.araqnid.stuff.jsp;

import java.beans.PropertyEditorManager;
import java.beans.PropertyEditorSupport;
import java.util.UUID;

import com.google.inject.Inject;

public class UUIDPropertyEditor extends PropertyEditorSupport {
	@Inject
	public static void register() {
		PropertyEditorManager.registerEditor(UUID.class, UUIDPropertyEditor.class);
	}

	private UUID uuid;

	@Override
	public void setValue(Object value) {
		uuid = (UUID) value;
	}

	@Override
	public Object getValue() {
		return uuid;
	}

	@Override
	public String getAsText() {
		return uuid != null ? uuid.toString() : null;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		uuid = text != null ? UUID.fromString(text) : null;
	}

}

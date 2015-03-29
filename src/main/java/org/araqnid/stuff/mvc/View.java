package org.araqnid.stuff.mvc;

import java.util.Map;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public final class View {
	public final String name;
	public final Map<String, Object> attributes;
	
	public View(String name, Map<String, Object> attributes) {
		this.name = name;
		this.attributes = ImmutableMap.copyOf(attributes);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).addValue(name).addValue(attributes).toString();
	}

	public static Builder of(String viewName) {
		return new Builder(viewName);
	}

	public static class Builder {
		private final String viewName;
		private final Map<String, Object> attributes = Maps.newHashMap();
		
		Builder(String viewName) {
			this.viewName = viewName;
		}

		public Builder put(String name, Object value) {
			Preconditions.checkNotNull(name);
			Preconditions.checkNotNull(value);
			attributes.put(name, value);
			return this;
		}

		public View build() {
			return new View(viewName, attributes);
		}
	}
}

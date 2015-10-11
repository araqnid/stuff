package org.araqnid.stuff.activity;

import java.util.UUID;

public class Activity {
	public final UUID id;
	public final ActivityNode root;
	public final ActivityEventSink sink;

	public Activity(UUID id, String type, Object attributes, ActivityEventSink sink) {
		this.id = id;
		this.sink = sink;
		this.root = new ActivityNode(this, null, type, attributes);
	}

	public void begin() {
		root.begin();
	}

	public void complete(boolean success, Object attributes) {
		root.complete(success, attributes);
	}
}

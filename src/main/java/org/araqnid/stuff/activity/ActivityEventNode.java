package org.araqnid.stuff.activity;

import com.google.common.base.Stopwatch;

public class ActivityEventNode {
	public final long id;
	public final String type;
	public final String description;
	public final Stopwatch stopwatch = Stopwatch.createStarted();
	public final ActivityEventNode parent;

	public ActivityEventNode(long id, String type, String description, ActivityEventNode parent) {
		this.id = id;
		this.type = type;
		this.description = description;
		this.parent = parent;
	}
}
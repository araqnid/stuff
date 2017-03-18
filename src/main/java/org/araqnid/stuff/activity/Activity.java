package org.araqnid.stuff.activity;

import java.util.UUID;
import javax.annotation.Nullable;

public class Activity implements Completable {
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

	@Override
	public void complete(boolean success, @Nullable Object attributes) {
		root.complete(success, attributes);
	}

	public static Completable.Rec<Activity> record(UUID id, String type, Object attributes, ActivityEventSink sink) {
		Activity activity = new Activity(id, type, attributes, sink);
		activity.begin();
		return new Completable.Rec<>(activity);
	}
}

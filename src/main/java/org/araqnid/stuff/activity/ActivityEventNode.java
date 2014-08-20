package org.araqnid.stuff.activity;

import java.util.Iterator;

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

	public Iterable<ActivityEventNode> stack() {
		return new Iterable<ActivityEventNode>() {
			@Override
			public Iterator<ActivityEventNode> iterator() {
				return new EventStackIterator(ActivityEventNode.this);
			}
		};
	}

	public static class EventStackIterator implements Iterator<ActivityEventNode> {
		private ActivityEventNode cursor;

		public EventStackIterator(ActivityEventNode cursor) {
			this.cursor = cursor;
		}

		@Override
		public boolean hasNext() {
			return cursor != null;
		}

		@Override
		public ActivityEventNode next() {
			ActivityEventNode next = cursor;
			cursor = cursor.parent;
			return next;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}

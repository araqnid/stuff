package org.araqnid.stuff.activity;

import java.util.Optional;

import org.slf4j.MDC;

import com.google.common.base.Preconditions;

public final class ThreadActivity {
	private static final ThreadLocal<ActivityNode> STATE = new ThreadLocal<>();

	public static void attach(ActivityNode node) {
		Preconditions.checkState(STATE.get() == null);
		STATE.set(node);
		MDC.put("activity", node.activity.id.toString());
	}

	public static void detach(Activity activity) {
		if (STATE.get() == null) return;
		ActivityNode currentNode = STATE.get();
		if (currentNode.activity != activity)
			throw new IllegalStateException("Activity in thread state did not match detach request");
		STATE.remove();
		MDC.remove("activity");
	}

	public static void detach(ActivityNode node) {
		if (STATE.get() == null) return;
		ActivityNode currentNode = STATE.get();
		if (currentNode != node)
			throw new IllegalStateException("Activity node in thread state did not match detach request");
		STATE.remove();
		MDC.remove("activity");
	}

	public static Optional<ActivityNode> current() {
		return Optional.ofNullable(STATE.get());
	}

	public static ActivityNode get() {
		return current().orElseThrow(() -> new IllegalStateException("No activity attached to this thread"));
	}

	public static void transition(ActivityNode from, ActivityNode to) {
		if (STATE.get() == from) STATE.set(to);
	}

	public static Scoper reattach(ActivityNode node) {
		attach(node);
		return new Scoper(node);
	}

	public static class Scoper implements AutoCloseable {
		private final ActivityNode node;

		public Scoper(ActivityNode node) {
			this.node = node;
		}

		@Override
		public void close() {
			ThreadActivity.detach(node.activity);
		}
	}

	private ThreadActivity() {
	}
}

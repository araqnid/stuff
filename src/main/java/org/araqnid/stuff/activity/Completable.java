package org.araqnid.stuff.activity;

public interface Completable {
	void complete(boolean success, Object completionAttributes);

	default void complete(boolean success) {
		complete(success, null);
	}

	class Rec<T extends Completable> implements AutoCloseable {
		private final T owner;
		private boolean success;
		private Object attributes;

		public Rec(T owner) {
			this.owner = owner;
		}

		public void markSuccess() {
			this.success = true;
		}

		public void markSuccess(Object attributes) {
			this.success = true;
			this.attributes = attributes;
		}

		@Override
		public void close() {
			owner.complete(success, attributes);
		}
	}
}

package org.araqnid.stuff.workqueue;

import org.araqnid.stuff.activity.AppEventType;
import org.araqnid.stuff.activity.RequestActivity;

import com.google.common.base.Joiner;

public class WorkDispatcher {
	private final WorkQueue queue;
	private final WorkProcessor processor;
	private final RequestActivity requestActivity;

	public WorkDispatcher(WorkQueue queue, WorkProcessor processor, RequestActivity requestActivity) {
		this.queue = queue;
		this.processor = processor;
		this.requestActivity = requestActivity;
	}

	public boolean process(String id, byte[] payload) {
		requestActivity.beginEvent(AppEventType.WorkQueueItem, Joiner.on('\t').join(queue.toString(), id));
		try {
			Work entry = new Work(id);
			entry.begin();
			try {
				processor.process(id, payload);
				entry.success();
				return true;
			} catch (PermanentWorkProcessorException e) {
				entry.permanentFailure(e.getMessage(), e);
				return true;
			} catch (Exception e) {
				entry.temporaryFailure(e);
				return false;
			} finally {
				entry.cleanup();
			}
		} finally {
			requestActivity.finishEvent(AppEventType.WorkQueueItem);
		}
	}

	private class Work {
		private final String id;
		private boolean marked;

		public Work(String id) {
			this.id = id;
		}

		public void begin() {
			queue.markInProgress(id);
			marked = true;
		}

		public void success() {
			queue.markProcessed(id);
			marked = true;
		}

		public void temporaryFailure(Throwable t) {
			queue.markFailed(id, false, null, t);
			marked = true;
		}

		public void permanentFailure(String message, Throwable t) {
			queue.markFailed(id, true, message, t);
			marked = true;
		}

		public void cleanup() {
			if (marked) return;
			queue.markFailed(id, false, "Exited processor without marking a result", null);
		}
	}
}

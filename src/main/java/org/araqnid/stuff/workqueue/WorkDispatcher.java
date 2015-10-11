package org.araqnid.stuff.workqueue;


public class WorkDispatcher {
	private final WorkQueue queue;
	private final WorkProcessor processor;

	public WorkDispatcher(WorkQueue queue, WorkProcessor processor) {
		this.queue = queue;
		this.processor = processor;
	}

	public boolean process(String id, byte[] payload) {
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

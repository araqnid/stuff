package org.araqnid.stuff.workqueue;

public interface WorkQueue {
	void markInProgress(String entryId);

	void markProcessed(String entryId);

	void markFailed(String entryId, boolean permanent, String message, Throwable t);
}

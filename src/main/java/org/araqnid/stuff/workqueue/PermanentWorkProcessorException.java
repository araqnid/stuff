package org.araqnid.stuff.workqueue;

public final class PermanentWorkProcessorException extends Exception {
	private static final long serialVersionUID = 1L;

	public PermanentWorkProcessorException(String message) {
		super(message);
	}

	public PermanentWorkProcessorException(String message, Throwable cause) {
		super(message, cause);
	}

	public PermanentWorkProcessorException(Throwable cause) {
		super(cause);
	}
}

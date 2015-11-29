package org.araqnid.stuff.zedis;

public final class ErrorMessage {
	private final String message;

	public ErrorMessage(String message) {
		this.message = message;
	}

	public String message() {
		return message;
	}

	@Override
	public String toString() {
		return "ErrorMessage(" + message + ")";
	}
}

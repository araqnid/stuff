package org.araqnid.stuff;

import org.araqnid.stuff.config.ActivityScoped;

import com.google.inject.Inject;

public class RequestActivityEvent {
	private final RequestActivity requestActivity;
	private final AppEventType type;
	private final String description;

	public RequestActivityEvent(RequestActivity requestActivity, AppEventType type, String description) {
		this.requestActivity = requestActivity;
		this.type = type;
		this.description = description;
	}

	public void begin() {
		requestActivity.beginEvent(type, description);
	}

	public void finish() {
		requestActivity.finishEvent(type);
	}

	@ActivityScoped
	public static class Factory {
		private final RequestActivity requestActivity;

		@Inject
		public Factory(RequestActivity requestActivity) {
			this.requestActivity = requestActivity;
		}

		public RequestActivityEvent beginEvent(AppEventType type) {
			RequestActivityEvent event = new RequestActivityEvent(requestActivity, type, null);
			event.begin();
			return event;
		}

		public RequestActivityEvent beginEvent(AppEventType type, String description) {
			RequestActivityEvent event = new RequestActivityEvent(requestActivity, type, description);
			event.begin();
			return event;
		}
	}
}

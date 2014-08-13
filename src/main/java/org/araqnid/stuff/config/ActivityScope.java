package org.araqnid.stuff.config;

import java.util.HashMap;
import java.util.Map;

import org.araqnid.stuff.RequestActivity;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;

public class ActivityScope {
	private static final ThreadLocal<Context> contexts = new ThreadLocal<>();
	public static final Scope SCOPE = new Scope() {
		@Override
		public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
			return new Provider<T>() {
				@Override
				public T get() {
					return acquireContext().scope(key, unscoped);
				}
			};
		}
	};

	public static class Control {
		private final Provider<RequestActivity> requestActivityProvider;

		@Inject
		public Control(Provider<RequestActivity> requestActivityProvider) {
			this.requestActivityProvider = requestActivityProvider;
		}

		public void beginRequest(String ruid, String type, String description) {
			if (contexts.get() != null) throw new IllegalStateException(
					"Activity context already attached to this thread");
			contexts.set(new Context());
			RequestActivity requestActivity = requestActivityProvider.get();
			if (ruid != null) requestActivity.setRuid(ruid);
			requestActivity.beginRequest(type, description);
		}

		public void finishRequest(String type) {
			RequestActivity requestActivity = requestActivityProvider.get();
			requestActivity.finishRequest(type);
			contexts.remove();
		}
	}

	private static Context acquireContext() {
		Context threadContext = contexts.get();
		if (threadContext == null) throw new OutOfScopeException("No activity context available in this thread");
		return threadContext;
	}

	private static class Context {
		private final Map<Key<?>, Object> contents = new HashMap<>();

		public <T> T scope(Key<T> key, Provider<T> unscoped) {
			@SuppressWarnings("unchecked")
			T value = (T) contents.get(key);
			if (value == null) {
				value = unscoped.get();
				contents.put(key, value);
			}
			return value;
		}
	}
}

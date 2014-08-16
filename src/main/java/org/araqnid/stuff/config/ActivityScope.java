package org.araqnid.stuff.config;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.araqnid.stuff.AppRequestType;
import org.araqnid.stuff.RequestActivity;
import org.araqnid.stuff.RequestActivity.ActivityEventSink;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;

public final class ActivityScope implements Scope {
	private final ThreadLocal<Context> contexts = new ThreadLocal<>();

	public static final class Module extends AbstractModule {
		private final ActivityScope scope;
		private final Class<? extends Annotation> scopeAnnotation;
		private final Key<ActivityEventSink> eventSink;

		public Module() {
			this(new ActivityScope(), ActivityScoped.class, Key.get(ActivityEventSink.class));
		}

		public Module(ActivityScope scope, Class<? extends Annotation> scopeAnnotation, Key<ActivityEventSink> eventSink) {
			this.scope = scope;
			this.scopeAnnotation = scopeAnnotation;
			this.eventSink = eventSink;
		}

		@Override
		protected void configure() {
			bindScope(scopeAnnotation, scope);
			bind(Control.class).toInstance(scope.createController(binder().getProvider(eventSink)));
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Module;
		}

		@Override
		public String toString() {
			return getClass().getName();
		}
	}

	@Override
	public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
		return new Provider<T>() {
			@Override
			public T get() {
				return acquireContext().scope(key, unscoped);
			}
		};
	}

	public Control createController(Provider<ActivityEventSink> sinkProvider) {
		return new ControlImpl(sinkProvider);
	}

	public interface Control {
		void beginRequest(AppRequestType type, String description);
		void beginRequest(String ruid, AppRequestType type, String description);
		void finishRequest(AppRequestType type);
	}

	private final class ControlImpl implements Control {
		private final Provider<ActivityEventSink> activitySinkProvider;

		@Inject
		public ControlImpl(Provider<ActivityEventSink> activitySinkProvider) {
			this.activitySinkProvider = activitySinkProvider;
		}

		@Override
		public void beginRequest(AppRequestType type, String description) {
			beginRequestWithRuid(newRuid(), type, description);
		}

		@Override
		public void beginRequest(String ruid, AppRequestType type, String description) {
			beginRequestWithRuid(ruid == null ? newRuid() : ruid, type, description);
		}

		private void beginRequestWithRuid(String ruid, AppRequestType type, String description) {
			if (contexts.get() != null) throw new IllegalStateException(
					"Activity context already attached to this thread");
			RequestActivity requestActivity = new RequestActivity(ruid, activitySinkProvider.get());
			Context context = new Context(requestActivity);
			requestActivity.beginRequest(type, description);
			contexts.set(context);
		}

		public String newRuid() {
			return UUID.randomUUID().toString();
		}

		@Override
		public void finishRequest(AppRequestType type) {
			Context context = acquireContext();
			RequestActivity requestActivity = context.getRequestActivity();
			requestActivity.finishRequest(type);
			contexts.remove();
		}
	}

	private Context acquireContext() {
		Context threadContext = contexts.get();
		if (threadContext == null) throw new OutOfScopeException("No activity context available in this thread");
		return threadContext;
	}

	private final class Context {
		private final Key<RequestActivity> activityKey = Key.get(RequestActivity.class);
		private final Map<Key<?>, Object> contents;

		private Context(RequestActivity requestActivity) {
			contents = new HashMap<>();
			contents.put(activityKey, requestActivity);
		}

		public RequestActivity getRequestActivity() {
			return (RequestActivity) contents.get(activityKey);
		}

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

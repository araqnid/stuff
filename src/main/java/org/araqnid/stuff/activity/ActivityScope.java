package org.araqnid.stuff.activity;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.ProviderWithDependencies;

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
			bind(ActivityScopeControl.class).toProvider(new ProviderWithDependencies<ActivityScopeControl>() {
				private final Provider<ActivityEventSink> sinkProvider = binder().getProvider(eventSink);
				private final Set<Dependency<?>> dependencies = ImmutableSet.<Dependency<?>> of(Dependency
						.get(eventSink));

				@Override
				public Set<Dependency<?>> getDependencies() {
					return dependencies;
				}

				@Override
				public ActivityScopeControl get() {
					return scope.createController(sinkProvider.get());
				}
			});
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

	public ActivityScopeControl createController(ActivityEventSink activityEventSink) {
		return new ControlImpl(activityEventSink);
	}

	private final class ControlImpl implements ActivityScopeControl {
		private final ActivityEventSink activityEventSink;

		@Inject
		public ControlImpl(ActivityEventSink activityEventSink) {
			this.activityEventSink = activityEventSink;
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
			if (contexts.get() != null)
				throw new IllegalStateException("Activity context already attached to this thread");
			RequestActivity requestActivity = new RequestActivity(ruid, activityEventSink);
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

	private static final class Context {
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

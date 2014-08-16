package org.araqnid.stuff.config;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.araqnid.stuff.RequestActivity;
import org.araqnid.stuff.RequestActivity.ActivityEventSink;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;

public final class ActivityScope<R extends Enum<R>, E extends Enum<E>> implements Scope {
	private final ThreadLocal<Context> contexts = new ThreadLocal<>();
	private final Key<RequestActivity<R, E>> key;

	@SuppressWarnings("unchecked")
	public ActivityScope(Class<R> requestTypeClass, Class<E> eventTypeClass) {
		this.key = (Key<RequestActivity<R, E>>) Key.get(type(RequestActivity.class, requestTypeClass, eventTypeClass));
	}

	public static final class Module<R extends Enum<R>, E extends Enum<E>> extends AbstractModule {
		private final Class<R> requestTypeClass;
		private final Class<E> eventTypeClass;

		public Module(Class<R> requestTypeClass, Class<E> eventTypeClass) {
			this.requestTypeClass = requestTypeClass;
			this.eventTypeClass = eventTypeClass;
		}

		@Override
		protected void configure() {
			ActivityScope<R, E> scope = new ActivityScope<>(requestTypeClass, eventTypeClass);
			bindScope(ActivityScoped.class, scope);

			@SuppressWarnings("unchecked")
			Key<ActivityEventSink<R, E>> sinkKey = (Key<ActivityEventSink<R, E>>) Key.get(type(ActivityEventSink.class,
					requestTypeClass, eventTypeClass));

			ParameterizedType controlType = type(Control.class, requestTypeClass);
			@SuppressWarnings("unchecked")
			Key<Control<R>> controlKey = (Key<Control<R>>) Key.get(controlType);

			bind(controlKey).toInstance(scope.createController(binder().getProvider(sinkKey)));
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

	private ControlImpl createController(Provider<ActivityEventSink<R, E>> activitySinkProvider) {
		return new ControlImpl(activitySinkProvider);
	}

	public interface Control<R extends Enum<R>> {
		void beginRequest(R type, String description);

		void beginRequest(String ruid, R type, String description);

		void finishRequest(R type);
	}

	public final class ControlImpl implements Control<R> {
		private final Provider<ActivityEventSink<R, E>> activitySinkProvider;

		public ControlImpl(Provider<ActivityEventSink<R, E>> activitySinkProvider) {
			this.activitySinkProvider = activitySinkProvider;
		}

		@Override
		public void beginRequest(R type, String description) {
			beginRequestWithRuid(newRuid(), type, description);
		}

		@Override
		public void beginRequest(String ruid, R type, String description) {
			beginRequestWithRuid(ruid == null ? newRuid() : ruid, type, description);
		}

		private void beginRequestWithRuid(String ruid, R type, String description) {
			if (contexts.get() != null) throw new IllegalStateException(
					"Activity context already attached to this thread");
			RequestActivity<R, E> requestActivity = new RequestActivity<R, E>(ruid, activitySinkProvider.get());
			Context context = new Context(requestActivity);
			requestActivity.beginRequest(type, description);
			contexts.set(context);
		}

		public String newRuid() {
			return UUID.randomUUID().toString();
		}

		@Override
		public void finishRequest(R type) {
			Context context = acquireContext();
			RequestActivity<R, E> requestActivity = context.getRequestActivity();
			requestActivity.finishRequest(type);
			contexts.remove();
		}
	}

	private Context acquireContext() {
		Context threadContext = contexts.get();
		if (threadContext == null) throw new OutOfScopeException("No activity context available in this thread");
		return threadContext;
	}

	private class Context {
		private final Map<Key<?>, Object> contents;

		private Context(RequestActivity<R, E> requestActivity) {
			contents = new HashMap<>();
			contents.put(key, requestActivity);
		}

		@SuppressWarnings("unchecked")
		public RequestActivity<R, E> getRequestActivity() {
			return (RequestActivity<R, E>) contents.get(key);
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

	private static final ParameterizedType type(final Class<?> rawType, final Type... paramTypes) {
		return new ParameterizedType() {
			@Override
			public Type getRawType() {
				return rawType;
			}

			@Override
			public Type getOwnerType() {
				return rawType.getEnclosingClass();
			}

			@Override
			public Type[] getActualTypeArguments() {
				return paramTypes;
			}
		};
	}

}

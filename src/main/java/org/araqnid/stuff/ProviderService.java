package org.araqnid.stuff;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.araqnid.stuff.ServiceActivator.ServiceNotActiveException;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Provider;

public abstract class ProviderService<T> extends AbstractIdleService implements Provider<T> {
	private final Class<T> clazz;

	protected ProviderService(Class<T> clazz) {
		this.clazz = clazz;
	}

	protected abstract T getValue();

	public Optional<T> getActiveValue() {
		if (!isRunning()) return Optional.absent();
		return Optional.of(getValue());
	}

	@SuppressWarnings("unchecked")
	public T getProxy() {
		return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { clazz },
				new ValueProxyInvocationHandler());
	}

	@Override
	public T get() {
		return getProxy();
	}

	private final class ValueProxyInvocationHandler implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Optional<T> activeService = getActiveValue();
			if (method.getName().equals("toString") && method.getParameterTypes().length == 0)
				return toString(activeService);
			if (method.getName().equals("hashCode") && method.getParameterTypes().length == 0) return getHashCode();
			if (method.getName().equals("equals") && method.getParameterTypes().length == 1) return isEqual(args[0]);
			if (!activeService.isPresent()) throw new ServiceNotActiveException();
			return method.invoke(activeService.get(), args);
		}

		private String toString(Optional<T> activeService) {
			if (activeService.isPresent()) {
				return "Activated: " + activeService.get();
			}
			else {
				return "Inactive";
			}
		}

		private int getHashCode() {
			return ProviderService.this.hashCode();
		}

		private ProviderService<T> provider() {
			return ProviderService.this;
		}

		private boolean isEqual(Object other) {
			if (other == null) return false;
			if (!Proxy.isProxyClass(other.getClass())) return false;
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(other);
			if (!(invocationHandler instanceof ProviderService.ValueProxyInvocationHandler)) return false;
			return ProviderService.this.equals(((ProviderService<?>.ValueProxyInvocationHandler) invocationHandler)
					.provider());
		}
	}

	public static class ValueNotPresentException extends IllegalStateException {
		private static final long serialVersionUID = 1L;
	}
}

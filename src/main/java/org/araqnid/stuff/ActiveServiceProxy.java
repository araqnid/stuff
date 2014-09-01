package org.araqnid.stuff;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.util.concurrent.Service;

public final class ActiveServiceProxy {
	private ActiveServiceProxy() {
	}

	@SuppressWarnings("unchecked")
	public static <S, T extends Service> S create(final ServiceActivator<T> activator, Class<S> serviceInterface) {
		return (S) Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class<?>[] { serviceInterface, Service.class },
				new ServiceProxyInvocationHandler<T>(new Supplier<Optional<T>>(){
					@Override
					public Optional<T> get() {
						return activator.getActiveService();
					}
				}));
	}

	private static final class ServiceProxyInvocationHandler<T> implements InvocationHandler {
		private final Supplier<Optional<T>> supplier;

		public ServiceProxyInvocationHandler(Supplier<Optional<T>> supplier) {
			this.supplier = supplier;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Optional<T> activeService = supplier.get();
			if (method.getName().equals("toString") && method.getParameterTypes().length == 0) return toString(activeService);
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
			return supplier.hashCode();
		}

		private boolean isEqual(Object other) {
			if (other == null) return false;
			if (!Proxy.isProxyClass(other.getClass())) return false;
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(other);
			if (!(invocationHandler instanceof ServiceProxyInvocationHandler)) return false;
			return supplier.equals(((ServiceProxyInvocationHandler<?>) invocationHandler).supplier);
		}
	}

	public static class ServiceNotActiveException extends IllegalStateException {
		private static final long serialVersionUID = 1L;

		public ServiceNotActiveException() {
		}
	}
}

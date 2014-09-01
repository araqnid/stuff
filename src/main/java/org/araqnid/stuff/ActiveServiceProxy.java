package org.araqnid.stuff;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Service;

public final class ActiveServiceProxy {
	private ActiveServiceProxy() {
	}

	public static <S, T extends Service> S create(final ServiceActivator<T> activator, Class<S> serviceInterface) {
		return create(new ServiceActivatorSupplier<T>(activator), serviceInterface, Service.class);
	}

	@SuppressWarnings("unchecked")
	public static <S, T> S create(final Supplier<Optional<T>> supplier, Class<S> serviceInterface, Class<?>... additionalInterfaces) {
		return (S) Proxy.newProxyInstance(serviceInterface.getClassLoader(),
				Iterables.toArray(Iterables.concat(ImmutableSet.of(serviceInterface), Arrays.asList(additionalInterfaces)), Class.class),
				new ServiceProxyInvocationHandler<T>(supplier));
	}

	private static final class ServiceActivatorSupplier<T extends Service> implements Supplier<Optional<T>> {
		private final ServiceActivator<T> activator;

		private ServiceActivatorSupplier(ServiceActivator<T> activator) {
			this.activator = activator;
		}

		@Override
		public Optional<T> get() {
			return activator.getActiveService();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ServiceActivatorSupplier)) return false;
			return activator.equals(((ServiceActivatorSupplier<?>) obj).activator);
		}

		@Override
		public int hashCode() {
			return activator.hashCode();
		}

		@Override
		public String toString() {
			return activator.toString();
		}
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

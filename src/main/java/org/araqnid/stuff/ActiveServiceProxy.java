package org.araqnid.stuff;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Service;

public final class ActiveServiceProxy {
	private ActiveServiceProxy() {
	}

	@SuppressWarnings("unchecked")
	public static <S, T extends Service> S create(ServiceActivator<T> activator, Class<S> serviceInterface) {
		return (S) Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class<?>[] { serviceInterface, Service.class },
				new ServiceProxyInvocationHandler<T>(activator));
	}

	private static final class ServiceProxyInvocationHandler<T extends Service> implements InvocationHandler {
		private final ServiceActivator<T> activator;

		public ServiceProxyInvocationHandler(ServiceActivator<T> activator) {
			this.activator = activator;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Optional<T> activeService = activator.getActiveService();
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
			return activator.hashCode();
		}

		private ServiceActivator<T> activator() {
			return activator;
		}

		private boolean isEqual(Object other) {
			if (other == null) return false;
			if (!Proxy.isProxyClass(other.getClass())) return false;
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(other);
			if (!(invocationHandler instanceof ServiceProxyInvocationHandler)) return false;
			return activator.equals(((ServiceProxyInvocationHandler<?>) invocationHandler).activator());
		}
	}

	public static class ServiceNotActiveException extends IllegalStateException {
		private static final long serialVersionUID = 1L;

		public ServiceNotActiveException() {
		}
	}
}

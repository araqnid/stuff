package org.araqnid.stuff;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Provider;

import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;

public class ServiceActivator<T extends Service> extends AbstractService {
	private final Provider<T> provider;
	private final boolean activateOnStartup;
	private T service;
	private List<ListenerExecutor> listeners = Lists.newLinkedList();

	public ServiceActivator(Provider<T> provider, boolean activateOnStartup) {
		this.provider = provider;
		this.activateOnStartup = activateOnStartup;
	}

	public synchronized void activate() {
		if (service != null) return;
		service = provider.get();
		service.startAsync();
		service.addListener(new Listener() {
			@Override
			public void running() {
				broadcastActivated();
			}

			@Override
			public void terminated(State from) {
				broadcastDeactivated();
				synchronized (ServiceActivator.this) {
					service = null;
				}
			}
		}, sameThreadExecutor());
	}

	public synchronized void deactivate() {
		if (service == null) return;
		service.stopAsync();
	}

	public static abstract class ActivationListener {
		public void created() {
		}

		public void activated() {
		}

		public void deactivated() {
		}
	}

	public synchronized void addActivationListener(final ActivationListener listener, Executor executor) {
		ListenerExecutor pair = new ListenerExecutor(listener, executor);
		boolean sendActivated = service.isRunning();
		listeners.add(pair);
		if (sendActivated) pair.sendActivated();
	}

	private void broadcastActivated() {
		for (final ListenerExecutor pair : listeners) {
			pair.sendActivated();
		}
	}

	private void broadcastDeactivated() {
		for (final ListenerExecutor pair : listeners) {
			pair.sendDeactivated();
		}
	}

	@Override
	protected void doStart() {
		if (!activateOnStartup) {
			notifyStarted();
			return;
		}
		activate();
		service.addListener(new Listener() {
			@Override
			public void running() {
				notifyStarted();
			}
		}, sameThreadExecutor());
	}

	@Override
	protected void doStop() {
		if (service == null) {
			notifyStopped();
			return;
		}
		deactivate();
		service.addListener(new Listener() {
			@Override
			public void terminated(State from) {
				notifyStopped();
			}
		}, sameThreadExecutor());
	}

	private static class ListenerExecutor {
		public final ActivationListener listener;
		public final Executor executor;

		public ListenerExecutor(ActivationListener listener, Executor executor) {
			this.listener = listener;
			this.executor = executor;
		}

		public void sendActivated() {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					listener.activated();
				}
			});
		}

		public void sendDeactivated() {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					listener.deactivated();
				}
			});
		}
	}

	public synchronized Optional<T> getActiveService() {
		if (service == null || !service.isRunning()) return Optional.absent();
		return Optional.of(service);
	}

	@SuppressWarnings("unchecked")
	public <S> S getActiveServiceProxy(Class<S> serviceInterface) {
		return (S) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { serviceInterface, Service.class }, new ServiceProxyInvocationHandler());
	}

	public Service getActiveServiceProxy(Class<?> serviceInterface, Class<?>... additionalInterfaces) {
		return (Service) Proxy
				.newProxyInstance(
						getClass().getClassLoader(),
						Iterables.toArray(
								Iterables.concat(ImmutableSet.of(serviceInterface, Service.class),
										Arrays.asList(additionalInterfaces)), Class.class),
						new ServiceProxyInvocationHandler());
	}

	private final class ServiceProxyInvocationHandler implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Optional<T> activeService = getActiveService();
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
			return ServiceActivator.this.hashCode();
		}

		private ServiceActivator<T> activator() {
			return ServiceActivator.this;
		}

		private boolean isEqual(Object other) {
			if (other == null) return false;
			if (!Proxy.isProxyClass(other.getClass())) return false;
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(other);
			if (!(invocationHandler instanceof ServiceActivator.ServiceProxyInvocationHandler)) return false;
			return ServiceActivator.this.equals(((ServiceActivator<?>.ServiceProxyInvocationHandler) invocationHandler)
					.activator());
		}
	}

	public static class ServiceNotActiveException extends IllegalStateException {
		private static final long serialVersionUID = 1L;

		public ServiceNotActiveException() {
		}
	}
}

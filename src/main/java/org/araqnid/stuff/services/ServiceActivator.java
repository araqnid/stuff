package org.araqnid.stuff.services;

import java.util.List;
import java.util.concurrent.Executor;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Provider;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

public class ServiceActivator<T extends Service> extends AbstractService implements Activator {
	private final Provider<T> provider;
	private final boolean activateOnStartup;
	private Optional<T> service = Optional.absent();
	private List<ListenerExecutor> listeners = Lists.newLinkedList();
	private boolean startupNotificationPending;
	private boolean shutdownNotificationPending;

	public ServiceActivator(Provider<T> provider, boolean activateOnStartup) {
		this.provider = provider;
		this.activateOnStartup = activateOnStartup;
	}

	@Override
	public synchronized void activate() {
		if (service.isPresent()) return;
		service = Optional.of(provider.get());
		service.get().addListener(new Listener() {
			@Override
			public void running() {
				broadcastActivated();
				synchronized (ServiceActivator.this) {
					if (startupNotificationPending) notifyStarted();
				}
			}

			@Override
			public void terminated(State from) {
				broadcastDeactivated();
				synchronized (ServiceActivator.this) {
					service = Optional.absent();
					if (shutdownNotificationPending) notifyStopped();
				}
			}

			@Override
			public void failed(State from, Throwable failure) {
				broadcastDeactivated();
				synchronized (ServiceActivator.this) {
					service = Optional.absent();
				}
				notifyFailed(failure);
			}
		}, directExecutor());
		service.get().startAsync();
	}

	@Override
	public synchronized void deactivate() {
		if (!service.isPresent()) return;
		service.get().stopAsync();
	}

	@Override
	public synchronized void addActivationListener(final Activator.ActivationListener listener, Executor executor) {
		ListenerExecutor pair = new ListenerExecutor(listener, executor);
		boolean sendActivated = service.get().isRunning();
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
		startupNotificationPending = true;
		activate();
	}

	@Override
	protected void doStop() {
		if (!service.isPresent()) {
			notifyStopped();
			return;
		}
		shutdownNotificationPending = true;
		deactivate();
	}

	@Override
	public String toString() {
		return "ServiceActivator{ " + (service.isPresent() ? service.get().toString() : "<empty> from " + provider)
				+ " } [" + state() + "]";
	}

	private static class ListenerExecutor {
		public final Activator.ActivationListener listener;
		public final Executor executor;

		public ListenerExecutor(Activator.ActivationListener listener, Executor executor) {
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
		if (!service.isPresent() || !service.get().isRunning()) return Optional.absent();
		return service;
	}
}

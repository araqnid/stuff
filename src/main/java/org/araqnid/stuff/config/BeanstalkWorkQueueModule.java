package org.araqnid.stuff.config;

import java.util.Map;
import java.util.Set;

import org.araqnid.stuff.BeanstalkProcessor;
import org.araqnid.stuff.RequestActivity;
import org.araqnid.stuff.workqueue.WorkDispatcher;
import org.araqnid.stuff.workqueue.WorkProcessor;
import org.araqnid.stuff.workqueue.WorkQueue;
import org.araqnid.stuff.workqueue.WorkQueueBeanstalkHandler;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.ProviderWithDependencies;

public abstract class BeanstalkWorkQueueModule extends AbstractModule {
	private Builder builder;

	@Override
	protected void configure() {
		builder = new Builder();
		try {
			configureDelivery();
			install(builder);
		} finally {
			builder = null;
		}
	}

	protected WorkQueueBinder process(String queueId) {
		return builder.process(queueId);
	}

	protected void into(Multibinder<? super BeanstalkProcessor> multibinder) {
		builder.into(multibinder);
	}

	protected abstract void configureDelivery();

	private class Builder implements Module {
		private Map<String, WorkQueueBinder> bindings = Maps.newHashMap();
		private Multibinder<? super BeanstalkProcessor> multibinder;

		@Override
		public void configure(Binder binder) {
			for (Map.Entry<String, WorkQueueBinder> e : bindings.entrySet()) {
				String queueId = e.getKey();
				binder.bind(Key.get(WorkQueueBeanstalkHandler.class, Names.named(queueId))).toProvider(
						makeQueueHandlerProvider(binder, queueId, e.getValue()));
			}
			install(new BeanstalkModule() {
				@Override
				protected void configureDelivery() {
					if (multibinder != null) into(multibinder);
					for (Map.Entry<String, WorkQueueBinder> e : bindings.entrySet()) {
						String queueId = e.getKey();
						process(queueId).with(Key.get(WorkQueueBeanstalkHandler.class, Names.named(queueId)));
					}
				}
			});
		}

		public void into(Multibinder<? super BeanstalkProcessor> multibinder) {
			this.multibinder = multibinder;
		}

		private Provider<WorkQueueBeanstalkHandler> makeQueueHandlerProvider(Binder binder, final String queueId, WorkQueueBinder queueBinding) {
			final Key<WorkProcessor> processorKey = queueBinding.bindWorkProcessor(binder);
			final Key<WorkQueue> queueKey = Key.get(WorkQueue.class, Names.named(queueId));
			final Provider<WorkQueue> queueProvider = binder.getProvider(queueKey);
			final Provider<WorkProcessor> processorProvider = binder.getProvider(processorKey);
			final Provider<RequestActivity> requestActivityProvider = binder.getProvider(RequestActivity.class);
			final Set<Dependency<?>> dependencies = ImmutableSet.<Dependency<?>> of(Dependency.get(queueKey), Dependency.get(processorKey),
					Dependency.get(Key.get(RequestActivity.class)));
			return new WorkQueueHandlerProvider(processorProvider, queueProvider, requestActivityProvider, dependencies, queueId);
		}

		public WorkQueueBinder process(String queueId) {
			if (bindings.containsKey(queueId)) throw new IllegalArgumentException("Queue '" + queueId + "' already bound");
			WorkQueueBinder queueBinder = new WorkQueueBinder(queueId);
			bindings.put(queueId, queueBinder);
			return queueBinder;
		}
	}

	public class WorkQueueBinder {
		private final String queueId;
		private Key<? extends WorkProcessor> targetKey;
		private Provider<? extends WorkProcessor> targetProvider;

		public WorkQueueBinder(String queueId) {
			this.queueId = queueId;
		}

		public WorkQueueBinder with(Class<? extends WorkProcessor> clazz) {
			return with(Key.get(clazz));
		}

		public WorkQueueBinder with(Key<? extends WorkProcessor> key) {
			targetKey = key;
			return this;
		}

		public WorkQueueBinder with(Provider<? extends WorkProcessor> provider) {
			targetProvider = provider;
			return this;
		}

		private Key<WorkProcessor> bindWorkProcessor(Binder binder) {
			if (targetKey != null)
				binder.bind(WorkProcessor.class).annotatedWith(Names.named(queueId)).to(targetKey);
			else if (targetProvider != null) binder.bind(WorkProcessor.class).annotatedWith(Names.named(queueId)).toProvider(targetProvider);
			return Key.get(WorkProcessor.class, Names.named(queueId));
		}
	}

	private static final class WorkQueueHandlerProvider implements ProviderWithDependencies<WorkQueueBeanstalkHandler> {
		private final Provider<WorkProcessor> processorProvider;
		private final Provider<WorkQueue> queueProvider;
		private final Provider<RequestActivity> requestActivityProvider;
		private final Set<Dependency<?>> dependencies;
		private final String queueId;

		private WorkQueueHandlerProvider(Provider<WorkProcessor> processorProvider, Provider<WorkQueue> queueProvider,
				Provider<RequestActivity> requestActivityProvider, Set<Dependency<?>> dependencies, String queueId) {
			this.processorProvider = processorProvider;
			this.queueProvider = queueProvider;
			this.requestActivityProvider = requestActivityProvider;
			this.dependencies = dependencies;
			this.queueId = queueId;
		}

		@Override
		public Set<Dependency<?>> getDependencies() {
			return dependencies;
		}

		@Override
		public WorkQueueBeanstalkHandler get() {
			return new WorkQueueBeanstalkHandler(queueId, new WorkDispatcher(queueProvider.get(), processorProvider.get()),
					requestActivityProvider.get());
		}
	}
}

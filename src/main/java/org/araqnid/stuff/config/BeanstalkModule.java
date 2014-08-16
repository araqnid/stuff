package org.araqnid.stuff.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.araqnid.stuff.BeanstalkProcessor;
import org.araqnid.stuff.BeanstalkProcessor.DeliveryTarget;
import org.araqnid.stuff.activity.ActivityScopeControl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.ProviderWithDependencies;
import com.surftools.BeanstalkClient.Client;

public abstract class BeanstalkModule extends AbstractModule {
	private BeanstalkModuleBuilder moduleBuilder;

	@Override
	protected void configure() {
		moduleBuilder = new BeanstalkModuleBuilder();
		try {
			configureDelivery();
			install(moduleBuilder);
		} finally {
			moduleBuilder = null;
		}
	}

	protected abstract void configureDelivery();

	protected void into(Multibinder<? super BeanstalkProcessor> multibinder) {
		moduleBuilder.multibinder = multibinder;
	}

	protected TubeBinding process() {
		return process("default");
	}

	protected TubeBinding process(String tubeName) {
		return moduleBuilder.createTubeBinding(tubeName);
	}

	public static class BeanstalkModuleBuilder implements Module {
		private final Collection<TubeBinding> tubeBindings = new ArrayList<>();
		private Multibinder<? super BeanstalkProcessor> multibinder;

		@Override
		public void configure(Binder binder) {
			if (multibinder == null) {
				multibinder = Multibinder.newSetBinder(binder, BeanstalkProcessor.class);
			}
			for (TubeBinding tubeBinding : tubeBindings) {
				tubeBinding.bind(binder, multibinder);
			}
		}

		protected TubeBinding createTubeBinding(String tubeName) {
			TubeBinding tubeBinding = new TubeBinding(tubeName);
			tubeBindings.add(tubeBinding);
			return tubeBinding;
		}
	}

	public static class TubeBinding {
		private final String tubeName;
		private Key<? extends DeliveryTarget> targetKey;
		private Provider<? extends DeliveryTarget> targetProvider;
		private int maxThreads = 1;

		private TubeBinding(String tubeName) {
			this.tubeName = tubeName;
		}

		public TubeBinding usingMaxThreads(int maxThreads) {
			this.maxThreads = maxThreads;
			return this;
		}

		public void with(Class<? extends DeliveryTarget> targetClass) {
			with(Key.get(targetClass));
		}

		public void with(Key<? extends DeliveryTarget> targetKey) {
			this.targetKey = targetKey;
		}

		public void with(Provider<? extends DeliveryTarget> targetProvider) {
			this.targetProvider = targetProvider;
		}

		private void bind(Binder binder, Multibinder<? super BeanstalkProcessor> multibinder) {
			multibinder.addBinding().toProvider(makeProvider(binder));
		}

		private Provider<BeanstalkProcessor> makeProvider(Binder binder) {
			Provider<Client> connectionProvider = binder.getProvider(Client.class);
			Provider<ActivityScopeControl> scopeControlProvider = binder.getProvider(ActivityScopeControl.class);
			Provider<BeanstalkProcessor> tubeProcessorProvider;
			MembersInjector<BeanstalkProcessor> membersInjector = binder.getMembersInjector(BeanstalkProcessor.class);
			if (targetKey != null) {
				Set<Dependency<?>> dependencies = ImmutableSet.<Dependency<?>> of(
						Dependency.get(Key.get(Client.class)), Dependency.get(Key.get(ActivityScopeControl.class)),
						Dependency.get(targetKey));
				tubeProcessorProvider = new TubeProcessorProvider(dependencies, membersInjector, connectionProvider,
						tubeName, maxThreads, scopeControlProvider, binder.getProvider(targetKey));
			}
			else if (targetProvider != null) {
				Set<Dependency<?>> dependencies = Sets.<Dependency<?>> newHashSet(
						Dependency.get(Key.get(Client.class)), Dependency.get(Key.get(ActivityScopeControl.class)));
				if (targetProvider instanceof ProviderWithDependencies) {
					dependencies.addAll(((ProviderWithDependencies<? extends DeliveryTarget>) targetProvider)
							.getDependencies());
				}
				tubeProcessorProvider = new TubeProcessorProvider(ImmutableSet.copyOf(dependencies), membersInjector,
						connectionProvider, tubeName, maxThreads, scopeControlProvider, targetProvider);
			}
			else {
				throw new IllegalStateException();
			}
			return tubeProcessorProvider;
		}
	}

	private static class TubeProcessorProvider implements ProviderWithDependencies<BeanstalkProcessor> {
		private final Set<Dependency<?>> dependencies;
		private final MembersInjector<BeanstalkProcessor> membersInjector;
		private final Provider<Client> connectionProvider;
		private final String tubeName;
		private final int maxThreads;
		private final Provider<ActivityScopeControl> scopeControlProvider;
		private final Provider<? extends DeliveryTarget> targetProvider;

		public TubeProcessorProvider(Set<Dependency<?>> dependencies,
				MembersInjector<BeanstalkProcessor> membersInjector, Provider<Client> connectionProvider,
				String tubeName, int maxThreads, Provider<ActivityScopeControl> scopeControlProvider,
				Provider<? extends DeliveryTarget> targetProvider) {
			this.dependencies = dependencies;
			this.membersInjector = membersInjector;
			this.connectionProvider = connectionProvider;
			this.tubeName = tubeName;
			this.maxThreads = maxThreads;
			this.scopeControlProvider = scopeControlProvider;
			this.targetProvider = targetProvider;
		}

		@Override
		public Set<Dependency<?>> getDependencies() {
			return dependencies;
		}

		@Override
		public BeanstalkProcessor get() {
			BeanstalkProcessor processor = new BeanstalkProcessor(connectionProvider, tubeName, maxThreads,
					scopeControlProvider.get(), targetProvider);
			membersInjector.injectMembers(processor);
			return processor;
		}
	}
}

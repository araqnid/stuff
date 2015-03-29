package org.araqnid.stuff.config;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.araqnid.stuff.ActivateOnStartup;
import org.araqnid.stuff.SometubeHandler;
import org.araqnid.stuff.activity.ActivityScopeControl;
import org.araqnid.stuff.messages.BeanstalkProcessor;
import org.araqnid.stuff.services.Activator;
import org.araqnid.stuff.services.ServiceActivator;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.surftools.BeanstalkClient.Client;
import com.surftools.BeanstalkClientImpl.ClientImpl;

public final class RawBeanstalkModule extends AbstractModule {
	@Override
	protected void configure() {
		bindLateService(new TypeLiteral<ServiceActivator<BeanstalkProcessor<SometubeHandler>>>() {
		});
	}

	@Provides
	@Singleton
	public ServiceActivator<BeanstalkProcessor<SometubeHandler>> sometubeActivator(Provider<BeanstalkProcessor<SometubeHandler>> provider) {
		return new ServiceActivator<>(provider, false);
	}

	@Provides
	public BeanstalkProcessor<SometubeHandler> sometubeProcessor(Provider<Client> connectionProvider,
			ActivityScopeControl scopeControl,
			Provider<SometubeHandler> targetProvider) {
		return new BeanstalkProcessor<>(connectionProvider, "sometube", scopeControl, targetProvider);
	}

	private <T extends ServiceActivator<?>> void bindLateService(TypeLiteral<T> type) {
		services().addBinding().to(type);
		activateOnStartup().addBinding().to(type);
	}

	private Multibinder<Activator> activateOnStartup() {
		return Multibinder.newSetBinder(binder(), Activator.class, ActivateOnStartup.OnStartup.class);
	}

	private Multibinder<Service> services() {
		return Multibinder.newSetBinder(binder(), Service.class);
	}

	@Provides
	public Client beanstalkClient() {
		ClientImpl client = new ClientImpl();
		client.setUniqueConnectionPerThread(false);
		return client;
	}
}

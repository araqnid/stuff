package org.araqnid.stuff.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.inject.Singleton;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import org.araqnid.stuff.AppStartupBanner;
import org.araqnid.stuff.AppVersion;
import org.araqnid.stuff.activity.ActivityEventSink;
import org.araqnid.stuff.activity.ActivityScope;
import org.araqnid.stuff.activity.LogActivityEvents;
import org.araqnid.stuff.activity.ThreadActivity;

public class AppModule extends AbstractModule {
	private Map<String, String> environment;

	public AppModule() {
		this(System.getenv());
	}

	@VisibleForTesting
	public AppModule(Map<String, String> environment) {
		this.environment = environment;
	}

	@Override
	protected void configure() {
		install(new JettyModule(port(61000)));
		bindConstant().annotatedWith(ServerIdentity.class).to(gethostname());
		bind(UUID.class).annotatedWith(ServerIdentity.class).toInstance(UUID.randomUUID());
		bind(Clock.class).toInstance(Clock.systemDefaultZone());
		bind(ActivityScope.class).toInstance(ThreadActivity::get);
		bind(ActivityEventSink.class).to(LogActivityEvents.class);

		Multibinder.newSetBinder(binder(), ServiceManager.Listener.class).addBinding().to(AppStartupBanner.class);
	}

	@Provides
	@Singleton
	public ServiceManager serviceManager(Set<Service> services, Set<ServiceManager.Listener> listeners) {
		ServiceManager serviceManager = new ServiceManager(services);
		listeners.forEach(serviceManager::addListener);
		return serviceManager;
	}

	@Provides
	@Singleton
	public AppVersion appVersion() {
		Package pkg = getClass().getPackage();
		String title = getClass().getPackage().getImplementationTitle();
		String vendor = pkg.getImplementationVendor();
		String version = getClass().getPackage().getImplementationVersion();
		return new AppVersion(title, vendor, version);
	}

	private Optional<String> getenv(String name) {
		return Optional.ofNullable(environment.get(name));
	}

	private int port(int defaultPort) {
		return getenv("PORT").map(Integer::valueOf).orElse(defaultPort);
	}

	private static String gethostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "localhost";
		}
	}
}

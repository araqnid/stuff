package org.araqnid.stuff.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.UUID;

import org.araqnid.stuff.ActivateOnStartup;
import org.araqnid.stuff.AppLifecycleEvent;
import org.araqnid.stuff.AppStateMonitor;
import org.araqnid.stuff.AppVersion;
import org.araqnid.stuff.MerlotRepository;
import org.araqnid.stuff.activity.ActivityScope;

import redis.clients.jedis.Jedis;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lexicalscope.eventcast.EventCast;

public final class CoreModule extends AbstractModule {
	@Override
	protected void configure() {
		install(EventCast.eventCastModuleBuilder().implement(AppLifecycleEvent.class).build());
		bind(AppVersion.class).toInstance(appVersion());
		bind(AppStateMonitor.class);
		bind(ActivateOnStartup.class);
		bind(JsonFactory.class).to(MappingJsonFactory.class).in(Singleton.class);
		bindConstant().annotatedWith(ServerIdentity.class).to(gethostname());
		bind(UUID.class).annotatedWith(ServerIdentity.class).toInstance(UUID.randomUUID());
		install(new ActivityScope.Module());
		install(new RawBeanstalkModule());
		install(new WorkQueueModule());
		install(new SynchronousActivityEventsModule());
		install(new JacksonModule());
		install(new SpooledEventsModule());
		bind(MerlotRepository.class);
	}

	@Provides
	public Jedis jedis() {
		return new Jedis("localhost");
	}

	@Provides
	@Singleton
	public ServiceManager serviceManager(Set<Service> services) {
		return new ServiceManager(services);
	}

	private AppVersion appVersion() {
		Package pkg = getClass().getPackage();
		String title = getClass().getPackage().getImplementationTitle();
		String vendor = pkg.getImplementationVendor();
		String version = getClass().getPackage().getImplementationVersion();
		return new AppVersion(title, vendor, version);
	}

	private static String gethostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "localhost";
		}
	}
}

package org.araqnid.stuff.config;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.araqnid.stuff.ActivateOnStartup;
import org.araqnid.stuff.AppLifecycleEvent;
import org.araqnid.stuff.AppStateMonitor;
import org.araqnid.stuff.AppVersion;
import org.araqnid.stuff.MerlotRepository;
import org.araqnid.stuff.activity.ActivityEventSink;
import org.araqnid.stuff.activity.ActivityScope;
import org.araqnid.stuff.activity.LogActivityEvents;
import org.araqnid.stuff.activity.ThreadActivity;
import org.araqnid.stuff.zedis.Zedis;
import org.araqnid.stuff.zedis.ZedisClient;

import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk7.Jdk7Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.guice.ObjectMapperModule;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.lexicalscope.eventcast.EventCast;

import redis.clients.jedis.Jedis;

public final class CoreModule extends AbstractModule {
	@Override
	protected void configure() {
		install(EventCast.eventCastModuleBuilder().implement(AppLifecycleEvent.class).build());
		bind(AppStateMonitor.class);
		bind(ActivateOnStartup.class);
		bindConstant().annotatedWith(ServerIdentity.class).to(gethostname());
		bind(UUID.class).annotatedWith(ServerIdentity.class).toInstance(UUID.randomUUID());
		install(new RawBeanstalkModule());
		install(new WorkQueueModule());
		install(new ObjectMapperModule().registerModule(GuavaModule.class).registerModule(NamingJacksonModule.class)
				.registerModule(Jdk7Module.class).registerModule(Jdk8Module.class).registerModule(JavaTimeModule.class)
				.registerModule(AfterburnerModule.class).registerModule(TextualTimestampsModule.class)
				.in(Singleton.class));
		install(new XmlMapperModule().registerModule(GuavaModule.class).registerModule(NamingJacksonModule.class)
				.registerModule(Jdk7Module.class).registerModule(Jdk8Module.class).registerModule(JavaTimeModule.class)
				.registerModule(AfterburnerModule.class).registerModule(TextualTimestampsModule.class)
				.usingJaxbAnnotations().in(Singleton.class));
		install(new SpooledEventsModule());
		install(new ElasticSearchModule("testcluster", new File(new File(System.getProperty("java.io.tmpdir")),
				"elasticsearch." + System.getProperty("user.name"))));
		bind(MerlotRepository.class);
		bind(Clock.class).toInstance(Clock.systemDefaultZone());
		bind(ActivityScope.class).toInstance(ThreadActivity::get);
		bind(ActivityEventSink.class).to(LogActivityEvents.class);
	}

	@Provides
	public Jedis jedis() {
		return new Jedis("localhost");
	}

	@Provides
	@Singleton
	@Named("zedis")
	public ExecutorService zedisThreadPool() {
		ExecutorService threadPool = Executors.newCachedThreadPool(
				new ThreadFactoryBuilder().setNameFormat("Zedis-localhost-6379-%d").setDaemon(true).build());
		return threadPool;
	}

	@Provides
	public Zedis zedis(@Named("zedis") ExecutorService threadPool) {
		return new Zedis(threadPool, "localhost", 6379);
	}

	@Provides
	@Singleton
	public ZedisClient zedisClient(@Named("zedis") ExecutorService threadPool) {
		ZedisClient client = new ZedisClient(threadPool, "localhost", 6379);
		client.start();
		return client;
	}

	@Provides
	@Singleton
	public ServiceManager serviceManager(Set<Service> services) {
		return new ServiceManager(services);
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

	private static String gethostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "localhost";
		}
	}
}

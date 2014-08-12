package org.araqnid.stuff.config;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;

import org.araqnid.stuff.AppService;
import org.araqnid.stuff.AppServicesManager;
import org.araqnid.stuff.AppStateServlet;
import org.araqnid.stuff.AppVersion;
import org.araqnid.stuff.AppVersionServlet;
import org.araqnid.stuff.AsyncActivityEventSink;
import org.araqnid.stuff.AsyncActivityEventsProcessor;
import org.araqnid.stuff.HelloResource;
import org.araqnid.stuff.LogActivityEvents;
import org.araqnid.stuff.RequestActivity;
import org.araqnid.stuff.RequestActivity.ActivityEventSink;
import org.araqnid.stuff.RequestActivityFilter;
import org.araqnid.stuff.RootServlet;
import org.araqnid.stuff.ScheduledJobs;
import org.araqnid.stuff.ScheduledJobs.CacheRefresher;
import org.araqnid.stuff.SomeQueueProcessor;
import org.araqnid.stuff.SometubeHandler;
import org.araqnid.stuff.workqueue.SqlWorkQueue;
import org.araqnid.stuff.workqueue.WorkQueue;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;
import com.surftools.BeanstalkClient.Client;
import com.surftools.BeanstalkClientImpl.ClientImpl;

public class AppConfig extends AbstractModule {
	@Override
	protected void configure() {
		bindConstant().annotatedWith(Names.named("http_port")).to(port(61000));

		install(new CoreModule());
		install(new RawBeanstalkModule());
		install(new WorkQueueModule());
		install(new ScheduledModule());
		install(new SynchronousActivityEventsModule());
		install(new WebModule());
		install(new JettyModule());
	}

	private int port(int defaultPort) {
		String envValue = System.getenv("PORT");
		if (envValue == null) return defaultPort;
		return Integer.valueOf(envValue);
	}

	public static final class CoreModule extends AbstractModule {
		@Override
		protected void configure() {
			Multibinder<AppService> appServices = Multibinder.newSetBinder(binder(), AppService.class);
			bind(AppServicesManager.class);
			appServices.addBinding().to(ScheduledJobs.class);
			bind(AppVersion.class).toInstance(appVersion());
		}

		private AppVersion appVersion() {
			Package pkg = getClass().getPackage();
			String title = getClass().getPackage().getImplementationTitle();
			String vendor = pkg.getImplementationVendor();
			String version = getClass().getPackage().getImplementationVersion();
			return new AppVersion(title, vendor, version);
		}
	}

	public static final class AsynchronousActivityEventsModule extends AbstractModule {
		@Override
		protected void configure() {
			Multibinder<AppService> appServices = Multibinder.newSetBinder(binder(), AppService.class);
			bind(ActivityEventSink.class).to(AsyncActivityEventSink.class);
			bind(ActivityEventSink.class).annotatedWith(Names.named("backend")).to(LogActivityEvents.class);
			appServices.addBinding().to(AsyncActivityEventsProcessor.class);
		}

		@Provides
		@Singleton
		public BlockingQueue<AsyncActivityEventSink.Event> activityEventQueue() {
			return new LinkedBlockingQueue<>();
		}

		@Provides
		public AsyncActivityEventsProcessor activityEventProcessor(@Named("backend") ActivityEventSink sink, BlockingQueue<AsyncActivityEventSink.Event> queue) {
			return new AsyncActivityEventsProcessor(sink, queue);
		}
	}

	public static final class SynchronousActivityEventsModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(ActivityEventSink.class).to(LogActivityEvents.class);
		}
	}

	public static final class RawBeanstalkModule extends BeanstalkModule {
		@Override
		protected void configureDelivery() {
			into(Multibinder.newSetBinder(binder(), AppService.class));
			process("sometube").with(SometubeHandler.class);
		}

		@Provides
		public Client beanstalkClient() {
			ClientImpl client = new ClientImpl();
			client.setUniqueConnectionPerThread(false);
			return client;
		}
	}

	public static final class WorkQueueModule extends BeanstalkWorkQueueModule {
		@Override
		protected void configureDelivery() {
			into(Multibinder.newSetBinder(binder(), AppService.class));
			process("somequeue").with(SomeQueueProcessor.class);
			process("otherqueue").with(SomeQueueProcessor.class);
		}

		@Provides
		@Named("somequeue")
		public WorkQueue somequeue() {
			return new SqlWorkQueue("somequeue");
		}

		@Provides
		@Named("otherqueue")
		public WorkQueue otherqueue() {
			return new SqlWorkQueue("otherqueue");
		}
	}

	public static final class ScheduledModule extends ScheduledJobsModule {
		@Override
		protected void configureJobs() {
			run(CacheRefresher.class).withInterval(60*1000L);
		}
	}

	public static final class WebModule extends ServletModule {
		@Override
		protected void configureServlets() {
			serve("/").with(RootServlet.class);
			serve("/_info/state").with(AppStateServlet.class);
			serve("/_info/version").with(AppVersionServlet.class);
			filter("/*").through(RequestActivityFilter.class);
		}
	}

	public static final class JettyModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(GuiceResteasyBootstrapServletContextListener.class).toInstance(
					new GuiceResteasyBootstrapServletContextListener() {
						@Override
						protected List<? extends Module> getModules(ServletContext context) {
							return ImmutableList.of(new ResteasyModule());
						}
					});
		}

		@Provides
		@Singleton
		public Connector connector(@Named("http_port") int port) {
			Connector connector = new SelectChannelConnector();
			connector.setPort(port);
			return connector;
		}

		@Provides
		@Singleton
		@Named("vanilla")
		public Handler vanillaContext(GuiceFilter guiceFilter, @Named("webapp-root") Resource baseResource) {
			ServletContextHandler context = new ServletContextHandler();
			context.setContextPath("/");
			context.addFilter(new FilterHolder(guiceFilter), "/*", EnumSet.of(DispatcherType.REQUEST));
			context.addServlet(DefaultServlet.class, "/");
			context.setBaseResource(baseResource);
			return context;
		}

		@Provides
		@Named("webapp-root")
		public Resource webappRoot() {
			return new EmbeddedResource(getClass().getClassLoader(), "web");
		}

		@Provides
		@Singleton
		@Named("resteasy")
		public Handler resteasyContext(GuiceResteasyBootstrapServletContextListener listener,
				@Named("resteasy") RequestActivityFilter activityFilter) {
			ServletContextHandler context = new ServletContextHandler();
			context.setContextPath("/_api");
			context.addEventListener(listener);
			context.addServlet(new ServletHolder(HttpServletDispatcher.class), "/");
			context.addFilter(new FilterHolder(activityFilter), "/*", EnumSet.of(DispatcherType.REQUEST));
			return context;
		}

		@Provides
		@Named("resteasy")
		public RequestActivityFilter hackedFilterForResteasy(final ActivityEventSink activityEventSink) {
			RequestActivityFilter activityFilter = new RequestActivityFilter(new Provider<RequestActivity>() {
				@Override
				public RequestActivity get() {
					return new RequestActivity(activityEventSink);
				}
			});
			return activityFilter;
		}

		@Provides
		@Singleton
		public Handler handler(@Named("vanilla") Handler vanillaContext, @Named("resteasy") Handler resteasyContext) {
			ContextHandlerCollection contexts = new ContextHandlerCollection();
			contexts.setHandlers(new Handler[] { vanillaContext, resteasyContext });
			return contexts;
		}

		@Provides
		@Singleton
		public Server server(Connector connector, Handler handler) {
			Server server = new Server();
			server.setConnectors(new Connector[] { connector });
			server.setHandler(handler);
			return server;
		}
	}

	public static final class ResteasyModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(HelloResource.class);
		}
	}
}

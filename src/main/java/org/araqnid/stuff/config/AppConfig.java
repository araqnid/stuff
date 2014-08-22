package org.araqnid.stuff.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;

import org.araqnid.stuff.RootServlet;
import org.araqnid.stuff.ServerIdentityFilter;
import org.araqnid.stuff.activity.ActivityScope;
import org.araqnid.stuff.activity.RequestActivityFilter;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Exposed;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.ServletModule;

public class AppConfig extends AbstractModule {
	private Map<String, String> environment;

	static String gethostname() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "localhost";
		}
	}

	public AppConfig() {
		this(System.getenv());
	}

	@VisibleForTesting
	public AppConfig(Map<String, String> environment) {
		this.environment = environment;
	}

	@Override
	protected void configure() {
		bindConstant().annotatedWith(Names.named("http_port")).to(port(61000));
		install(new ActivityScope.Module());
		install(new CoreModule());
		install(new RawBeanstalkModule());
		install(new WorkQueueModule());
		install(new ScheduledModule());
		install(new SynchronousActivityEventsModule());
		install(new JettyModule());
	}

	private int port(int defaultPort) {
		String envValue = environment.get("PORT");
		if (envValue == null) return defaultPort;
		return Integer.valueOf(envValue);
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
			install(new VanillaContextModule());
			install(new ResteasyContextModule());
		}

		public static final class VanillaContextModule extends PrivateModule {
			@Override
			protected void configure() {
				install(new WebModule());
			}

			public static final class WebModule extends ServletModule {
				@Override
				protected void configureServlets() {
					serve("/").with(RootServlet.class);
					filter("/*").through(RequestActivityFilter.class);
					filter("/*").through(ServerIdentityFilter.class);
				}
			}

			@Provides
			@Named("vanilla")
			@Exposed
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
		}

		public static final class ResteasyContextModule extends PrivateModule {
			@Override
			protected void configure() {
				install(new WebModule());
				expose(HttpServletDispatcher.class);
			}

			public static final class WebModule extends ServletModule {
				@Override
				protected void configureServlets() {
					bind(HttpServletDispatcher.class).in(Singleton.class);
					serve("/*").with(HttpServletDispatcher.class);
					filter("/*").through(RequestActivityFilter.class);
					filter("/*").through(ServerIdentityFilter.class);
				}
			}

			@Provides
			@Named("resteasy")
			@Exposed
			public Handler resteasyContext(GuiceFilter guiceFilter,
					GuiceResteasyBootstrapServletContextListener listener) {
				ServletContextHandler context = new ServletContextHandler();
				context.setContextPath("/_api");
				context.addFilter(new FilterHolder(guiceFilter), "/*", EnumSet.of(DispatcherType.REQUEST));
				context.addServlet(DefaultServlet.class, "/");
				context.addEventListener(listener);
				return context;
			}
		}

		@Provides
		public Connector connector(@Named("http_port") int port) {
			Connector connector = new SelectChannelConnector();
			connector.setPort(port);
			return connector;
		}

		@Provides
		public Handler handler(@Named("vanilla") Handler vanillaContext, @Named("resteasy") Handler resteasyContext) {
			ContextHandlerCollection contexts = new ContextHandlerCollection();
			contexts.setHandlers(new Handler[] { vanillaContext, resteasyContext });
			return contexts;
		}

		@Provides
		public Server server(Connector connector, Handler handler) {
			Server server = new Server();
			server.setConnectors(new Connector[] { connector });
			server.setHandler(handler);
			return server;
		}
	}
}

package org.araqnid.stuff.config;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;

import org.araqnid.stuff.AppStartupBanner;
import org.araqnid.stuff.JettyAppService;
import org.araqnid.stuff.activity.RequestActivityFilter;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Exposed;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceFilter;

public class StandaloneAppConfig extends AbstractModule {
	private static final TypeLiteral<Optional<String>> OptionalString = new TypeLiteral<Optional<String>>() {
	};
	private Map<String, String> environment;

	public StandaloneAppConfig() {
		this(System.getenv());
	}

	@VisibleForTesting
	public StandaloneAppConfig(Map<String, String> environment) {
		this.environment = environment;
	}

	@Override
	protected void configure() {
		install(new CoreModule());
		install(new JettyModule(port(61000)));
		bind(AppStartupBanner.class);
		bind(OptionalString).annotatedWith(Names.named("pgUser")).toInstance(getenv("PGUSER"));
		bind(OptionalString).annotatedWith(Names.named("pgPassword")).toInstance(getenv("PGPASSWORD"));
		bind(OptionalString).annotatedWith(Names.named("pgDatabase")).toInstance(getenv("PGDATABASE"));
	}

	private Optional<String> getenv(String name) {
		return Optional.fromNullable(environment.get(name));
	}

	private int port(int defaultPort) {
		return getenv("PORT").transform(new Function<String, Integer>() {
			@Override
			public Integer apply(String input) {
				return Integer.valueOf(input);
			}
		}).or(defaultPort);
	}

	public static final class JettyModule extends AbstractModule {
		private final int port;

		public JettyModule(int port) {
			this.port = port;
		}

		@Override
		protected void configure() {
			bind(GuiceResteasyBootstrapServletContextListener.class).toInstance(
					new GuiceResteasyBootstrapServletContextListener() {
						@Override
						protected List<? extends Module> getModules(ServletContext context) {
							return ImmutableList.of(new ResteasyModule());
						}
					});
			bind(RequestActivityFilter.RequestLogger.class).to(RequestActivityFilter.BasicRequestLogger.class);
			Multibinder<Service> services = Multibinder.newSetBinder(binder(), Service.class);
			services.addBinding().to(JettyAppService.class);
			install(new VanillaContextModule());
			install(new ResteasyContextModule());
		}

		public static final class VanillaContextModule extends PrivateModule {
			@Override
			protected void configure() {
				install(new VanillaServletModule());
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
				install(new ResteasyServletModule());
				expose(HttpServletDispatcher.class);
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
		public Handler handler(@Named("vanilla") Handler vanillaContext, @Named("resteasy") Handler resteasyContext) {
			ContextHandlerCollection contexts = new ContextHandlerCollection();
			contexts.setHandlers(new Handler[] { vanillaContext, resteasyContext });
			return contexts;
		}

		@Provides
		@Singleton
		public Server server(Handler handler) {
			Server server = new Server();
			ServerConnector connector = new ServerConnector(server);
			connector.setPort(port);
			server.setConnectors(new Connector[] { connector });
			server.setHandler(handler);
			return server;
		}
	}
}

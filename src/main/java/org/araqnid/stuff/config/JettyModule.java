package org.araqnid.stuff.config;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import javax.inject.Singleton;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.ClassPath;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.servlet.GuiceFilter;
import org.araqnid.stuff.JettyService;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.Filter30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.FilterDispatcher;

public final class JettyModule extends AbstractModule {
	private final int port;

	JettyModule(int port) {
		this.port = port;
	}

	@Override
	protected void configure() {
		bind(GuiceResteasyBootstrapServletContextListener.class)
				.toInstance(new GuiceResteasyBootstrapServletContextListener() {
					@Override
					protected List<? extends Module> getModules(ServletContext context) {
						return ImmutableList.of(new ResteasyModule());
					}
				});
		services().addBinding().to(JettyService.class);
		install(new ServletDispatchModule());
		bind(FilterDispatcher.class).to(Filter30Dispatcher.class);
		bind(Filter30Dispatcher.class).in(Singleton.class);
	}

	@Provides
	public Handler context(GuiceFilter guiceFilter,
			Resource baseResource,
			GuiceResteasyBootstrapServletContextListener resteasyListener) {
		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		context.addFilter(new FilterHolder(guiceFilter), "/*", EnumSet.of(DispatcherType.REQUEST));
		context.addServlet(DefaultServlet.class, "/");
		context.setBaseResource(baseResource);
		context.addEventListener(resteasyListener);

		return gzip(context);
	}

	private Handler gzip(Handler underlying) {
		GzipHandler gzipHandler = new GzipHandler();
		gzipHandler.setHandler(underlying);
		return gzipHandler;
	}

	@Provides
	public Resource webappRoot() throws IOException {
		if (getClass().getResource("/stuff/web/index.html") != null) {
			ClassLoader classLoader = getClass().getClassLoader();
			return new EmbeddedResource(classLoader, "stuff/web", ClassPath.from(classLoader));
		}
		else {
			return new PathResource(Paths.get("web"));
		}
	}

	@Provides
	@Singleton
	public Server server(Handler handler) {
		Server server = new Server();
		HttpConfiguration config = new HttpConfiguration();
		HttpConnectionFactory http1 = new HttpConnectionFactory(config);
		HTTP2CServerConnectionFactory http2c = new HTTP2CServerConnectionFactory(config);
		ServerConnector connector = new ServerConnector(server, http1, http2c);
		connector.setPort(port);
		server.setConnectors(new Connector[] { connector });
		server.setSessionIdManager(new DefaultSessionIdManager(server));
		server.setHandler(handler);
		server.getBean(QueuedThreadPool.class).setName("Jetty");
		return server;
	}

	private Multibinder<Service> services() {
		return Multibinder.newSetBinder(binder(), Service.class);
	}
}

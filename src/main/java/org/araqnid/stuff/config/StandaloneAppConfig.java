package org.araqnid.stuff.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.jsp.tagext.TagAttributeInfo;

import org.apache.jasper.servlet.JspServlet;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.util.descriptor.tld.TagXml;
import org.apache.tomcat.util.descriptor.tld.TaglibXml;
import org.araqnid.stuff.AppStartupBanner;
import org.araqnid.stuff.JettyAppService;
import org.araqnid.stuff.activity.RequestActivityFilter;
import org.araqnid.stuff.jsp.InjectedInstanceManager;
import org.araqnid.stuff.jsp.ThingTag;
import org.araqnid.stuff.jsp.ThingTagInfo;
import org.araqnid.stuff.jsp.UUIDPropertyEditor;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.FileResource;
import org.eclipse.jetty.util.resource.Resource;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.common.reflect.ClassPath;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Exposed;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.servlet.GuiceFilter;

public class StandaloneAppConfig extends AbstractModule {
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
	}

	@Provides
	@Named("pgUser")
	public Optional<String> pgUser() {
		return getenv("PGUSER");
	}

	@Provides
	@Named("pgPassword")
	public Optional<String> pgPassword() {
		return getenv("PGPASSWORD");
	}

	@Provides
	@Named("pgDatabase")
	public Optional<String> pgDatabase() {
		return getenv("PGDATABASE");
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
			requestStaticInjection(UUIDPropertyEditor.class);
		}

		public static final class VanillaContextModule extends PrivateModule {
			@Override
			protected void configure() {
				install(new VanillaServletModule());
				bind(InstanceManager.class).to(InjectedInstanceManager.class);
			}

			@Provides
			@Named("vanilla")
			@Exposed
			public Handler vanillaContext(GuiceFilter guiceFilter,
					@Named("webapp-root") Resource baseResource,
					InstanceManager instanceManager,
					Map<String, TaglibXml> embeddedTaglibs) {
				// Set Classloader of Context to be sane (needed for JSTL)
				// JSP requires a non-System classloader, this simply wraps the
				// embedded System classloader in a way that makes it suitable
				// for JSP to use
				ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());

				final File jspTempDir = Files.createTempDir();
				ServletContextHandler context = new ServletContextHandler();
				context.setContextPath("/");
				context.addFilter(new FilterHolder(guiceFilter), "/*", EnumSet.of(DispatcherType.REQUEST));
				context.addServlet(DefaultServlet.class, "/");
				context.addServlet(JspServlet.class, "*.jsp");
				context.setBaseResource(baseResource);
				context.setClassLoader(jspClassLoader);
				context.setAttribute("javax.servlet.context.tempdir", jspTempDir);
				context.setAttribute(InstanceManager.class.getName(), instanceManager);
				context.addEventListener(new JettyJspServletContextListener(jspTempDir, embeddedTaglibs));
				return context;
			}

			@Provides
			@Named("webapp-root")
			public Resource webappRoot() throws IOException {
				if (getClass().getResource("/stuff/web/index.html") != null) {
					ClassLoader classLoader = getClass().getClassLoader();
					return new EmbeddedResource(classLoader, "stuff/web", ClassPath.from(classLoader));
				}
				else {
					File documentRoot = new File("web");
					System.out.println("document root = " + documentRoot);
					return new FileResource(documentRoot.toURI());
				}
			}

			@Provides
			public Map<String, TaglibXml> embeddedTaglibs() {
				String uri = "file://localhost/araqnid/stuff/embedded";
				TaglibXml taglibXml = new TaglibXml();
				taglibXml.setTlibVersion("1.0");
				taglibXml.setJspVersion("1.2");
				taglibXml.setShortName("embd");
				taglibXml.setUri(uri);
				taglibXml.setInfo("embedded taglib");

				TagXml tagXml = new TagXml();
				tagXml.setName("thing");
				tagXml.setTagClass(ThingTag.class.getName());
				tagXml.setTeiClass(ThingTagInfo.class.getName());
				tagXml.setBodyContent("scriptless");
				tagXml.setInfo("Test tag");
				TagAttributeInfo attr = new TagAttributeInfo("id", true, null, false);
				tagXml.getAttributes().add(attr);
				taglibXml.addTag(tagXml);

				return ImmutableMap.of(uri, taglibXml);
			}
		}

		public static final class ResteasyContextModule extends PrivateModule {
			@Override
			protected void configure() {
				install(new ResteasyServletModule());
				expose(HttpServlet30Dispatcher.class);
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

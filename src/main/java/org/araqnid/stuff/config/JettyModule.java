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
import org.eclipse.jetty.server.SessionIdManager;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.FileResource;
import org.eclipse.jetty.util.resource.Resource;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.springframework.web.servlet.DispatcherServlet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.common.reflect.ClassPath;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Exposed;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.servlet.GuiceFilter;

public final class JettyModule extends AbstractModule {
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
		bind(SessionIdManager.class).to(HashSessionIdManager.class);
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
				Map<String, TaglibXml> embeddedTaglibs,
				Injector injector) {
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
			ServletHolder mvcServlet = new ServletHolder("mvc", DispatcherServlet.class);
			mvcServlet.setInitOrder(1);
			context.addServlet(mvcServlet, "/mvc/*");
			context.setBaseResource(baseResource);
			context.setClassLoader(jspClassLoader);
			context.setAttribute("javax.servlet.context.tempdir", jspTempDir);
			context.setAttribute(InstanceManager.class.getName(), instanceManager);
			context.setAttribute(Injector.class.getName(), injector);
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
				return new FileResource(new File("web").toURI());
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
			bind(SessionManager.class).to(HashSessionManager.class);
			expose(HttpServlet30Dispatcher.class);
		}

		@Provides
		@Named("resteasy")
		@Exposed
		public Handler resteasyContext(GuiceFilter guiceFilter,
				GuiceResteasyBootstrapServletContextListener listener,
				SessionManager sessionManager) {
			ServletContextHandler context = new ServletContextHandler();
			context.setContextPath("/_api");
			context.setSessionHandler(new SessionHandler(sessionManager));
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
	public Server server(Handler handler, SessionIdManager sessionIdManager) {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.setConnectors(new Connector[] { connector });
		server.setSessionIdManager(sessionIdManager);
		server.setHandler(handler);
		return server;
	}
}
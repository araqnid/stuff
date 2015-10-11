package org.araqnid.stuff.config;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;

import org.araqnid.stuff.EmbeddedAppStartupBanner;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class EmbeddedWebappConfig extends AbstractModule {
	private final ServletContext context;

	public EmbeddedWebappConfig(ServletContext context) {
		this.context = context;
	}

	@Override
	protected void configure() {
		bindConstant().annotatedWith(Names.named("context_path")).to(context.getContextPath());
		Multibinder<ServletContextListener> servletContextListeners = Multibinder.newSetBinder(binder(),
				ServletContextListener.class);
		servletContextListeners.addBinding().toInstance(new GuiceResteasyBootstrapServletContextListener() {
			@Override
			protected List<? extends Module> getModules(ServletContext context) {
				return ImmutableList.of(new ResteasyModule());
			}
		});
		install(new CoreModule());
		install(new ServletDispatchModule());
		bind(EmbeddedAppStartupBanner.class);
	}

}

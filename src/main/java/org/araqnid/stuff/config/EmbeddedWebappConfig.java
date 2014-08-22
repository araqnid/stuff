package org.araqnid.stuff.config;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.araqnid.stuff.ScheduledJobController;
import org.araqnid.stuff.activity.RequestActivityFilter;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class EmbeddedWebappConfig extends AbstractModule {
	@Override
	protected void configure() {
		bindConstant().annotatedWith(Names.named("http_port")).to(0);
		Multibinder.newSetBinder(binder(), ScheduledJobController.JobDefinition.class);
		bind(GuiceResteasyBootstrapServletContextListener.class).toInstance(new GuiceResteasyBootstrapServletContextListener() {
			@Override
			protected List<? extends Module> getModules(ServletContext context) {
				return ImmutableList.of(new ResteasyModule());
			}
		});
		install(new CoreModule());
		install(new ResteasyServletModule());
		if (servletApiSupportsRequestGetStatus()) {
			bind(RequestActivityFilter.RequestLogger.class).to(RequestActivityFilter.BasicRequestLogger.class);
		} else {
			bind(RequestActivityFilter.RequestLogger.class).to(RequestActivityFilter.NoStatusRequestLogger.class);
		}
	}

	private static boolean servletApiSupportsRequestGetStatus() {
		try {
			HttpServletResponse.class.getMethod("getStatus", new Class[0]);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}
}

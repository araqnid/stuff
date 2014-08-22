package org.araqnid.stuff.config;

import java.util.UUID;

import org.araqnid.stuff.AppLifecycleEvent;
import org.araqnid.stuff.AppService;
import org.araqnid.stuff.AppServicesManager;
import org.araqnid.stuff.AppStartupBanner;
import org.araqnid.stuff.AppStateMonitor;
import org.araqnid.stuff.AppVersion;
import org.araqnid.stuff.JettyAppService;
import org.araqnid.stuff.ScheduledJobController;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.lexicalscope.eventcast.EventCast;

public final class CoreModule extends AbstractModule {
	@Override
	protected void configure() {
		install(EventCast.eventCastModuleBuilder()
				.implement(AppLifecycleEvent.class)
				.build());
		Multibinder<AppService> appServices = Multibinder.newSetBinder(binder(), AppService.class);
		bind(AppServicesManager.class);
		appServices.addBinding().to(JettyAppService.class);
		appServices.addBinding().to(ScheduledJobController.class);
		bind(AppVersion.class).toInstance(appVersion());
		bind(AppStateMonitor.class);
		bind(AppStartupBanner.class);
		bind(JsonFactory.class).to(MappingJsonFactory.class).in(Singleton.class);
		bindConstant().annotatedWith(ServerIdentity.class).to(AppConfig.gethostname());
		bind(UUID.class).annotatedWith(ServerIdentity.class).toInstance(UUID.randomUUID());
	}

	private AppVersion appVersion() {
		Package pkg = getClass().getPackage();
		String title = getClass().getPackage().getImplementationTitle();
		String vendor = pkg.getImplementationVendor();
		String version = getClass().getPackage().getImplementationVersion();
		return new AppVersion(title, vendor, version);
	}
}
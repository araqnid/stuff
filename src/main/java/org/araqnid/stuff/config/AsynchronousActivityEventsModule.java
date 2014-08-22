package org.araqnid.stuff.config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.araqnid.stuff.AppService;
import org.araqnid.stuff.activity.ActivityEventSink;
import org.araqnid.stuff.activity.AsyncActivityEventSink;
import org.araqnid.stuff.activity.AsyncActivityEventsProcessor;
import org.araqnid.stuff.activity.LogActivityEvents;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public final class AsynchronousActivityEventsModule extends AbstractModule {
	@Override
	protected void configure() {
		Multibinder<AppService> appServices = Multibinder.newSetBinder(binder(), AppService.class);
		bind(ActivityEventSink.class).to(MDCPopulatingEventSink.class);
		bind(ActivityEventSink.class).annotatedWith(Names.named("logger")).to(AsyncActivityEventSink.class);
		bind(ActivityEventSink.class).annotatedWith(Names.named("backend")).to(LogActivityEvents.class);
		appServices.addBinding().to(AsyncActivityEventsProcessor.class);
	}

	@Provides
	@Singleton
	public BlockingQueue<AsyncActivityEventSink.Event> activityEventQueue() {
		return new LinkedBlockingQueue<>();
	}

	@Provides
	public AsyncActivityEventsProcessor activityEventProcessor(@Named("backend") ActivityEventSink sink,
			BlockingQueue<AsyncActivityEventSink.Event> queue) {
		return new AsyncActivityEventsProcessor(sink, queue);
	}
}
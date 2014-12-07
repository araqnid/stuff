package org.araqnid.stuff.config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.araqnid.stuff.activity.ActivityEventSink;
import org.araqnid.stuff.activity.AsyncActivityEventSink;
import org.araqnid.stuff.activity.AsyncActivityEventsProcessor;
import org.araqnid.stuff.activity.LogActivityEvents;
import org.araqnid.stuff.activity.MDCPopulatingEventSink;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public final class AsynchronousActivityEventsModule extends AbstractModule {
	@Override
	protected void configure() {
		Multibinder<Service> services = Multibinder.newSetBinder(binder(), Service.class);
		bind(ActivityEventSink.class).to(MDCPopulatingEventSink.class);
		bind(ActivityEventSink.class).annotatedWith(Names.named("logger")).to(AsyncActivityEventSink.class);
		bind(ActivityEventSink.class).annotatedWith(Names.named("backend")).to(LogActivityEvents.class);
		services.addBinding().to(AsyncActivityEventsProcessor.class);
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

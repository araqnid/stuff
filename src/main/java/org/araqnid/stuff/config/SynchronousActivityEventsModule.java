package org.araqnid.stuff.config;

import org.araqnid.stuff.activity.ActivityEventSink;
import org.araqnid.stuff.activity.LogActivityEvents;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public final class SynchronousActivityEventsModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(ActivityEventSink.class).to(MDCPopulatingEventSink.class);
		bind(ActivityEventSink.class).annotatedWith(Names.named("logger")).to(LogActivityEvents.class);
	}
}
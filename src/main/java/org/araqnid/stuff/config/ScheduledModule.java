package org.araqnid.stuff.config;

import org.araqnid.stuff.CacheRefresher;

public final class ScheduledModule extends ScheduledJobsModule {
	@Override
	protected void configureJobs() {
		run(CacheRefresher.class).withInterval(60 * 1000L);
	}
}
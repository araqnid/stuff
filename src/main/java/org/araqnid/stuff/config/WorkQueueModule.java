package org.araqnid.stuff.config;

import org.araqnid.stuff.AppService;
import org.araqnid.stuff.SomeQueueProcessor;
import org.araqnid.stuff.activity.RequestActivity;
import org.araqnid.stuff.workqueue.SqlWorkQueue;
import org.araqnid.stuff.workqueue.WorkQueue;

import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;

public final class WorkQueueModule extends BeanstalkWorkQueueModule {
	@Override
	protected void configureDelivery() {
		into(Multibinder.newSetBinder(binder(), AppService.class));
		process("somequeue").with(SomeQueueProcessor.class);
		process("otherqueue").with(SomeQueueProcessor.class);
	}

	@Provides
	@Named("somequeue")
	public WorkQueue somequeue(RequestActivity requestActivity) {
		return new SqlWorkQueue("somequeue", requestActivity);
	}

	@Provides
	@Named("otherqueue")
	public WorkQueue otherqueue(RequestActivity requestActivity) {
		return new SqlWorkQueue("otherqueue", requestActivity);
	}
}
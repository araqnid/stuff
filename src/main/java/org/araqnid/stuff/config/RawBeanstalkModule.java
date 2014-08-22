package org.araqnid.stuff.config;

import org.araqnid.stuff.AppService;
import org.araqnid.stuff.SometubeHandler;

import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.surftools.BeanstalkClient.Client;
import com.surftools.BeanstalkClientImpl.ClientImpl;

public final class RawBeanstalkModule extends BeanstalkModule {
	@Override
	protected void configureDelivery() {
		into(Multibinder.newSetBinder(binder(), AppService.class));
		process("sometube").with(SometubeHandler.class);
	}

	@Provides
	public Client beanstalkClient() {
		ClientImpl client = new ClientImpl();
		client.setUniqueConnectionPerThread(false);
		return client;
	}
}
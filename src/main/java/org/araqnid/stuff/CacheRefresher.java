package org.araqnid.stuff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class CacheRefresher implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(CacheRefresher.class);
	private final RequestActivity<AppRequestType, AppEventType> requestActivity;

	@Inject
	public CacheRefresher(RequestActivity<AppRequestType, AppEventType> requestActivity) {
		this.requestActivity = requestActivity;
	}

	@Override
	public void run() {
		LOG.info("{} Cache refresh", requestActivity.getRuid());
	}
}
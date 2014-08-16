package org.araqnid.stuff;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheRefresher implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(CacheRefresher.class);

	@Override
	public void run() {
		LOG.info("Cache refresh");
	}
}

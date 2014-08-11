package org.araqnid.stuff;

import org.araqnid.stuff.workqueue.PermanentWorkProcessorException;
import org.araqnid.stuff.workqueue.WorkProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SomeQueueProcessor implements WorkProcessor {
	private static final Logger LOG = LoggerFactory.getLogger(SomeQueueProcessor.class);

	@Override
	public void process(String id, byte[] payload) throws PermanentWorkProcessorException {
		LOG.info("Process work item {}", id);
	}
}

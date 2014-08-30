package org.araqnid.stuff.workqueue;

import org.araqnid.stuff.RedisProcessor.DeliveryTarget;

public class WorkQueueRedisHandler implements DeliveryTarget {
	private final WorkDispatcher dispatcher;

	public WorkQueueRedisHandler(WorkDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Override
	public boolean deliver(String data) {
		return dispatcher.process(data, null);
	}
}

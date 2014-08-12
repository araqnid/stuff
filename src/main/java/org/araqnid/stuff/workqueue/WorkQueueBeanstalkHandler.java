package org.araqnid.stuff.workqueue;

import java.nio.charset.Charset;

import org.araqnid.stuff.BeanstalkProcessor.DeliveryTarget;
import org.araqnid.stuff.RequestActivity;

public class WorkQueueBeanstalkHandler implements DeliveryTarget {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private final WorkDispatcher dispatcher;
	private final RequestActivity requestActivity;
	private final String queueId;

	public WorkQueueBeanstalkHandler(String queueId, WorkDispatcher dispatcher, RequestActivity requestActivity) {
		this.queueId = queueId;
		this.dispatcher = dispatcher;
		this.requestActivity = requestActivity;
	}

	@Override
	public boolean deliver(byte[] data) {
		int pos = 0;
		while (pos < data.length) {
			if (data[pos] == 0) {
				break;
			}
			pos++;
		}
		String id;
		byte[] payload;
		if (pos == data.length) {
			id = new String(data, UTF8);
			payload = null;
		}
		else {
			id = new String(data, 0, pos, UTF8);
			payload = new byte[data.length - pos];
			System.arraycopy(data, pos, data, 0, data.length - pos);
		}
		requestActivity.beginEvent("WQP", queueId + " " + id);
		try {
			return dispatcher.process(id, payload);
		} finally {
			requestActivity.finishEvent("WQP");
		}
	}
}

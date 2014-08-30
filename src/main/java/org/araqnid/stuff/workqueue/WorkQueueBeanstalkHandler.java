package org.araqnid.stuff.workqueue;

import java.nio.charset.Charset;

import org.araqnid.stuff.BeanstalkProcessor.DeliveryTarget;

public class WorkQueueBeanstalkHandler implements DeliveryTarget {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private final WorkDispatcher dispatcher;

	public WorkQueueBeanstalkHandler(WorkDispatcher dispatcher) {
		this.dispatcher = dispatcher;
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
			payload = new byte[data.length - pos - 1];
			System.arraycopy(data, pos + 1, payload, 0, data.length - pos - 1);
		}
		return dispatcher.process(id, payload);
	}
}

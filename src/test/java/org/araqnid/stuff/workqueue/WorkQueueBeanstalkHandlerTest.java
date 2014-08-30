package org.araqnid.stuff.workqueue;

import java.nio.charset.Charset;

import org.junit.Test;
import org.mockito.Mockito;

import static org.araqnid.stuff.testutil.RandomData.randomString;
import static org.mockito.Mockito.mock;

public class WorkQueueBeanstalkHandlerTest {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private WorkDispatcher dispatcher = mock(WorkDispatcher.class);

	@Test
	public void delivers_message_with_just_identifier() {
		WorkQueueBeanstalkHandler handler = new WorkQueueBeanstalkHandler(dispatcher);
		String messageId = randomString();
		handler.deliver(messageId.getBytes(UTF8));
		Mockito.verify(dispatcher).process(messageId, null);
	}

	@Test
	public void delivers_message_with_identifier_and_empty_payload() {
		WorkQueueBeanstalkHandler handler = new WorkQueueBeanstalkHandler(dispatcher);
		String messageId = randomString();
		handler.deliver((messageId + "\0").getBytes(UTF8));
		Mockito.verify(dispatcher).process(messageId, new byte[0]);
	}

	@Test
	public void delivers_message_with_identifier_and_payload() {
		WorkQueueBeanstalkHandler handler = new WorkQueueBeanstalkHandler(dispatcher);
		String messageId = randomString();
		String payload = randomString();
		handler.deliver((messageId + "\0" + payload).getBytes(UTF8));
		Mockito.verify(dispatcher).process(messageId, payload.getBytes(UTF8));
	}
}

package org.araqnid.stuff.workqueue;

import java.nio.charset.Charset;
import java.util.Random;

import org.araqnid.stuff.activity.AppEventType;
import org.araqnid.stuff.activity.RequestActivity;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class WorkQueueBeanstalkHandlerTest {
	private static final Charset UTF8 = Charset.forName("UTF-8");

	@Test
	public void delivers_message_with_just_identifier() {
		WorkDispatcher dispatcher = Mockito.mock(WorkDispatcher.class);
		String queueId = randomString();
		RequestActivity requestActivity = Mockito.mock(RequestActivity.class);
		WorkQueueBeanstalkHandler handler = new WorkQueueBeanstalkHandler(queueId, dispatcher, requestActivity);
		String messageId = randomString();
		handler.deliver(messageId.getBytes(UTF8));
		Mockito.verify(dispatcher).process(messageId, null);
	}

	@Test
	public void delivers_message_with_identifier_and_empty_payload() {
		WorkDispatcher dispatcher = Mockito.mock(WorkDispatcher.class);
		String queueId = randomString();
		RequestActivity requestActivity = Mockito.mock(RequestActivity.class);
		WorkQueueBeanstalkHandler handler = new WorkQueueBeanstalkHandler(queueId, dispatcher, requestActivity);
		String messageId = randomString();
		handler.deliver((messageId + "\0").getBytes(UTF8));
		Mockito.verify(dispatcher).process(messageId, new byte[0]);
	}

	@Test
	public void delivers_message_with_identifier_and_payload() {
		WorkDispatcher dispatcher = Mockito.mock(WorkDispatcher.class);
		String queueId = randomString();
		RequestActivity requestActivity = Mockito.mock(RequestActivity.class);
		WorkQueueBeanstalkHandler handler = new WorkQueueBeanstalkHandler(queueId, dispatcher, requestActivity);
		String messageId = randomString();
		String payload = randomString();
		handler.deliver((messageId + "\0" + payload).getBytes(UTF8));
		Mockito.verify(dispatcher).process(messageId, payload.getBytes(UTF8));
	}

	@Test
	public void adds_activity_event_around_delivery() {
		WorkDispatcher dispatcher = Mockito.mock(WorkDispatcher.class);
		String queueId = randomString();
		String messageId = randomString();
		RequestActivity requestActivity = Mockito.mock(RequestActivity.class);
		WorkQueueBeanstalkHandler handler = new WorkQueueBeanstalkHandler(queueId, dispatcher, requestActivity);
		handler.deliver(messageId.getBytes(UTF8));
		Mockito.verify(requestActivity).beginEvent(AppEventType.WorkQueueItem, queueId + "\t" + messageId);
		Mockito.verify(requestActivity).finishEvent(AppEventType.WorkQueueItem);
	}

	@Test
	public void event_is_finished_even_if_dispatcher_dies() {
		WorkDispatcher dispatcher = Mockito.mock(WorkDispatcher.class);
		RequestActivity requestActivity = Mockito.mock(RequestActivity.class);
		UnsupportedOperationException exception = new UnsupportedOperationException();
		Mockito.when(dispatcher.process(Mockito.anyString(), Mockito.any(byte[].class))).thenThrow(exception);
		WorkQueueBeanstalkHandler handler = new WorkQueueBeanstalkHandler(randomString(), dispatcher, requestActivity);
		try {
			handler.deliver(randomString().getBytes(UTF8));
			Assert.fail();
		} catch (UnsupportedOperationException e) {
			Assert.assertSame(exception, e);
		}
		Mockito.verify(requestActivity).finishEvent(AppEventType.WorkQueueItem);
	}

	private static String randomString() {
		Random random = new Random();
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		int len = 10;
		StringBuilder builder = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
		}
		return builder.toString();
	}
}

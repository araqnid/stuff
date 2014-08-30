package org.araqnid.stuff.workqueue;

import java.util.Random;

import org.araqnid.stuff.activity.AppEventType;
import org.araqnid.stuff.activity.RequestActivity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.araqnid.stuff.testutil.RandomData.randomString;
import static org.mockito.Mockito.mock;

public class WorkDispatcherTest {
	private WorkQueue queue = mock(WorkQueue.class);
	private WorkProcessor processor = mock(WorkProcessor.class);
	private RequestActivity requestActivity = mock(RequestActivity.class);
	private String queueId = randomString();

	@Before
	public void setUp() {
		Mockito.when(queue.toString()).thenReturn(queueId);
	}

	@Test
	public void message_is_passed_to_processor_and_queue_marked() throws Exception {
		WorkDispatcher dispatcher = new WorkDispatcher(queue, processor, requestActivity);
		String messageId = randomString();
		byte[] payload = randomPayload();
		dispatcher.process(messageId, payload);
		Mockito.verify(processor).process(messageId, payload);
		Mockito.verify(queue).markInProgress(messageId);
		Mockito.verify(queue).markProcessed(messageId);
		Mockito.verifyNoMoreInteractions(queue);
	}

	@Test
	public void item_is_marked_as_temporarily_failed_if_processor_throws_runtime_exception() throws Exception {
		WorkDispatcher dispatcher = new WorkDispatcher(queue, processor, requestActivity);
		String messageId = randomString();
		UnsupportedOperationException exception = new UnsupportedOperationException();
		Mockito.doThrow(exception).when(processor).process(Mockito.anyString(), Mockito.any(byte[].class));
		dispatcher.process(messageId, randomPayload());
		Mockito.verify(queue).markInProgress(messageId);
		Mockito.verify(queue).markFailed(messageId, false, null, exception);
		Mockito.verifyNoMoreInteractions(queue);
	}

	@Test
	public void item_is_marked_as_permanently_failed_if_processor_throws_specific_exception() throws Exception {
		WorkDispatcher dispatcher = new WorkDispatcher(queue, processor, requestActivity);
		String messageId = randomString();
		String failureMessage = randomString();
		PermanentWorkProcessorException exception = new PermanentWorkProcessorException(failureMessage);
		Mockito.doThrow(exception).when(processor).process(Mockito.anyString(), Mockito.any(byte[].class));
		dispatcher.process(messageId, randomPayload());
		Mockito.verify(queue).markInProgress(messageId);
		Mockito.verify(queue).markFailed(messageId, true, failureMessage, exception);
		Mockito.verifyNoMoreInteractions(queue);
	}

	@Test
	public void adds_activity_event_around_delivery() throws Exception {
		WorkDispatcher dispatcher = new WorkDispatcher(queue, processor, requestActivity);
		String messageId = randomString();
		byte[] payload = randomPayload();
		dispatcher.process(messageId, payload);
		Mockito.verify(requestActivity).beginEvent(AppEventType.WorkQueueItem, queueId + "\t" + messageId);
		Mockito.verify(requestActivity).finishEvent(AppEventType.WorkQueueItem);
	}

	@Test
	public void event_is_finished_even_if_processor_dies() throws Exception {
		WorkDispatcher dispatcher = new WorkDispatcher(queue, processor, requestActivity);
		String messageId = randomString();
		UnsupportedOperationException exception = new UnsupportedOperationException();
		Mockito.doThrow(exception).when(processor).process(Mockito.anyString(), Mockito.any(byte[].class));
		dispatcher.process(messageId, randomPayload());
		Mockito.verify(requestActivity).finishEvent(AppEventType.WorkQueueItem);
	}

	private static byte[] randomPayload() {
		Random random = new Random();
		int len = random.nextInt(80);
		byte[] data = new byte[len];
		random.nextBytes(data);
		return data;
	}
}

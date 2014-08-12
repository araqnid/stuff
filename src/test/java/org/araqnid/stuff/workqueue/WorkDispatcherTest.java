package org.araqnid.stuff.workqueue;

import java.util.Random;

import org.junit.Test;
import org.mockito.Mockito;

public class WorkDispatcherTest {
	@Test
	public void message_is_passed_to_processor_and_queue_marked() throws Exception {
		WorkQueue queue = Mockito.mock(WorkQueue.class);
		WorkProcessor processor = Mockito.mock(WorkProcessor.class);
		WorkDispatcher dispatcher = new WorkDispatcher(queue, processor);
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
		WorkQueue queue = Mockito.mock(WorkQueue.class);
		WorkProcessor processor = Mockito.mock(WorkProcessor.class);
		WorkDispatcher dispatcher = new WorkDispatcher(queue, processor);
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
		WorkQueue queue = Mockito.mock(WorkQueue.class);
		WorkProcessor processor = Mockito.mock(WorkProcessor.class);
		WorkDispatcher dispatcher = new WorkDispatcher(queue, processor);
		String messageId = randomString();
		String failureMessage = randomString();
		PermanentWorkProcessorException exception = new PermanentWorkProcessorException(failureMessage);
		Mockito.doThrow(exception).when(processor).process(Mockito.anyString(), Mockito.any(byte[].class));
		dispatcher.process(messageId, randomPayload());
		Mockito.verify(queue).markInProgress(messageId);
		Mockito.verify(queue).markFailed(messageId, true, failureMessage, exception);
		Mockito.verifyNoMoreInteractions(queue);
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

	private static byte[] randomPayload() {
		Random random = new Random();
		int len = random.nextInt(80);
		byte[] data = new byte[len];
		random.nextBytes(data);
		return data;
	}
}

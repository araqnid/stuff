package org.araqnid.stuff.workqueue;

public interface WorkProcessor {
	void process(String id, byte[] payload) throws PermanentWorkProcessorException;
}

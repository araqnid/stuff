package org.araqnid.stuff.workqueue;

import org.hibernate.SessionFactory;

public class HibernateWorkQueue implements WorkQueue {
	private final String queueCode;
	private final SessionFactory sessionFactory;

	public HibernateWorkQueue(String queueCode, SessionFactory sessionFactory) {
		this.queueCode = queueCode;
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void markInProgress(String entryId) {
	}

	@Override
	public void markProcessed(String entryId) {
	}

	@Override
	public void markFailed(String entryId, boolean permanent, String message, Throwable t) {
	}
}

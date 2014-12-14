package org.araqnid.stuff.workqueue;

import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.joda.time.DateTime;

public class HibernateWorkQueue implements WorkQueue {
	private final String queueCode;
	private final SessionFactory sessionFactory;

	public HibernateWorkQueue(String queueCode, SessionFactory sessionFactory) {
		this.queueCode = queueCode;
		this.sessionFactory = sessionFactory;
	}

	public void create(String entryId, byte[] payload) {
		Session session = sessionFactory.openSession();
		try {
			Transaction tx = session.beginTransaction();
			boolean committed = false;
			try {
				WorkItem item = new WorkItem();
				item.setQueueCode(queueCode);
				item.setReference(entryId);
				item.setPayload(payload);
				item.setStatus(WorkItem.Status.READY);
				session.save(item);
				WorkItemEvent event = new WorkItemEvent();
				event.setItem(item);
				event.setType(WorkItemEvent.Type.CREATED);
				event.setContext(context());
				event.setCreated(DateTime.now());
				session.save(event);
				tx.commit();
				committed = true;
			} finally {
				if (!committed) tx.rollback();
			}
		} finally {
			session.close();
		}
	}

	@Override
	public void markInProgress(String entryId) {
		Session session = sessionFactory.openSession();
		try {
			Query query = session
					.createQuery("from WorkItem item where item.queueCode = :queue and item.reference = :ref");
			query.setParameter("queue", queueCode);
			query.setParameter("ref", entryId);
			query.setComment("HibernateWorkQueue#markInProgress");
			query.setLockMode("item", LockMode.PESSIMISTIC_WRITE);
			WorkItem item = (WorkItem) query.uniqueResult();
			if (item == null) {
			}
		} finally {
			session.close();
		}
	}

	@Override
	public void markProcessed(String entryId) {
	}

	@Override
	public void markFailed(String entryId, boolean permanent, String message, Throwable t) {
	}

	private String context() {
		return "{}";
	}
}

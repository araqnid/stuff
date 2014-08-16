package org.araqnid.stuff.workqueue;

import org.araqnid.stuff.AppEventType;
import org.araqnid.stuff.RequestActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlWorkQueue implements WorkQueue {
	private static final Logger LOG = LoggerFactory.getLogger(SqlWorkQueue.class);
	private final String queueCode;
	private final RequestActivity requestActivity;

	public SqlWorkQueue(String queueCode, RequestActivity requestActivity) {
		this.queueCode = queueCode;
		this.requestActivity = requestActivity;
	}

	public void create(String entryId, byte[] payload) {
		LOG.info("{} create {} {} bytes", queueCode, entryId, payload.length);
		doSql("SqlWorkQueue#create",
				"insert into item(queue_code, item_status_code, reference, payload) values(?, 'Q', ?, ?)",
				"insert into item_event(queue_code, reference, item_event_type_code, context) values(?, ?, 'Q', ?)");
	}

	@Override
	public void markInProgress(String entryId) {
		LOG.info("{} in-progress {}", queueCode, entryId);
		doSql("SqlWorkQueue#markInProgress",
				"update item set item_status_code = 'I' where item.queue_code = ? and item.reference = ?",
				"insert into item_event(queue_code, reference, item_event_type_code, context) values(?, ?, 'I', ?)");
	}

	@Override
	public void markProcessed(String entryId) {
		LOG.info("{} processed {}", queueCode, entryId);
		doSql("SqlWorkQueue#markProcessed",
				"update item set item_status_code = 'P' where item.queue_code = ? and item.reference = ?",
				"insert into item_event(queue_code, reference, item_event_type_code, context) values(?, ?, 'P', ?)");
	}

	@Override
	public void markFailed(String entryId, boolean permanent, String message, Throwable t) {
		LOG.info("{} failed {} {} {}", queueCode, entryId, permanent ? "permanent" : "temporary",
				message != null ? message : t != null ? t.toString() : "<no message>");
		doSql("SqlWorkQueue#markFailed",
				"update item set item_status_code = 'F' where item.queue_code = ? and item.reference = ?",
				"insert into item_event(queue_code, reference, item_event_type_code, context) values(?, ?, 'F', ?)");
	}

	private void doSql(String caller, String... statements) {
		requestActivity.beginEvent(AppEventType.DatabaseTransaction, caller);
		try {
			for (String sql : statements) {
				requestActivity.beginEvent(AppEventType.DatabaseStatement, sql);
				try {
				} finally {
					requestActivity.finishEvent(AppEventType.DatabaseStatement);
				}
			}
		} finally {
			requestActivity.finishEvent(AppEventType.DatabaseTransaction);
		}
	}
}

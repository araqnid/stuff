package org.araqnid.stuff.workqueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class SqlWorkQueue implements WorkQueue {
	private static final Logger LOG = LoggerFactory.getLogger(SqlWorkQueue.class);
	private final String queueCode;

	public SqlWorkQueue(String queueCode) {
		this.queueCode = queueCode;
	}

	public void create(String entryId, byte[] payload) {
		LOG.info("create {} {} bytes", entryId, payload.length);
		ImmutableList.of(
				"insert into item(queue_code, item_status_code, reference, payload) values(?, 'Q', ?, ?)",
				"insert into item_event(queue_code, reference, item_event_type_code, context) values(?, ?, 'Q', ?)"
		);
	}

	@Override
	public void markInProgress(String entryId) {
		LOG.info("in-progress {}", entryId);
		ImmutableList.of(
				"update item set item_status_code = 'I' where item.queue_code = ? and item.reference = ?",
				"insert into item_event(queue_code, reference, item_event_type_code, context) values(?, ?, 'I', ?)"
		);
	}

	@Override
	public void markProcessed(String entryId) {
		LOG.info("processed {}", entryId);
		ImmutableList.of(
				"update item set item_status_code = 'P' where item.queue_code = ? and item.reference = ?",
				"insert into item_event(queue_code, reference, item_event_type_code, context) values(?, ?, 'P', ?)"
		);
	}

	@Override
	public void markFailed(String entryId, boolean permanent, String message, Throwable t) {
		LOG.info("failed {} {} {}", entryId, permanent ? "permanent" : "temporary", message != null ? message : t != null ? t.toString()
				: "<no message>");
		ImmutableList.of(
				"update item set item_status_code = 'F' where item.queue_code = ? and item.reference = ?",
				"insert into item_event(queue_code, reference, item_event_type_code, context) values(?, ?, 'F', ?)"
		);
	}
}

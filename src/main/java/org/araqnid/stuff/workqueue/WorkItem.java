package org.araqnid.stuff.workqueue;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(schema = "work")
public class WorkItem {
	@Id
	@GeneratedValue
	private Integer id;
	@Column(nullable = false)
	private String queueCode;
	@Column(nullable = false)
	private String reference;
	@Column(nullable = false)
	private Status status;
	@Column(nullable = false)
	private byte[] payload;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getQueueCode() {
		return queueCode;
	}

	public void setQueueCode(String queueCode) {
		this.queueCode = queueCode;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public byte[] getPayload() {
		return payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	@Override
	public String toString() {
		return "WorkItem#" + id;
	}

	public enum Status {
		READY
	}
}

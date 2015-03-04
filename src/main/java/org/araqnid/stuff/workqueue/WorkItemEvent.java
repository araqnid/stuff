package org.araqnid.stuff.workqueue;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.joda.time.DateTime;

@Entity
@Table(schema = "work")
public class WorkItemEvent {
	@Id
	@GeneratedValue
	private Integer id;
	@ManyToOne(optional = false)
	private WorkItem item;
	@Column(nullable = false)
	private Type type;
	@Column(nullable = false)
	private String context;
	@Column(nullable = false)
	private DateTime created;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public WorkItem getItem() {
		return item;
	}

	public void setItem(WorkItem item) {
		this.item = item;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public DateTime getCreated() {
		return created;
	}

	public void setCreated(DateTime created) {
		this.created = created;
	}

	@Override
	public String toString() {
		return "WorkItemEvent#" + id;
	}

	public enum Type {
		CREATED
	}
}

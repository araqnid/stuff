package org.araqnid.stuff.workqueue;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.joda.time.DateTime;

@Entity
public class WorkItemEvent {
	@Id
	@GeneratedValue
	private Integer id;
	private WorkItem item;
	private WorkItemEventType type;
	private String context;
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

	public WorkItemEventType getType() {
		return type;
	}

	public void setType(WorkItemEventType type) {
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
}

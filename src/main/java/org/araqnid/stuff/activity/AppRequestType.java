package org.araqnid.stuff.activity;

public enum AppRequestType {
	HttpRequest, BeanstalkMessage, ScheduledJob, RedisMessage, Initialisation, EventReplay;
}

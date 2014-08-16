package org.araqnid.stuff.activity;


public interface ActivityScopeControl {
	void beginRequest(AppRequestType type, String description);

	void beginRequest(String ruid, AppRequestType type, String description);

	void finishRequest(AppRequestType type);
}
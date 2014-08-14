package org.araqnid.stuff;

public interface AppLifecycleEvent {
	void starting();
	void started();
	void stopping();
	void stopped();
}

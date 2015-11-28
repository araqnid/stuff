package org.araqnid.stuff;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "app-version")
public class AppVersion {
	public final String version;
	public final String title;
	public final String vendor;

	public AppVersion(String title, String vendor, String version) {
		this.title = title;
		this.vendor = vendor;
		this.version = version;
	}
}

package org.araqnid.stuff;

import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;

@SuppressWarnings("WeakerAccess")
@XmlRootElement(name = "app-version")
public final class AppVersion {
	public final String version;
	public final String title;
	public final String vendor;

	public AppVersion(String title, String vendor, String version) {
		this.title = title;
		this.vendor = vendor;
		this.version = version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AppVersion that = (AppVersion) o;
		return Objects.equals(version, that.version) &&
				Objects.equals(title, that.title) &&
				Objects.equals(vendor, that.vendor);
	}

	@Override
	public int hashCode() {
		return Objects.hash(version, title, vendor);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("version", version)
				.add("title", title)
				.add("vendor", vendor)
				.toString();
	}
}

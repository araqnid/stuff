package org.araqnid.stuff.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.util.resource.Resource;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ResourceInfo;

public class EmbeddedResource extends Resource {
	private final ClassLoader classLoader;
	private final String path;
	private final ClassPath classPath;

	public EmbeddedResource(ClassLoader classLoader, String path, ClassPath classPath) {
		this.classLoader = classLoader;
		this.path = path;
		this.classPath = classPath;
	}

	@Override
	public boolean isContainedIn(Resource r) throws MalformedURLException {
		return false;
	}

	@Override
	public boolean exists() {
		URL url = classLoader.getResource(path);
		return url != null;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public long lastModified() {
		return -1;
	}

	@Override
	public long length() {
		return -1;
	}

	@Override
	public URL getURL() {
		return classLoader.getResource(path);
	}

	@Override
	public File getFile() throws IOException {
		return null;
	}

	@Override
	public String getName() {
		return path;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return classLoader.getResourceAsStream(path);
	}

	@Override
	public ReadableByteChannel getReadableByteChannel() throws IOException {
		return null;
	}

	@Override
	public void close() {
	}

	@Override
	public boolean delete() throws SecurityException {
		return false;
	}

	@Override
	public boolean renameTo(Resource dest) throws SecurityException {
		return false;
	}

	@Override
	public String[] list() {
		String prefix = path;
		if (!prefix.endsWith("/")) {
			prefix = prefix + "/";
		}
		Set<String> matching = new HashSet<>();
		ImmutableSet<ResourceInfo> resources = classPath.getResources();
		for (ResourceInfo resource : resources) {
			String name = resource.getResourceName();
			if (name.startsWith(prefix)) {
				String tail = name.substring(prefix.length());
				Iterable<String> parts = Splitter.on('/').split(tail);
				matching.add(parts.iterator().next());
			}
		}
		return matching.toArray(new String[matching.size()]);
	}

	@Override
	public Resource addPath(String path) throws IOException, MalformedURLException {
		return new EmbeddedResource(classLoader, this.path + path, classPath);
	}

	@Override
	public String toString() {
		return "Classpath:/" + path;
	}
}

package org.araqnid.stuff.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;

import org.eclipse.jetty.util.resource.Resource;

public class EmbeddedResource extends Resource {
	private final ClassLoader classLoader;
	private final String path;

	public EmbeddedResource(ClassLoader classLoader, String path) {
		this.classLoader = classLoader;
		this.path = path;
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
		return null;
	}

	@Override
	public Resource addPath(String path) throws IOException, MalformedURLException {
		return new EmbeddedResource(classLoader, this.path + path);
	}
}

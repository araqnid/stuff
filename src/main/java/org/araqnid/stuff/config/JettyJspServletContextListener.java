package org.araqnid.stuff.config;

import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;

import com.google.common.base.Throwables;

public class JettyJspServletContextListener implements ServletContextListener {
	private final File jspTempDir;

	public JettyJspServletContextListener(File jspTempDir) {
		this.jspTempDir = jspTempDir;
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			new JettyJasperInitializer().onStartup(null, sce.getServletContext());
		} catch (ServletException e) {
			throw Throwables.propagate(e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		cleanDirectory(jspTempDir);
	}

	private static void cleanDirectory(File dir) {
		for (File entry : dir.listFiles()) {
			if (entry.isDirectory()) {
				cleanDirectory(entry);
			}
			else {
				entry.delete();
			}
		}
		dir.delete();
	}
}
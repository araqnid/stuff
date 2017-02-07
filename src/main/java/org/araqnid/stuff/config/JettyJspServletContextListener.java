package org.araqnid.stuff.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.apache.jasper.servlet.JasperInitializer;
import org.apache.jasper.servlet.TldScanner;
import org.apache.tomcat.util.descriptor.tld.TaglibXml;
import org.apache.tomcat.util.descriptor.tld.TldResourcePath;
import org.xml.sax.SAXException;

public class JettyJspServletContextListener implements ServletContextListener {
	private final File jspTempDir;
	private final Map<String, TaglibXml> embeddedTaglibs;

	public JettyJspServletContextListener(File jspTempDir, Map<String, TaglibXml> embeddedTaglibs) {
		this.jspTempDir = jspTempDir;
		this.embeddedTaglibs = embeddedTaglibs;
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			JasperInitializer initialiser = new JasperInitializer() {
				@Override
				public TldScanner newTldScanner(ServletContext context,
						boolean namespaceAware,
						boolean validate,
						boolean blockExternal) {
					return new TldScanner(context, namespaceAware, validate, blockExternal) {
						@Override
						public void scan() throws IOException, SAXException {
							super.scan();
							for (Map.Entry<String, TaglibXml> e : embeddedTaglibs.entrySet()) {
								TldResourcePath resourcePath = new TldResourcePath(new URL(e.getKey()), null);
								getUriTldResourcePathMap().put(e.getKey(), resourcePath);
								getTldResourcePathTaglibXmlMap().put(resourcePath, e.getValue());
							}
						}
					};
				}
			};
			initialiser.onStartup(null, sce.getServletContext());
		} catch (ServletException e) {
			throw new RuntimeException(e);
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

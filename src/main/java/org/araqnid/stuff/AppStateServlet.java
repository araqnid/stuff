package org.araqnid.stuff;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AppStateServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final AppStateMonitor servicesMonitor;

	@Inject
	public AppStateServlet(AppStateMonitor servicesMonitor) {
		this.servicesMonitor = servicesMonitor;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/plain");
		resp.setHeader("Cache-Control", "no-cache, no-store");
		PrintWriter pw = resp.getWriter();
		pw.print(servicesMonitor.getState().toString());
	}
}

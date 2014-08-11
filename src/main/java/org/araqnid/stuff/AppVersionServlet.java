package org.araqnid.stuff;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AppVersionServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final AppVersion versionInfo;

	@Inject
	public AppVersionServlet(AppVersion versionInfo) {
		this.versionInfo = versionInfo;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Cache-Control", "no-cache, no-store");
		new GsonBuilder().serializeNulls().create().toJson(versionInfo, resp.getWriter());
	}
}

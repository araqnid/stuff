package org.araqnid.stuff;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AppVersionServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final AppVersion versionInfo;
	private final Gson gson;

	@Inject
	public AppVersionServlet(AppVersion versionInfo) {
		this.versionInfo = versionInfo;
		this.gson = new GsonBuilder().serializeNulls().create();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		resp.setHeader("Cache-Control", "no-cache, no-store");
		gson.toJson(versionInfo, resp.getWriter());
	}
}

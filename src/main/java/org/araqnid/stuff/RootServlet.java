package org.araqnid.stuff;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.araqnid.stuff.activity.RequestActivity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class RootServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Provider<RequestActivity> stateProvider;

	@Inject
	public RootServlet(Provider<RequestActivity> stateProvider) {
		this.stateProvider = stateProvider;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/plain");
		resp.getWriter().println("This is a test");
		resp.getWriter().println("<!-- " + stateProvider.get().getRuid() + "-->");
	}
}

package org.araqnid.stuff;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class RequestActivityFilter implements Filter {
	private final Provider<RequestActivity> stateProvider;

	@Inject
	public RequestActivityFilter(Provider<RequestActivity> stateProvider) {
		this.stateProvider = stateProvider;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		if (request instanceof HttpServletRequest) {
			doHttpFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
		}
		else {
			chain.doFilter(request, response);
		}
	}

	private void doHttpFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		RequestActivity requestActivity = stateProvider.get();
		String ruid = request.getHeader("ruid");
		if (ruid != null) {
			requestActivity.setRuid(ruid);
		}
		requestActivity.beginRequest("REQ", request.getServletPath());
		try {
			chain.doFilter(request, response);
		} finally {
			requestActivity.finishRequest("REQ");
		}
	}

	@Override
	public void destroy() {
	}
}

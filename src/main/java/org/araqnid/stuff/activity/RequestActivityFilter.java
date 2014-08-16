package org.araqnid.stuff.activity;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class RequestActivityFilter implements Filter {
	private final Provider<RequestActivity> stateProvider;
	private final ActivityScopeControl scopeControl;

	@Inject
	public RequestActivityFilter(Provider<RequestActivity> stateProvider, ActivityScopeControl scopeControl) {
		this.stateProvider = stateProvider;
		this.scopeControl = scopeControl;
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
		String ruid = request.getHeader("X-RUID");
		scopeControl.beginRequest(
				ruid,
				AppRequestType.HttpRequest,
				Joiner.on('\t').join(
						request.getMethod(),
						Joiner.on("").join(
								Iterables.filter(
										Arrays.asList(request.getContextPath(), request.getServletPath(),
												request.getPathInfo()), Predicates.notNull()))));
		try {
			RequestActivity requestActivity = stateProvider.get();
			response.setHeader("X-RUID", requestActivity.getRuid());
			chain.doFilter(request, response);
		} finally {
			scopeControl.finishRequest(AppRequestType.HttpRequest);
		}
	}

	@Override
	public void destroy() {
	}
}

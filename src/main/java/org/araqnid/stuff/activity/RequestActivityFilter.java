package org.araqnid.stuff.activity;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class RequestActivityFilter implements Filter {
	private static final Logger LOG = LoggerFactory.getLogger(RequestActivityFilter.class);
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
		String eventPath = Joiner.on("").join(
				Iterables.filter(
						Arrays.asList(request.getContextPath(), request.getServletPath(),
								request.getPathInfo()), Predicates.notNull()));
		scopeControl.beginRequest(
				ruid,
				AppRequestType.HttpRequest,
				Joiner.on('\t').join(
						request.getMethod(),
						eventPath));
		try {
			RequestActivity requestActivity = stateProvider.get();
			ActivityEventNode rootEvent = requestActivity.getRootEvent();
			response.setHeader("X-RUID", requestActivity.getRuid());
			try {
				chain.doFilter(request, response);
			} finally {
				logRequest(request, response, eventPath, rootEvent);
			}
		} finally {
			scopeControl.finishRequest(AppRequestType.HttpRequest);
		}
	}

	private void logRequest(HttpServletRequest request, HttpServletResponse response, String eventPath, ActivityEventNode event) {
		long elapsedMillis = event.stopwatch.elapsed(TimeUnit.MILLISECONDS);
		NumberFormat nfmt = NumberFormat.getInstance();
		nfmt.setMaximumFractionDigits(1);
		nfmt.setMinimumFractionDigits(1);
		LOG.info("{} {} {} {}s", request.getMethod(), eventPath, response.getStatus(), nfmt.format(elapsedMillis / 1000.0));
	}

	@Override
	public void destroy() {
	}
}

package org.araqnid.stuff.activity;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
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

@Singleton
public class RequestActivityFilter implements Filter {
	private static final Logger LOG = LoggerFactory.getLogger(RequestActivityFilter.class);
	private final Provider<RequestActivity> stateProvider;
	private final RequestLogger requestLogger;
	private final ActivityScopeControl scopeControl;

	@Inject
	public RequestActivityFilter(Provider<RequestActivity> stateProvider, RequestLogger requestLogger, ActivityScopeControl scopeControl) {
		this.stateProvider = stateProvider;
		this.requestLogger = requestLogger;
		this.scopeControl = scopeControl;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest) {
			doHttpFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
		}
		else {
			chain.doFilter(request, response);
		}
	}

	private void doHttpFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		String ruid = request.getHeader("X-RUID");
		String eventPath = Joiner.on("").join(
				Iterables.filter(Arrays.asList(request.getContextPath(), request.getServletPath(), request.getPathInfo()), Predicates.notNull()));
		scopeControl.beginRequest(ruid, AppRequestType.HttpRequest, Joiner.on('\t').join(request.getMethod(), eventPath));
		try {
			RequestActivity requestActivity = stateProvider.get();
			ActivityEventNode rootEvent = requestActivity.getRootEvent();
			response.setHeader("X-RUID", requestActivity.getRuid());
			try {
				chain.doFilter(request, response);
				if (!request.isAsyncStarted()) response.flushBuffer();
			} finally {
				requestLogger.logRequest(request, response, eventPath, rootEvent);
			}
		} finally {
			scopeControl.finishRequest(AppRequestType.HttpRequest);
		}
	}

	@Override
	public void destroy() {
	}

	public interface RequestLogger {
		void logRequest(HttpServletRequest request, HttpServletResponse response, String eventPath, ActivityEventNode event);
	}

	public static class BasicRequestLogger implements RequestLogger {
		@Override
		public void logRequest(HttpServletRequest request, HttpServletResponse response, String eventPath, ActivityEventNode event) {
			long elapsedMillis = event.stopwatch.elapsed(TimeUnit.MILLISECONDS);
			NumberFormat nfmt = NumberFormat.getInstance();
			nfmt.setMaximumFractionDigits(1);
			nfmt.setMinimumFractionDigits(1);
			LOG.info("{} {} {} {}s", request.getMethod(), eventPath, response.getStatus(), nfmt.format(elapsedMillis / 1000.0));
		}
	}

	public static class NoStatusRequestLogger implements RequestLogger {
		@Override
		public void logRequest(HttpServletRequest request, HttpServletResponse response, String eventPath, ActivityEventNode event) {
			long elapsedMillis = event.stopwatch.elapsed(TimeUnit.MILLISECONDS);
			NumberFormat nfmt = NumberFormat.getInstance();
			nfmt.setMaximumFractionDigits(1);
			nfmt.setMinimumFractionDigits(1);
			LOG.info("{} {} {} {}s", request.getMethod(), eventPath, "???", nfmt.format(elapsedMillis / 1000.0));
		}
	}
}

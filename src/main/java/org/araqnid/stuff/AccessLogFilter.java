package org.araqnid.stuff;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
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

import com.google.common.base.Stopwatch;

@Singleton
public class AccessLogFilter implements Filter {
	private static final Logger LOG = LoggerFactory.getLogger(AccessLogFilter.class);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			doFilterHttp((HttpServletRequest) request, (HttpServletResponse) response, chain);
		}
	}

	private void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		Pending pending = new Pending(request, response);
		try {
			chain.doFilter(request, response);
		} finally {
			if (request.isAsyncStarted()) {
				request.getAsyncContext().addListener(new AsyncListener() {
					@Override
					public void onComplete(AsyncEvent event) throws IOException {
						pending.completed(response.getStatus());
					}

					@Override
					public void onTimeout(AsyncEvent event) throws IOException {
					}

					@Override
					public void onStartAsync(AsyncEvent event) throws IOException {
					}

					@Override
					public void onError(AsyncEvent event) throws IOException {
					}
				});
			}
			else {
				pending.completed(response.getStatus());
			}
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}

	private static final class Pending {
		private final String method;
		private final String protocol;
		private final String path;
		private final Stopwatch stopwatch = Stopwatch.createStarted();

		Pending(HttpServletRequest request, HttpServletResponse response) {
			this.method = request.getMethod();
			this.protocol = request.getProtocol();
			this.path = request.getServletPath();
		}

		public void completed(int status) {
			LOG.info("{} {} {} {} {}", method, path, protocol, status,
					Duration.ofNanos(stopwatch.elapsed(TimeUnit.NANOSECONDS)));
		}
	}
}

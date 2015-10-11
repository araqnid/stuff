package org.araqnid.stuff.activity;

import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;
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

import com.google.common.collect.ImmutableMap;

@Singleton
public class ActivityFilter implements Filter {
	private final ActivityEventSink activityEventSink;

	@Inject
	public ActivityFilter(ActivityEventSink activityEventSink) {
		this.activityEventSink = activityEventSink;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			doFilterHttp((HttpServletRequest) request, (HttpServletResponse) response, chain);
		}
	}

	private void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		UUID activityId = UUID.randomUUID();
		Activity activity = new Activity(activityId, "HttpRequest", ImmutableMap.of("method", request.getMethod(),
				"path", request.getServletPath()), activityEventSink);
		response.setHeader("X-Activity", activityId.toString() + " " + activity.root.id);
		boolean success = false;
		ThreadActivity.attach(activity.root);
		activity.begin();
		try {
			chain.doFilter(request, response);
			success = true;
		} finally {
			if (request.isAsyncStarted()) {
				request.getAsyncContext().addListener(new AsyncListener() {
					private String result = "complete";
					private boolean success = true;

					@Override
					public void onTimeout(AsyncEvent event) throws IOException {
						result = "timeout";
						success = false;
					}

					@Override
					public void onStartAsync(AsyncEvent event) throws IOException {
					}

					@Override
					public void onError(AsyncEvent event) throws IOException {
						result = "error";
						success = false;
					}

					@Override
					public void onComplete(AsyncEvent event) throws IOException {
						activity.complete(success,
								ImmutableMap.of("status", response.getStatus(), "async_completion", result));
					}
				});
				ThreadActivity.detach(activity);
			}
			else {
				activity.complete(success, ImmutableMap.of("status", response.getStatus()));
				ThreadActivity.detach(activity.root);
			}
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}
}

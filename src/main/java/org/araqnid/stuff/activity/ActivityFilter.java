package org.araqnid.stuff.activity;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

@Singleton
public class ActivityFilter implements Filter {
	private static final Logger LOG = LoggerFactory.getLogger(ActivityFilter.class);
	private final ObjectMapper mapper;
	private final ActivityEventSink activityEventSink = new ActivityEventSink() {
		@Override
		public void activityNodeStart(UUID activityId,
				long nodeId,
				long nodeParentId,
				String type,
				Instant started,
				Object attributes) {
			LOG.info("start {} {} {} {} {} {}", activityId, nodeId, nodeParentId, type, started, toJson(attributes));
		}

		@Override
		public void activityNodeEnd(UUID activityId, long nodeId, boolean success, Duration duration, Object attributes) {
			LOG.info("end   {} {} {} {} {}", activityId, nodeId, success ? "OK" : "BAD", duration, toJson(attributes));
		}

		private String toJson(Object attributes) {
			try {
				return mapper.writeValueAsString(attributes);
			} catch (JsonProcessingException e) {
				return "/* error */ null";
			}
		}
	};

	@Inject
	public ActivityFilter(ObjectMapper mapper) {
		this.mapper = mapper;
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
				"host", Optional.ofNullable(request.getHeader("Host")), "path", request.getServletPath(), "protocol",
				request.getProtocol(), "userAgent", Optional.ofNullable(request.getHeader("User-Agent"))),
				activityEventSink);
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

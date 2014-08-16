package org.araqnid.stuff;

import static org.araqnid.stuff.AppRequestType.HttpRequest;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.araqnid.stuff.config.ActivityScope;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class RequestActivityFilter implements Filter {
	private final Provider<RequestActivity<AppRequestType, AppEventType>> stateProvider;
	private final ActivityScope.Control<AppRequestType> scopeControl;

	@Inject
	public RequestActivityFilter(Provider<RequestActivity<AppRequestType, AppEventType>> stateProvider, ActivityScope.Control<AppRequestType> scopeControl) {
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
		scopeControl.beginRequest(ruid, HttpRequest, request.getServletPath());
		try {
			RequestActivity<AppRequestType, AppEventType> requestActivity = stateProvider.get();
			response.setHeader("X-RUID", requestActivity.getRuid());
			chain.doFilter(request, response);
		} finally {
			scopeControl.finishRequest(HttpRequest);
		}
	}

	@Override
	public void destroy() {
	}
}

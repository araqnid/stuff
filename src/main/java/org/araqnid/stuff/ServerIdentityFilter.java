package org.araqnid.stuff;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.araqnid.stuff.config.ServerIdentity;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ServerIdentityFilter implements Filter {
	private final String serverIdentity;
	private final UUID instanceId;

	@Inject
	public ServerIdentityFilter(@ServerIdentity String serverIdentity, @ServerIdentity UUID instanceId) {
		this.serverIdentity = serverIdentity;
		this.instanceId = instanceId;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			doHttpFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
		}
		else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
	}

	private void doHttpFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		response.addHeader("X-Server-Identity", Joiner.on(' ').join(serverIdentity, instanceId));
		chain.doFilter(request, response);
	}
}

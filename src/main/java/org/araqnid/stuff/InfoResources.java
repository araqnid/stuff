package org.araqnid.stuff;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.spi.Registry;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;

@Path("info")
@Produces("application/json")
public class InfoResources {
	private final AppVersion appVersion;
	private final AppStateMonitor appStateMonitor;
	private final Registry registry;

	@Inject
	public InfoResources(AppVersion appVersion, AppStateMonitor appStateMonitor, Registry registry) {
		this.appVersion = appVersion;
		this.appStateMonitor = appStateMonitor;
		this.registry = registry;
	}

	@GET
	@Path("version")
	public AppVersion getVersion() {
		return appVersion;
	}

	@GET
	@Path("state")
	public AppState getAppState() {
		return appStateMonitor.getState();
	}

	public static class InvokerDetail {
		public final String method;
		public final String resourceClass;
		public final Set<String> httpMethods;
		public final Set<String> consumes;
		public final Set<String> produces;

		public InvokerDetail(String method, String resourceClass, Set<String> httpMethods, Set<String> consumes,
				Set<String> produces) {
			super();
			this.method = method;
			this.resourceClass = resourceClass;
			this.httpMethods = httpMethods;
			this.consumes = consumes;
			this.produces = produces;
		}

	}

	@GET
	@Path("routing")
	public Map<String, List<InvokerDetail>> getRouting() {
		ResourceMethodRegistry rmr = (ResourceMethodRegistry) registry;
		Map<String, List<InvokerDetail>> output = new TreeMap<>();
		for (Map.Entry<String, List<ResourceInvoker>> e : rmr.getBounded().entrySet()) {
			List<InvokerDetail> values = new ArrayList<>();
			output.put(e.getKey(), values);
			for (ResourceInvoker invoker : e.getValue()) {
				Method method = invoker.getMethod();
				InvokerDetail invokerInfo;
				if (invoker instanceof ResourceMethodInvoker) {
					ResourceMethodInvoker resourceMethodInvoker = (ResourceMethodInvoker) invoker;
					invokerInfo = new InvokerDetail(method.getName(), resourceMethodInvoker.getResourceClass()
							.getName(), resourceMethodInvoker.getHttpMethods(),
							ImmutableSet.copyOf(Iterables.transform(Arrays.asList(resourceMethodInvoker.getConsumes()),
									Functions.toStringFunction())), ImmutableSet.copyOf(Iterables.transform(
									Arrays.asList(resourceMethodInvoker.getProduces()), Functions.toStringFunction())));
				}
				else {
					invokerInfo = new InvokerDetail(method.getName(), method.getDeclaringClass().getName(), null, null,
							null);
				}
				values.add(invokerInfo);
			}
		}
		return output;
	}
}

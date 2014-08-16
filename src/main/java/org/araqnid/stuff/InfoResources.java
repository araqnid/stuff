package org.araqnid.stuff;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.spi.Registry;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

@Path("info")
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
	@Produces("application/json")
	public AppVersion getVersion() {
		return appVersion;
	}

	@GET
	@Path("state")
	@Produces("application/json")
	public AppState getAppState() {
		return appStateMonitor.getState();
	}

	@GET
	@Path("routing")
	@Produces("application/json")
	public Map<String, List<Map<String, String>>> getRouting() {
		ResourceMethodRegistry rmr = (ResourceMethodRegistry) registry;
		Map<String, List<Map<String, String>>> output = new TreeMap<>();
		for (Map.Entry<String, List<ResourceInvoker>> e : rmr.getBounded().entrySet()) {
			List<Map<String, String>> values = new ArrayList<>();
			output.put(e.getKey(), values);
			for (ResourceInvoker invoker : e.getValue()) {
				Method method = invoker.getMethod();
				ImmutableMap<String, String> invokerInfo;
				if (invoker instanceof ResourceMethodInvoker) {
					ResourceMethodInvoker resourceMethodInvoker = (ResourceMethodInvoker) invoker;
					invokerInfo = ImmutableMap.of(
							"method",
							method.getName(),
							"resourceClass",
							resourceMethodInvoker.getResourceClass().getName(),
							"httpMethods",
							resourceMethodInvoker.getHttpMethods().toString(),
							"consumes",
							Arrays.asList(resourceMethodInvoker.getConsumes()).toString(),
							"produces",
							Arrays.asList(resourceMethodInvoker.getProduces()).toString()
							);
				}
				else {
					invokerInfo = ImmutableMap.of("method", method.getName(), "resourceClass", method
							.getDeclaringClass().getName());
				}
				values.add(invokerInfo);
			}
		}
		return output;
	}
}

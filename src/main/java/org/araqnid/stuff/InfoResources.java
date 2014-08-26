package org.araqnid.stuff;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
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
	@Path("version")
	@Produces("text/plain")
	public String getPlainVersion() {
		return appVersion.version;
	}

	@GET
	@Path("state")
	@Produces({ "application/json", "text/plain" })
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
	@Produces("application/json")
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
							.getSimpleName(), resourceMethodInvoker.getHttpMethods(),
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

	@GET
	@Path("routing")
	@Produces("text/plain")
	public String dumpRouting() {
		DumpNode root = new DumpNode();
		for (Map.Entry<String, List<InvokerDetail>> e : getRouting().entrySet()) {
			Iterator<String> segmentIter = Splitter.on('/').omitEmptyStrings().split(e.getKey()).iterator();
			DumpNode cursor = root;
			while (segmentIter.hasNext()) {
				cursor = cursor.matchSegment(segmentIter.next());
			}
			for (InvokerDetail invoker : e.getValue()) {
				for (String httpMethod : invoker.httpMethods) {
					cursor.addHandler(httpMethod, invoker.resourceClass + "." + invoker.method);
				}
			}
		}
		StringWriter sw = new StringWriter();
		root.dump(new PrintWriter(sw));
		return sw.toString();
	}

	private class DumpNode {
		private final DumpNode parent;
		private final Map<String, DumpNode> segmentMatches = new TreeMap<>();
		private final Multimap<String, String> methodHandlers = TreeMultimap.create();

		public DumpNode() {
			this.parent = null;
		}

		private DumpNode(DumpNode parent) {
			this.parent = parent;
		}

		public DumpNode matchSegment(String segment) {
			DumpNode next = segmentMatches.get(segment);
			if (next == null) {
				next = new DumpNode(this);
				segmentMatches.put(segment, next);
			}
			return next;
		}

		public void addHandler(String httpMethod, String target) {
			methodHandlers.put(httpMethod, target);
		}

		public void dump(PrintWriter pw) {
			if (!methodHandlers.isEmpty()) {
				dumpHandlers(pw);
				pw.println("");
			}
			dumpSegments(pw);
		}

		private void dumpSegments(PrintWriter pw) {
			for (Map.Entry<String, DumpNode> e : segmentMatches.entrySet()) {
				indent(pw);
				pw.append(e.getKey());
				e.getValue().dumpHandlers(pw);
				pw.println("");
				e.getValue().dumpSegments(pw);
			}
		}

		private void dumpHandlers(PrintWriter pw) {
			if (methodHandlers.isEmpty()) return;
			pw.append(" = ").append(
					Joiner.on(' ').join(
							Iterables.transform(methodHandlers.entries(),
									new Function<Map.Entry<String, String>, String>() {
										@Override
										public String apply(Map.Entry<String, String> input) {
											return input.getKey() + ":" + input.getValue();
										}
									})));
		}

		private int level() {
			if (parent == null) return 0;
			return parent.level() + 1;
		}

		private void indent(PrintWriter pw) {
			for (int i = 0; i < level(); i++) {
				pw.write('\t');
			}
		}
	}
}

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

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.spi.Registry;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

@Path("_api/info")
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
		StringWriter sw = new StringWriter();
		routingToTree(new TextDumpNode()).dump(new PrintWriter(sw));
		return sw.toString();
	}

	@GET
	@Path("routing")
	@Produces("text/html")
	public String formatRouting() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("<html><head><title>Routing</title></head><body>");
		routingToTree(new HtmlDumpNode()).dump(pw);
		pw.println("</body></html>");
		return sw.toString();
	}

	private <T extends RoutingTree<T>> T routingToTree(T root) {
		for (Map.Entry<String, List<InvokerDetail>> e : getRouting().entrySet()) {
			Iterator<String> segmentIter = Splitter.on('/').omitEmptyStrings().split(e.getKey()).iterator();
			T cursor = root;
			while (segmentIter.hasNext()) {
				cursor = cursor.matchSegment(segmentIter.next());
			}
			for (InvokerDetail invoker : e.getValue()) {
				for (String httpMethod : invoker.httpMethods) {
					cursor.addHandler(httpMethod, invoker.resourceClass + "." + invoker.method);
				}
			}
		}
		return root;
	}

	private static abstract class RoutingTree<T extends RoutingTree<T>> {
		protected final T parent;
		protected final Map<String, T> segmentMatches = new TreeMap<>();
		protected final Multimap<String, String> methodHandlers = TreeMultimap.create();

		public RoutingTree() {
			this.parent = null;
		}

		protected RoutingTree(T parent) {
			this.parent = parent;
		}

		public T matchSegment(String segment) {
			T next = segmentMatches.get(segment);
			if (next == null) {
				next = createChild();
				segmentMatches.put(segment, next);
			}
			return next;
		}

		public void addHandler(String httpMethod, String target) {
			methodHandlers.put(httpMethod, target);
		}

		protected abstract T createChild();
	}

	private static class TextDumpNode extends RoutingTree<TextDumpNode> {
		public TextDumpNode() {
			super();
		}

		private TextDumpNode(TextDumpNode parent) {
			super(parent);
		}

		@Override
		protected TextDumpNode createChild() {
			return new TextDumpNode(this);
		}

		public void dump(PrintWriter pw) {
			if (!methodHandlers.isEmpty()) {
				dumpHandlers(pw);
				pw.println("");
			}
			dumpSegments(pw);
		}

		private void dumpSegments(PrintWriter pw) {
			for (Map.Entry<String, TextDumpNode> e : segmentMatches.entrySet()) {
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
							Collections2.transform(methodHandlers.entries(), e -> e.getKey() + ":" + e.getValue())));
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

	private static class HtmlDumpNode extends RoutingTree<HtmlDumpNode> {
		public HtmlDumpNode() {
			super();
		}

		private HtmlDumpNode(HtmlDumpNode parent) {
			super(parent);
		}

		@Override
		protected HtmlDumpNode createChild() {
			return new HtmlDumpNode(this);
		}

		public void dump(PrintWriter pw) {
			if (!methodHandlers.isEmpty()) {
				dumpHandlers(pw);
			}
			dumpSegments(pw);
		}

		private void dumpSegments(PrintWriter pw) {
			pw.println("<dl class='segments'>");
			for (Map.Entry<String, HtmlDumpNode> e : segmentMatches.entrySet()) {
				pw.print("<dt>");
				pw.print(e.getKey());
				pw.print("</dt>");
				pw.print("<dd>");
				e.getValue().dumpHandlers(pw);
				e.getValue().dumpSegments(pw);
				pw.print("</dd>");
			}
			pw.println("</dl>");
		}

		private void dumpHandlers(PrintWriter pw) {
			pw.println("<ul class='handlers'>");
			for (Map.Entry<String, String> e : methodHandlers.entries()) {
				pw.print("<li>");
				pw.print(e.getKey());
				pw.print(":");
				pw.print(e.getValue());
				pw.println("</li>");
			}
			pw.println("</ul>");
		}
	}
}

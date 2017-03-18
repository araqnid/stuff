package org.araqnid.stuff;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.spi.Registry;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

@Path("_api/info")
public class InfoResources {
	private final AppVersion appVersion;
	private final Registry registry;

	@Inject
	public InfoResources(AppVersion appVersion,
						 Registry registry) {
		this.appVersion = appVersion;
		this.registry = registry;
	}

	@GET
	@Path("version")
	@Produces({ "application/json", "application/xml" })
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
	@Path("routing")
	@Produces("application/json")
	public Multimap<String, InvokerDetail> getRouting() {
		ResourceMethodRegistry rmr = (ResourceMethodRegistry) registry;
		Multimap<String, InvokerDetail> output = ArrayListMultimap.create();
		for (Map.Entry<String, List<ResourceInvoker>> e : rmr.getBounded().entrySet()) {
			for (ResourceInvoker invoker : e.getValue()) {
				Method method = invoker.getMethod();
				InvokerDetail invokerInfo;
				if (invoker instanceof ResourceMethodInvoker) {
					ResourceMethodInvoker resourceMethodInvoker = (ResourceMethodInvoker) invoker;
					invokerInfo = new InvokerDetail(method.getName(), resourceMethodInvoker.getResourceClass()
							.getSimpleName(), resourceMethodInvoker.getHttpMethods(),
							Arrays.stream(resourceMethodInvoker.getConsumes())
									.map(Object::toString)
									.collect(toSet()),
							Arrays.stream(resourceMethodInvoker.getProduces())
									.map(Object::toString)
									.collect(toSet()));
				}
				else {
					invokerInfo = new InvokerDetail(method.getName(), method.getDeclaringClass().getName(), ImmutableSet.of(), ImmutableSet.of(), ImmutableSet.of());
				}
				output.put(e.getKey(), invokerInfo);
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
		pw.println("<html><head><title>Routing</title><style type='text/css'>" + stylesheet() + "</style></head><body>");
		routingToTree(new HtmlDumpNode()).dump(pw);
		pw.println("</body></html>");
		return sw.toString();
	}

	private <T extends RoutingTree<T>> T routingToTree(T root) {
		for (Map.Entry<String, Collection<InvokerDetail>> e : getRouting().asMap().entrySet()) {
			Iterator<String> segmentIter = Splitter.on('/').omitEmptyStrings().split(e.getKey()).iterator();
			T cursor = root;
			while (segmentIter.hasNext()) {
				cursor = cursor.matchSegment(segmentIter.next());
			}
			for (InvokerDetail invoker : e.getValue()) {
				cursor.addInvoker(invoker);
			}
		}
		return root;
	}

	private static abstract class RoutingTree<T extends RoutingTree<T>> {
		final T parent;
		final Map<String, T> segmentMatches = new TreeMap<>();
		final Set<InvokerDetail> methodInvokers = Sets.newHashSet();

		RoutingTree() {
			this.parent = null;
		}

		RoutingTree(T parent) {
			this.parent = parent;
		}

		T matchSegment(String segment) {
			return segmentMatches.computeIfAbsent(segment, k -> createChild());
		}

		void addInvoker(InvokerDetail invoker) {
			methodInvokers.add(invoker);
		}

		protected abstract T createChild();
	}

	private static class TextDumpNode extends RoutingTree<TextDumpNode> {
		TextDumpNode() {
			super();
		}

		private TextDumpNode(TextDumpNode parent) {
			super(parent);
		}

		@Override
		protected TextDumpNode createChild() {
			return new TextDumpNode(this);
		}

		void dump(PrintWriter pw) {
			if (!methodInvokers.isEmpty()) {
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
			if (methodInvokers.isEmpty()) return;
			String handlers = methodInvokers
					.stream()
					.sorted(INVOKER_ORDERING)
					.flatMap(
							invoker -> invoker.httpMethods.stream()
									.map(method -> MethodInvokerPair.of(method, invoker)))
					.map(pair -> pair.method + ":" + pair.invoker.resourceClass + "." + pair.invoker.method)
					.collect(joining(" "));
			pw.append(" = ").append(handlers);
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
		HtmlDumpNode() {
			super();
		}

		private HtmlDumpNode(HtmlDumpNode parent) {
			super(parent);
		}

		@Override
		protected HtmlDumpNode createChild() {
			return new HtmlDumpNode(this);
		}

		void dump(PrintWriter pw) {
			dumpHandlers(pw);
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
			if (methodInvokers.isEmpty()) return;
			pw.println("<ul class='handlers'>");
			List<InvokerDetail> sortedInvokers = new ArrayList<>(methodInvokers);
			sortedInvokers.sort(INVOKER_ORDERING);
			for (InvokerDetail invoker : sortedInvokers) {
				for (String httpMethod : invoker.httpMethods) {
					pw.print("<li>");
					pw.print(httpMethod);
					pw.print(":");
					pw.print(invoker.resourceClass);
					pw.print('.');
					pw.print(invoker.method);
					if (!invoker.consumes.isEmpty()) {
						pw.print("<ul class='consumes'>");
						for (String mimeType : invoker.consumes) {
							pw.print("<li>");
							pw.print(mimeType);
							pw.print("</li>");
						}
						pw.print("</ul>");
					}
					if (!invoker.produces.isEmpty()) {
						pw.print("<ul class='produces'>");
						for (String mimeType : invoker.produces) {
							pw.print("<li>");
							pw.print(mimeType);
							pw.print("</li>");
						}
						pw.print("</ul>");
					}
					pw.println("</li>");
				}
			}
			pw.println("</ul>");
		}
	}

	private static String stylesheet() {
		URL resource = Resources.getResource(InfoResources.class, "routing.css");
		try {
			return Resources.asCharSource(resource, StandardCharsets.UTF_8).read();
		} catch (IOException e) {
			throw new IllegalStateException("Unable to read stylesheet resource: " + resource, e);
		}
	}

	static class InvokerDetail {
		@JsonProperty final String method;
		@JsonProperty final String resourceClass;
		@JsonProperty final Set<String> httpMethods;
		@JsonProperty final Set<String> consumes;
		@JsonProperty final Set<String> produces;

		InvokerDetail(String method,
					  String resourceClass,
					  Set<String> httpMethods,
					  Set<String> consumes,
					  Set<String> produces) {
			this.method = method;
			this.resourceClass = resourceClass;
			this.httpMethods = ImmutableSet.copyOf(httpMethods);
			this.consumes = ImmutableSet.copyOf(consumes);
			this.produces = ImmutableSet.copyOf(produces);
		}
	}

	private static final Comparator<InvokerDetail> INVOKER_ORDERING = comparing((InvokerDetail id) -> id.resourceClass).thenComparing(id -> id.method);

	private static class MethodInvokerPair {
		final String method;
		final InvokerDetail invoker;

		private MethodInvokerPair(String method, InvokerDetail invoker) {
			this.method = method;
			this.invoker = invoker;
		}

		public static MethodInvokerPair of(String method, InvokerDetail invoker) {
			return new MethodInvokerPair(method, invoker);
		}

		@Override
		public int hashCode() {
			return Objects.hash(method, invoker);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof MethodInvokerPair && Objects.equals(method, ((MethodInvokerPair) obj).method)
					&& Objects.equals(invoker, ((MethodInvokerPair) obj).invoker);
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).addValue(method).addValue(invoker).toString();
		}
	}
}

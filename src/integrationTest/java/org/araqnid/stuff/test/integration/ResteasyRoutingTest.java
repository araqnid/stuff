package org.araqnid.stuff.test.integration;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.araqnid.stuff.HelloResource;
import org.araqnid.stuff.InfoResources;
import org.araqnid.stuff.config.AppConfig;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.plugins.guice.ModuleProcessor;
import org.junit.BeforeClass;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Guice;

@RunWith(Theories.class)
public class ResteasyRoutingTest {
	private static Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();

	@BeforeClass
	public static void setupDispatcher() {
		new ModuleProcessor(dispatcher.getRegistry(), dispatcher.getProviderFactory()).processInjector(Guice
				.createInjector(new AppConfig()).createChildInjector(new AppConfig.ResteasyModule()));
	}

	@DataPoint
	public static Routing get_hello = new Routing("/hello/kitty", HelloResource.class, "hello") {{
			withPathParameters(parameters("name", "kitty"));
	}};
	@DataPoint
	public static Routing put_hello = new Routing("/hello/kitty", HelloResource.class, "receiveHello") {{
			withPathParameters(parameters("name", "kitty"));
			whenUsing("PUT");
			whenBodyContentType("text/plain");
	}};
	@DataPoint
	public static Routing delete_hello = new Routing("/hello/kitty", HelloResource.class, "deleteHello") {{
			withPathParameters(parameters("name", "kitty"));
			whenUsing("DELETE");
	}};

	@DataPoint
	public static Routing info_version = new Routing("/info/version", InfoResources.class, "getVersion");
	@DataPoint
	public static Routing info_state = new Routing("/info/state", InfoResources.class, "getAppState");
	@DataPoint
	public static Routing info_routing = new Routing("/info/routing", InfoResources.class, "getRouting");

	public static class Routing {
		public final MockHttpRequest request;
		public final Class<?> resourceClass;
		public final String methodName;
		public Multimap<String, String> pathParameters = newParametersMultimap();

		public Routing(String path, Class<?> resourceClass, String methodName) {
			try {
				this.request = MockHttpRequest.get(path);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
			List<Method> methods = new ArrayList<>();
			for (Method method : resourceClass.getMethods()) {
				if (method.getName().equals(methodName)) {
					methods.add(method);
				}
			}
			if (methods.isEmpty())
				throw new IllegalArgumentException("No such method \"" + methodName + "\" on " + resourceClass.getName());
			if (methods.size() > 1)
				throw new IllegalArgumentException("Multiple methods called \"" + methodName + "\" on " + resourceClass.getName());
			this.resourceClass = resourceClass;
			this.methodName = methodName;
		}

		public Routing withPathParameters(Multimap<String, String> pathParameters) {
			this.pathParameters = pathParameters;
			return this;
		}

		public Routing whenUsing(String httpMethod) {
			request.setHttpMethod(httpMethod);
			return this;
		}

		public Routing whenBodyContentType(String contentType) {
			request.contentType(contentType);
			return this;
		}

		public MockHttpRequest theRequest() {
			return request;
		}
	}

	@Theory
	public void path_is_mapped_to_resource(Routing routing) throws Exception {
		MockHttpRequest request = routing.theRequest();
		ResourceInvoker resourceInvoker = dispatcher.getRegistry().getResourceInvoker(request);
		MatcherAssert.assertThat(resourceInvoker, is_to_method(routing.resourceClass, routing.methodName));
	}

	@Theory
	public void path_parameters_are_captured(Routing routing) throws Exception {
		MockHttpRequest request = routing.theRequest();
		dispatcher.getRegistry().getResourceInvoker(request);
		MatcherAssert.assertThat(request.getUri().getPathParameters(), is_like(routing.pathParameters));
	}

	private static Matcher<MultivaluedMap<String, String>> is_like(final Multimap<String, String> expected) {
		return new TypeSafeDiagnosingMatcher<MultivaluedMap<String, String>>() {
			@Override
			protected boolean matchesSafely(MultivaluedMap<String, String> item, Description mismatchDescription) {
				Multimap<String, String> multimap = newParametersMultimap();
				for (Map.Entry<String, List<String>> e : item.entrySet()) {
					for (String str : e.getValue()) {
						multimap.put(e.getKey(), str);
					}
				}
				if (!multimap.equals(expected)) {
					mismatchDescription.appendValue(item);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("like ").appendValue(expected);
			}
		};
	}

	private static Multimap<String, String> parameters(String k, String v) {
		Multimap<String, String> multimap = newParametersMultimap();
		multimap.put(k, v);
		return multimap;
	}

	private static Multimap<String, String> newParametersMultimap() {
		return Multimaps.newMultimap(new HashMap<String, Collection<String>>(), new Supplier<List<String>>() {
			@Override
			public List<String> get() {
				return new ArrayList<>();
			}
		});
	}

	private static Matcher<ResourceInvoker> is_to_method(final Class<?> resourceClass, final String methodName) {
		return new TypeSafeDiagnosingMatcher<ResourceInvoker>() {
			@Override
			protected boolean matchesSafely(ResourceInvoker item, Description mismatchDescription) {
				Method method = item.getMethod();
				if (method.getDeclaringClass() != resourceClass || !method.getName().equals(methodName)) {
					mismatchDescription.appendText("invoker method is ").appendValue(method);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("invoker on ").appendValue(resourceClass).appendText(" method ")
						.appendValue(methodName);
			}
		};
	}
}

package org.araqnid.stuff.config;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.araqnid.stuff.AppStateMonitor;
import org.araqnid.stuff.AppVersion;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;

public class ElasticSearchModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(ElasticSearchService.class).in(Singleton.class);
		services().addBinding().to(ElasticSearchService.class);
	}

	@Provides
	public Node node(ElasticSearchService service) {
		return service.node;
	}

	@Provides
	public Client client(ElasticSearchService service) {
		return service.client;
	}

	private Multibinder<Service> services() {
		return Multibinder.newSetBinder(binder(), Service.class);
	}

	public static final class ElasticSearchService extends AbstractIdleService {
		private final Injector injector;
		private Node node;
		private Client client;
		private org.elasticsearch.common.inject.Injector esInject;

		@Inject
		public ElasticSearchService(Injector injector) {
			this.injector = injector;
		}

		@Override
		protected void startUp() throws Exception {
			node = NodeBuilder
					.nodeBuilder()
					.settings(
							ImmutableSettings.builder().put("node.name", "testnode").put("path.data", "/tmp/data")
									.put("http.enabled", true).build()).clusterName("testcluster").data(true)
					.local(true).node();
			client = node.client();
			esInject = ((InternalNode) node).injector();
			registerRestHandler(RestRequest.Method.GET, "/_info/version", InfoVersionRestHandler.class);
			registerRestHandler(RestRequest.Method.GET, "/_info/state", InfoStateRestHandler.class);
		}

		private void registerRestHandler(RestRequest.Method method, String path, Class<? extends RestHandler> clazz) {
			RestController restController = esInject.getInstance(RestController.class);
			RestHandler restHandler = esInject.getInstance(clazz);
			injector.injectMembers(restHandler);
			restController.registerHandler(method, path, restHandler);
		}

		@Override
		protected void shutDown() throws Exception {
			client.close();
			node.close();
		}
	}

	public static final class InfoVersionRestHandler extends BaseRestHandler {
		@Inject
		private AppVersion appVersion;

		@org.elasticsearch.common.inject.Inject
		public InfoVersionRestHandler(Settings settings, RestController controller, Client client) {
			super(settings, controller, client);
		}

		@Override
		protected void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
			XContentBuilder builder = channel.newBuilder();
			builder.startObject().field("version", appVersion.version).field("title", appVersion.title)
					.field("vendor", appVersion.vendor).endObject();
			channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));
		}
	}

	public static final class InfoStateRestHandler extends BaseRestHandler {
		@Inject
		private AppStateMonitor appStateMonitor;

		@org.elasticsearch.common.inject.Inject
		public InfoStateRestHandler(Settings settings, RestController controller, Client client) {
			super(settings, controller, client);
		}

		@Override
		protected void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
			XContentBuilder builder = channel.newBuilder();
			builder.startObject().field("state", appStateMonitor.getState()).endObject();
			channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));
		}
	}
}

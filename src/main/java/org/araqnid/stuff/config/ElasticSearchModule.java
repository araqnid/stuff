package org.araqnid.stuff.config;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.inject.Singleton;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;

public class ElasticSearchModule extends AbstractModule {
	private final String clusterName;
	private final String nodeName;
	private final File home;

	public ElasticSearchModule(String clusterName, String nodeName, File home) {
		this.clusterName = clusterName;
		this.nodeName = nodeName;
		this.home = home;
	}

	public ElasticSearchModule(String clusterName, File home) {
		this(clusterName, localhost(), home);
	}

	@Override
	protected void configure() {
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

	@Provides
	@Singleton
	public ElasticSearchService service() {
		return new ElasticSearchService(clusterName, nodeName, home);
	}

	private Multibinder<Service> services() {
		return Multibinder.newSetBinder(binder(), Service.class);
	}

	public static final class ElasticSearchService extends AbstractIdleService {
		private final String clusterName;
		private final String nodeName;
		private final File home;

		public ElasticSearchService(String clusterName, String nodeName, File home) {
			this.clusterName = clusterName;
			this.nodeName = nodeName;
			this.home = home;
		}

		private Node node;
		private Client client;

		@Override
		protected void startUp() throws Exception {
			node = NodeBuilder.nodeBuilder()
					.settings(Settings.builder().put("node.name", nodeName).put("path.home", home.toString())
							.put("network.bind_host", "0.0.0.0").put("network.publish_host", "_non_loopback_")
							.put("http.enabled", true).put("http.port", 19200).build())
					.clusterName(clusterName).data(true).local(true).node();
			client = node.client();
		}

		@Override
		protected void shutDown() throws Exception {
			client.close();
			node.close();
		}
	}

	private static String localhost() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "localhost";
		}
	}
}

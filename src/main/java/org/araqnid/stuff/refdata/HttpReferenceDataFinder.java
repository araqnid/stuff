package org.araqnid.stuff.refdata;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

public class HttpReferenceDataFinder implements ReferenceDataFinder<String, String> {
	private final CloseableHttpClient httpClient;
	private final URI uri;
	private final ObjectMapper objectMapper;

	public HttpReferenceDataFinder(CloseableHttpClient httpClient, URI uri, ObjectMapper objectMapper) {
		this.httpClient = httpClient;
		this.uri = uri;
		this.objectMapper = objectMapper;
	}

	@Override
	public Map<String, String> get(Set<String> keys) {
		URI requestUri;
		try {
			requestUri = new URIBuilder(uri).setCustomQuery(Joiner.on('|').join(keys)).build();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Unable to make URI", e);
		}
		try {
			HttpGet request = new HttpGet(requestUri);
			try (CloseableHttpResponse response = httpClient.execute(request)) {
				try (InputStream stream = response.getEntity().getContent()) {
					ObjectNode root = (ObjectNode) objectMapper.readTree(stream);
					Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
					ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
					while (fields.hasNext()) {
						Map.Entry<String, JsonNode> next = fields.next();
						builder.put(next.getKey(), next.getValue().asText());
					}
					return builder.build();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Unable to parse response from " + requestUri, e);
		}
	}
}

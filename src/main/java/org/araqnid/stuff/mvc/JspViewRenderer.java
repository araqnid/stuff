package org.araqnid.stuff.mvc;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.jasper.Constants;
import org.araqnid.stuff.config.MvcPathPattern;

@javax.ws.rs.ext.Provider
public class JspViewRenderer implements MessageBodyWriter<View> {
	private final Provider<HttpServletRequest> requestProvider;
	private final Provider<HttpServletResponse> responseProvider;
	private final String pathPattern;

	@Inject
	public JspViewRenderer(Provider<HttpServletRequest> requestProvider, Provider<HttpServletResponse> responseProvider, @MvcPathPattern String pathPattern) {
		this.requestProvider = requestProvider;
		this.responseProvider = responseProvider;
		this.pathPattern = pathPattern;
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return type == View.class;
	}

	@Override
	public long getSize(View view, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(View view, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException,
			WebApplicationException {
		HttpServletRequest httpServletRequest = requestProvider.get();
		HttpServletResponse httpServletResponse = responseProvider.get();
		String targetPath = String.format(pathPattern, view.name);

		httpServletRequest.setAttribute(Constants.JSP_FILE, targetPath);
		for (Map.Entry<String, Object> e : view.attributes.entrySet()) {
			httpServletRequest.setAttribute(e.getKey(), e.getValue());
		}

		try {
			httpServletRequest.getRequestDispatcher(targetPath).forward(httpServletRequest, httpServletResponse);
		} catch (ServletException e) {
			throw new WebApplicationException(e);
		}
	}
}

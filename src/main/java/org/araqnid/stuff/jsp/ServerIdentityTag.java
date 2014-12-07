package org.araqnid.stuff.jsp;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.araqnid.stuff.config.ServerIdentity;

import com.google.inject.Inject;

public class ServerIdentityTag extends SimpleTagSupport {
	@Inject
	@ServerIdentity
	private UUID serverIdentity;

	@Override
	public void doTag() throws JspException, IOException {
		getJspContext().getOut().print(serverIdentity);
	}
}

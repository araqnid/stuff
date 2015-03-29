package org.araqnid.stuff.jsp;

import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.araqnid.stuff.config.ServerIdentity;

public class ServerIdentityTag extends SimpleTagSupport {
	@Inject
	@ServerIdentity
	private UUID serverIdentity;

	@Override
	public void doTag() throws JspException, IOException {
		getJspContext().getOut().print(serverIdentity);
	}
}

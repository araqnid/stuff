package org.araqnid.stuff.jsp;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class ThingTag extends SimpleTagSupport {
	private String id;

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public void doTag() throws JspException, IOException {
		JspWriter out = getJspContext().getOut();
		out.print("<span id=\"");
		out.print(id);
		out.print("\">");
		getJspBody().invoke(out);
		out.print("</span>");
	}
}

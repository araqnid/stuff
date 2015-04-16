package org.araqnid.stuff.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class TextualTimestampsModule extends SimpleModule {
	private static final long serialVersionUID = 2015041601L;

	@Override
	public void setupModule(SetupContext context) {
		ObjectMapper owner = context.getOwner();
		owner.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		super.setupModule(context);
	}
}

package org.araqnid.stuff.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class NamingJacksonModule extends SimpleModule {
	private static final long serialVersionUID = 2014082501L;

	public NamingJacksonModule() {
		setNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
	}
}

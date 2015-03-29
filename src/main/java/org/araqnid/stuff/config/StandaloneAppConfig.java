package org.araqnid.stuff.config;

import java.util.Map;

import javax.inject.Named;

import org.araqnid.stuff.AppStartupBanner;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class StandaloneAppConfig extends AbstractModule {
	private Map<String, String> environment;

	public StandaloneAppConfig() {
		this(System.getenv());
	}

	@VisibleForTesting
	public StandaloneAppConfig(Map<String, String> environment) {
		this.environment = environment;
	}

	@Override
	protected void configure() {
		install(new CoreModule());
		install(new JettyModule(port(61000)));
		bind(AppStartupBanner.class);
	}

	@Provides
	@Named("pgUser")
	public Optional<String> pgUser() {
		return getenv("PGUSER");
	}

	@Provides
	@Named("pgPassword")
	public Optional<String> pgPassword() {
		return getenv("PGPASSWORD");
	}

	@Provides
	@Named("pgDatabase")
	public Optional<String> pgDatabase() {
		return getenv("PGDATABASE");
	}

	private Optional<String> getenv(String name) {
		return Optional.fromNullable(environment.get(name));
	}

	private int port(int defaultPort) {
		return getenv("PORT").transform(new Function<String, Integer>() {
			@Override
			public Integer apply(String input) {
				return Integer.valueOf(input);
			}
		}).or(defaultPort);
	}
}

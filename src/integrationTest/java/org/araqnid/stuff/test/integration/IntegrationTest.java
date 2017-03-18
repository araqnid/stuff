package org.araqnid.stuff.test.integration;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.junit.Rule;

import static java.time.ZoneOffset.UTC;

public class IntegrationTest {
	final ManuallySetClock clock = new ManuallySetClock(Instant.parse("2012-11-10T09:08:07.006005004Z"), UTC);

	@Rule
	public final ServerRunner server = new ServerRunner(this::environment, new AbstractModule() {
		@Override
		protected void configure() {
			bind(Clock.class).toInstance(clock);
			install(serverConfiguration());
		}
	});

	@SuppressWarnings("WeakerAccess")
	protected Map<String, String> environment() {
		return ImmutableMap.of();
	}

	protected Module serverConfiguration() {
		return new AbstractModule() {
			@Override
			protected void configure() {
			}
		};
	}
}

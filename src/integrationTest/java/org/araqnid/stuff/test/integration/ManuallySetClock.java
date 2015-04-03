package org.araqnid.stuff.test.integration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A clock that can be set to a different time manually, but does not automatically tick.
 */
public class ManuallySetClock extends Clock {
	private Instant time;
	private ZoneId zone;

	public ManuallySetClock(Instant time, ZoneId zone) {
		this.time = time;
		this.zone = zone;
	}

	public void setTime(Instant time) {
		this.time = time;
	}

	@Override
	public ZoneId getZone() {
		return zone;
	}

	@Override
	public Clock withZone(ZoneId newZone) {
		return new Clock() {
			@Override
			public ZoneId getZone() {
				return newZone;
			}

			@Override
			public Clock withZone(ZoneId newNewZone) {
				return ManuallySetClock.this.withZone(newNewZone);
			}

			@Override
			public Instant instant() {
				return time;
			}
		};
	}

	@Override
	public Instant instant() {
		return time;
	}
}

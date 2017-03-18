package org.araqnid.stuff.test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Supplier;

import com.google.common.base.MoreObjects;

public final class SupplierClock extends Clock {
	private final Supplier<Instant> instantSupplier;
	private final ZoneId zoneId;

	public SupplierClock(Supplier<Instant> instantSupplier, ZoneId zoneId) {
		this.instantSupplier = instantSupplier;
		this.zoneId = zoneId;
	}

	@Override
	public ZoneId getZone() {
		return zoneId;
	}

	@Override
	public Clock withZone(ZoneId overriddenZone) {
		return new Clock() {
			@Override
			public ZoneId getZone() {
				return overriddenZone;
			}

			@Override
			public Clock withZone(ZoneId zone) {
				return SupplierClock.this.withZone(zone);
			}

			@Override
			public Instant instant() {
				return instantSupplier.get();
			}

			@Override
			public String toString() {
				return MoreObjects.toStringHelper(this)
						.add("instantSupplier", instantSupplier)
						.add("overriddenZone", overriddenZone)
						.toString();
			}
		};
	}

	@Override
	public Instant instant() {
		return instantSupplier.get();
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("instantSupplier", instantSupplier)
				.add("zoneId", zoneId)
				.toString();
	}
}

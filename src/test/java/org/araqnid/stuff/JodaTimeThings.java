package org.araqnid.stuff;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.chrono.IslamicChronology;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

public class JodaTimeThings {
	@Test
	public void date_times_of_the_same_instant_with_the_same_time_zones_are_equal() throws Exception {
		Instant instant = Instant.parse("2015-01-13T01:02:03Z");
		DateTime dt1 = new DateTime(instant.getMillis(), DateTimeZone.UTC);
		DateTime dt2 = new DateTime(instant.getMillis(), DateTimeZone.UTC);
		assertThat(dt1, equalTo(dt2));
	}

	@Test
	public void date_times_of_the_same_instant_with_different_time_zones_are_not_equal() throws Exception {
		Instant instant = Instant.parse("2015-01-13T01:02:03Z");
		DateTime dt1 = new DateTime(instant.getMillis(), DateTimeZone.forID("Europe/London"));
		DateTime dt2 = new DateTime(instant.getMillis(), DateTimeZone.forID("America/Los_Angeles"));
		assertThat(dt1, not(equalTo(dt2)));
	}

	@Test
	public void date_times_of_the_same_instant_with_different_chronologies_are_not_equal() throws Exception {
		Instant instant = Instant.parse("2015-01-13T01:02:03Z");
		DateTime dt1 = new DateTime(instant.getMillis(), ISOChronology.getInstanceUTC());
		DateTime dt2 = new DateTime(instant.getMillis(), IslamicChronology.getInstanceUTC());
		assertThat(dt1, not(equalTo(dt2)));
	}
}

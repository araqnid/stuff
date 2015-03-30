package org.araqnid.stuff;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

public class TimeThings {
	// $ perl -MDate::Parse -le 'print str2time("2015-03-29T23:12:37Z")'
	// 1427670757
	@Test
	public void instant_can_be_made_from_epoch_time() throws Exception {
		assertThat(Instant.ofEpochSecond(1427670757L), equalTo(Instant.parse("2015-03-29T23:12:37Z")));
	}

	@Test
	public void legacy_date_can_be_converted_to_instant() throws Exception {
		assertThat(new java.util.Date(1427670757000L).toInstant(), equalTo(Instant.parse("2015-03-29T23:12:37Z")));
	}

	@Test
	public void offset_date_time_can_be_made_from_component_parts() throws Exception {
		OffsetDateTime now = OffsetDateTime.of(2015, 3, 30, 0, 12, 37, 0, ZoneOffset.ofHours(1));
		assertThat(now, equalTo(OffsetDateTime.parse("2015-03-30T00:12:37+01:00")));
	}

	@Test
	public void zoned_date_time_can_be_made_from_local_date_time_and_time_zone() throws Exception {
		LocalDateTime nowDateTime = LocalDateTime.parse("2015-03-30T00:12:37");
		ZoneId nowTimeZone = ZoneId.of("Europe/London");
		assertThat(ZonedDateTime.of(nowDateTime, nowTimeZone),
				equalTo(ZonedDateTime.parse("2015-03-30T00:12:37+01:00[Europe/London]")));
	}

	@Test
	public void zoned_date_time_can_be_made_from_component_parts() throws Exception {
		ZonedDateTime now = ZonedDateTime.of(2015, 3, 30, 0, 12, 37, 0, ZoneId.of("Europe/London"));
		assertThat(now, equalTo(ZonedDateTime.parse("2015-03-30T00:12:37+01:00[Europe/London]")));
	}

	@Test
	public void offset_date_time_can_be_made_from_local_date_time_and_offset() throws Exception {
		LocalDateTime nowDateTime = LocalDateTime.parse("2015-03-30T00:12:37");
		ZoneOffset nowOffset = ZoneOffset.of("+01:00");
		assertThat(OffsetDateTime.of(nowDateTime, nowOffset),
				equalTo(OffsetDateTime.parse("2015-03-30T00:12:37+01:00")));
	}

	@Test
	public void offset_date_time_can_be_converted_simply_to_instant() throws Exception {
		OffsetDateTime now = OffsetDateTime.parse("2015-03-30T00:12:37+01:00");
		assertThat(now.toString(), equalTo("2015-03-30T00:12:37+01:00"));
		assertThat(now.toInstant(), equalTo(Instant.parse("2015-03-29T23:12:37Z")));
	}

	@Test
	public void zoned_date_time_can_be_converted_simply_to_instant() throws Exception {
		ZonedDateTime now = ZonedDateTime.parse("2015-03-30T00:12:37+01:00[Europe/London]");
		assertThat(now.toString(), equalTo("2015-03-30T00:12:37+01:00[Europe/London]"));
		assertThat(now.toInstant(), equalTo(Instant.parse("2015-03-29T23:12:37Z")));
	}

	@Test
	public void zoned_date_time_calculates_offset_using_time_zone_rules() throws Exception {
		ZonedDateTime now = ZonedDateTime.parse("2015-01-30T00:12:37+01:00[Europe/London]");
		assertThat(now.toString(), equalTo("2015-01-30T00:12:37Z[Europe/London]"));
		assertThat(now.toInstant(), equalTo(Instant.parse("2015-01-30T00:12:37Z")));
	}

	@Test
	public void instant_can_be_converted_using_arbitrary_offset() throws Exception {
		Instant now = Instant.parse("2015-03-29T23:12:37Z");
		assertThat(now.atOffset(ZoneOffset.ofHours(3)), equalTo(OffsetDateTime.parse("2015-03-30T02:12:37+03:00")));
		assertThat(now.atOffset(ZoneOffset.ofHours(-7)), equalTo(OffsetDateTime.parse("2015-03-29T16:12:37-07:00")));
	}

	@Test
	public void instant_can_be_converted_using_arbitrary_time_zone() throws Exception {
		Instant now = Instant.parse("2015-03-29T23:12:37Z");
		assertThat(now.atZone(ZoneId.of("Europe/Moscow")),
				equalTo(ZonedDateTime.parse("2015-03-30T02:12:37+03:00[Europe/Moscow]")));
		assertThat(now.atZone(ZoneId.of("America/Los_Angeles")),
				equalTo(ZonedDateTime.parse("2015-03-29T16:12:37-07:00[America/Los_Angeles]")));
	}

	@Test
	public void offset_date_time_can_be_converted_to_zoned_date_time_in_fixed_time_zone() throws Exception {
		OffsetDateTime nowOffset = OffsetDateTime.parse("2015-03-30T00:12:37+01:00");
		assertThat(nowOffset.toZonedDateTime(), equalTo(ZonedDateTime.parse("2015-03-30T00:12:37+01:00")));
		assertThat(nowOffset.toZonedDateTime().getZone(), equalTo(ZoneOffset.ofHours(1)));
	}

	@Test
	public void offset_date_time_can_be_converted_to_zoned_date_time_at_same_instant() throws Exception {
		OffsetDateTime nowOffset = OffsetDateTime.parse("2015-03-30T00:12:37+01:00");
		assertThat(nowOffset.atZoneSameInstant(ZoneId.of("America/Los_Angeles")),
				equalTo(ZonedDateTime.parse("2015-03-29T16:12:37-07:00[America/Los_Angeles]")));
	}

	@Test
	public void offset_date_time_can_be_converted_to_zoned_date_time_at_same_local_datetime() throws Exception {
		OffsetDateTime nowOffset = OffsetDateTime.parse("2015-03-30T00:12:37+01:00");
		assertThat(nowOffset.atZoneSimilarLocal(ZoneId.of("America/Los_Angeles")),
				equalTo(ZonedDateTime.parse("2015-03-30T00:12:37+03:00[America/Los_Angeles]")));
	}

	@Test
	public void offset_date_time_corresponding_to_non_existent_zoned_date_time_is_adjusted() throws Exception {
		OffsetDateTime nowOffset = OffsetDateTime.parse("2015-03-29T01:30:00+01:00");
		assertThat(nowOffset.atZoneSimilarLocal(ZoneId.of("Europe/London")).toLocalDateTime(),
				equalTo(LocalDateTime.parse("2015-03-29T02:30:00")));
	}

	@Test
	public void zoned_date_time_can_be_converted_simply_to_offset_date_time() throws Exception {
		ZonedDateTime now = ZonedDateTime.parse("2015-03-30T00:12:37+01:00[Europe/London]");
		assertThat(now.toOffsetDateTime(), equalTo(OffsetDateTime.parse("2015-03-30T00:12:37+01:00")));
	}

	@Test
	public void zoned_date_time_loses_rules_in_round_trip_through_offset_date_time() throws Exception {
		ZonedDateTime now = ZonedDateTime.parse("2015-03-30T00:12:37+01:00[Europe/London]");
		assertThat(now.toOffsetDateTime().toZonedDateTime(),
				allOf(equalTo(ZonedDateTime.parse("2015-03-30T00:12:37+01:00")), not(equalTo(now))));
	}

	@Test
	public void zoned_date_time_can_be_stripped_of_rules() throws Exception {
		ZonedDateTime now = ZonedDateTime.parse("2015-03-30T00:12:37+01:00[Europe/London]");
		assertThat(
				now.withFixedOffsetZone(),
				allOf(equalTo(ZonedDateTime.parse("2015-03-30T00:12:37+01:00")), equalTo(now.toOffsetDateTime()
						.toZonedDateTime())));
	}

	@Test
	public void zoned_date_time_can_be_converted_to_different_time_zone_at_same_instant() throws Exception {
		ZonedDateTime now = ZonedDateTime.parse("2015-03-30T00:12:37+01:00[Europe/London]");
		assertThat(now.withZoneSameInstant(ZoneId.of("America/Los_Angeles")),
				equalTo(ZonedDateTime.parse("2015-03-29T16:12:37-07:00[America/Los_Angeles]")));

	}

	@Test
	public void zoned_date_time_can_be_converted_to_different_time_zone_at_same_local_datetime() throws Exception {
		ZonedDateTime now = ZonedDateTime.parse("2015-03-30T00:12:37+01:00[Europe/London]");
		assertThat(now.withZoneSameLocal(ZoneId.of("America/Los_Angeles")),
				equalTo(ZonedDateTime.parse("2015-03-30T00:12:37+03:00[America/Los_Angeles]")));

	}

	@Test
	public void clock_produces_instants() throws Exception {
		Clock clock = Clock.fixed(Instant.parse("2015-03-29T23:12:37Z"), ZoneId.of("Europe/London"));
		assertThat(clock.instant(), equalTo(Instant.parse("2015-03-29T23:12:37Z")));
	}

	@Test
	public void offset_date_time_available_from_clock() throws Exception {
		Clock clock = Clock.fixed(Instant.parse("2015-03-29T23:12:37Z"), ZoneId.of("Europe/London"));
		assertThat(OffsetDateTime.now(clock), equalTo(OffsetDateTime.parse("2015-03-30T00:12:37+01:00")));
	}

	@Test
	public void zoned_date_time_available_from_clock() throws Exception {
		Clock clock = Clock.fixed(Instant.parse("2015-03-29T23:12:37Z"), ZoneId.of("Europe/London"));
		assertThat(ZonedDateTime.now(clock), equalTo(ZonedDateTime.parse("2015-03-30T00:12:37+01:00[Europe/London]")));
	}

	@Test
	public void local_date_time_produced_from_clock_uses_clock_time_zone() throws Exception {
		Clock clock = Clock.fixed(Instant.parse("2015-03-29T23:12:37Z"), ZoneId.of("Europe/London"));
		assertThat(LocalDateTime.now(clock), equalTo(ZonedDateTime.now(clock).toLocalDateTime()));
	}
}

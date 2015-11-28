package org.araqnid.stuff.test.integration;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.araqnid.stuff.messages.RedisProcessor;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.araqnid.stuff.testutil.RandomData.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import redis.clients.jedis.Jedis;

public class RedisProcessorIntegrationTest {
	private static final Logger LOG = LoggerFactory.getLogger(RedisProcessorIntegrationTest.class);

	@Rule
	public final RedisSetup redis = new RedisSetup();

	@Test
	public void message_delivered_to_target() throws Exception {
		BlockingQueue<String> delivered = new LinkedBlockingQueue<String>();
		RedisProcessor.DeliveryTarget target = data -> {
			delivered.add(data);
			return true;
		};
		RedisProcessor<?> processor = new RedisProcessor<>(() -> new Jedis("localhost"), redis.key(), target);
		processor.startAsync().awaitRunning();
		String data = randomString();
		redis.push(data);
		Optional<String> received = Optional.ofNullable(delivered.poll(500, TimeUnit.MILLISECONDS));
		processor.stopAsync().awaitTerminated();
		assertThat(received, isValue(data));
	}

	public static final class RedisSetup extends ExternalResource {
		private Jedis jedis;
		private String key = randomString("queue");

		@Override
		protected void before() throws Throwable {
			jedis = new Jedis("localhost");
		}

		public String key() {
			return key;
		}

		public Jedis connection() {
			return jedis;
		}

		public void push(String data) {
			jedis.rpush(key, data);
		}

		@Override
		protected void after() {
			try {
				jedis.close();
			} catch (Exception e) {
				LOG.warn("Ignoring exception closing Redis connection: " + e);
			}
		}
	}

	private static <T> Matcher<Optional<T>> isValue(T value) {
		return isPresent(is(equalTo(value)));
	}

	private static <T> Matcher<Optional<T>> isPresent(Matcher<T> valueMatcher) {
		return new TypeSafeDiagnosingMatcher<Optional<T>>() {
			@Override
			protected boolean matchesSafely(Optional<T> item, Description mismatchDescription) {
				if (!item.isPresent()) {
					mismatchDescription.appendText("was absent");
					return false;
				}
				valueMatcher.describeMismatch(item.get(), mismatchDescription);
				return valueMatcher.matches(item.get());
			}

			@Override
			public void describeTo(Description description) {
				valueMatcher.describeTo(description);
			}
		};
	}
}

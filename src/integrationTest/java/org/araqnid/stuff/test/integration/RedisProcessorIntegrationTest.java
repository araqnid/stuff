package org.araqnid.stuff.test.integration;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.araqnid.stuff.messages.RedisProcessor;
import org.araqnid.stuff.zedis.ZedisClient;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;
import static org.araqnid.stuff.testutil.RandomData.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import redis.clients.jedis.Jedis;

public class RedisProcessorIntegrationTest {
	private static final Logger LOG = LoggerFactory.getLogger(RedisProcessorIntegrationTest.class);

	@Rule
	public final RedisSetup redis = new RedisSetup();

	@Rule
	public final ThreadPoolSetup threadPool = new ThreadPoolSetup();

	private ZedisClient client;

	@Before
	public void setup_client() throws Exception {
		client = new ZedisClient(threadPool.executor(), "localhost", 6379);
		client.start();
	}

	@After
	public void shutdown_client() throws Exception {
		client.close();
	}

	@Test
	public void message_delivered_to_target() throws Exception {
		BlockingQueue<String> delivered = new LinkedBlockingQueue<>();
		RedisProcessor.DeliveryTarget target = data -> {
			delivered.add(data);
			return true;
		};
		RedisProcessor processor = new RedisProcessor(client, redis.key(), target);
		processor.startAsync().awaitRunning();
		String data = randomString();
		redis.push(data);
		Optional<String> received = Optional.ofNullable(delivered.poll(500, TimeUnit.MILLISECONDS));
		processor.stopAsync().awaitTerminated();
		assertThat(received, isValue(data));
		TimeUnit.MILLISECONDS.sleep(250);
		assertThat(delivered, is(emptyIterable()));
	}

	@Test
	public void mdc_values_set_during_delivery() throws Exception {
		BlockingQueue<Map<String, String>> delivered = new LinkedBlockingQueue<>();
		RedisProcessor.DeliveryTarget target = data -> {
			delivered.add(MDC.getCopyOfContextMap());
			return true;
		};
		RedisProcessor processor = new RedisProcessor(client, redis.key(), target);
		processor.startAsync().awaitRunning();
		String data = randomString();
		redis.push(data);
		Optional<Map<String, String>> received = Optional.ofNullable(delivered.poll(500, TimeUnit.MILLISECONDS));
		processor.stopAsync().awaitTerminated();
		assertThat(received, isPresent(equalTo(ImmutableMap.of("queue", redis.key()))));
		TimeUnit.MILLISECONDS.sleep(250);
		assertThat(delivered, is(emptyIterable()));
	}

	@Test
	public void during_delivery_data_is_on_in_progress_key() throws Exception {
		BlockingQueue<QueueSizes> delivered = new LinkedBlockingQueue<>();
		RedisProcessor.DeliveryTarget target = data -> {
			long queueSize = redis.connection().llen(redis.key());
			long inProgressSize = redis.connection().llen(redis.key() + ".working");
			delivered.add(new QueueSizes(queueSize, inProgressSize));
			return true;
		};
		RedisProcessor processor = new RedisProcessor(client, redis.key(), target);
		processor.startAsync().awaitRunning();
		String data = randomString();
		redis.push(data);
		Optional<QueueSizes> received = Optional.ofNullable(delivered.poll(500, TimeUnit.MILLISECONDS));
		processor.stopAsync().awaitTerminated();
		assertThat(received, isPresent(queueSize(0, 1)));
		assertThat(delivered, is(emptyIterable()));
	}

	@Test
	public void false_return_from_delivery_target_causes_redelivery() throws Exception {
		AtomicInteger deliveryCount = new AtomicInteger();
		BlockingQueue<String> delivered = new LinkedBlockingQueue<>();
		RedisProcessor.DeliveryTarget target = data -> {
			delivered.add(data);
			return deliveryCount.getAndIncrement() == 0 ? false : true; // return false on 1st delivery
		};
		RedisProcessor processor = new RedisProcessor(client, redis.key(), target);
		processor.startAsync().awaitRunning();
		String data = randomString();
		redis.push(data);
		Optional<String> received1 = Optional.ofNullable(delivered.poll(500, TimeUnit.MILLISECONDS));
		Optional<String> received2 = Optional.ofNullable(delivered.poll(500, TimeUnit.MILLISECONDS));
		processor.stopAsync().awaitTerminated();
		assertThat(received1, isValue(data));
		assertThat(received2, isValue(data));
		TimeUnit.MILLISECONDS.sleep(250);
		assertThat(delivered, is(emptyIterable()));
	}

	@Test
	public void runtime_exception_from_delivery_target_leaves_item_on_in_progress_key() throws Exception {
		BlockingQueue<String> delivered = new LinkedBlockingQueue<>();
		RedisProcessor.DeliveryTarget target = data -> {
			delivered.add(data);
			throw new RuntimeException("boom");
		};
		RedisProcessor processor = new RedisProcessor(client, redis.key(), target);
		processor.startAsync().awaitRunning();
		String data = randomString();
		redis.push(data);
		Optional<String> received = Optional.ofNullable(delivered.poll(500, TimeUnit.MILLISECONDS));
		processor.stopAsync().awaitTerminated();
		assertThat(received, isValue(data));
		assertThat(redis.connection().llen(redis.key() + ".working"), equalTo(1L));
		assertThat(redis.connection().lindex(redis.key() + ".working", 0), equalTo(data));
		TimeUnit.MILLISECONDS.sleep(250);
		assertThat(delivered, is(emptyIterable()));
	}

	private static final class QueueSizes {
		private final long queueSize;
		private final long inProgressSize;

		public QueueSizes(long queueSize, long inProgressSize) {
			this.queueSize = queueSize;
			this.inProgressSize = inProgressSize;
		}
	}

	private static Matcher<QueueSizes> queueSize(long queueSize, long inProgressSize) {
		return new TypeSafeDiagnosingMatcher<QueueSizes>() {
			@Override
			protected boolean matchesSafely(QueueSizes item, Description mismatchDescription) {
				if (item.queueSize != queueSize) {
					mismatchDescription.appendText("queue size is ").appendValue(item.queueSize);
					return false;
				}
				if (item.inProgressSize != inProgressSize) {
					mismatchDescription.appendText("in-progress size is ").appendValue(item.inProgressSize);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("queueSize=").appendValue(queueSize).appendText(", inProgressSize=")
						.appendValue(inProgressSize);
			}
		};
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
				jedis.del(key);
				jedis.del(key + ".working");
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

	public static final class ThreadPoolSetup extends ExternalResource {
		private ExecutorService executor;
		private String displayName;

		@Override
		public Statement apply(Statement base, org.junit.runner.Description description) {
			displayName = description.getMethodName();
			return super.apply(base, description);
		}

		@Override
		protected void before() throws Throwable {
			executor = Executors.newCachedThreadPool(
					new ThreadFactoryBuilder().setNameFormat("Zedis-" + displayName + "-%d").build());
		}

		@Override
		protected void after() {
			shutdownAndAwaitTermination(executor, 1, TimeUnit.SECONDS);
		}

		public Executor executor() {
			return executor;
		}
	}
}

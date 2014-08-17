package org.araqnid.stuff.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;

public class ActivityScopeTest {
	private ActivityEventSink mockSink = Mockito.mock(ActivityEventSink.class);

	@Test
	public void passes_request_details_to_sink_and_generates_ruid() {
		ActivityScope scope = new ActivityScope();
		ActivityScopeControl scopeControl = scope.createController(mockSink);
		AppRequestType type = AppRequestType.HttpRequest;
		String description = randomString();
		scopeControl.beginRequest(type, description);
		Mockito.verify(mockSink).beginRequest(Mockito.argThat(like_a_uuid()), Mockito.anyLong(),
				Mockito.eq(type.name()), Mockito.eq(description));
	}

	@Test
	public void passes_ruid_and_request_details_to_sink() {
		ActivityScope scope = new ActivityScope();
		ActivityScopeControl scopeControl = scope.createController(mockSink);
		String ruid = randomString();
		AppRequestType type = AppRequestType.HttpRequest;
		String description = randomString();
		scopeControl.beginRequest(ruid, type, description);
		Mockito.verify(mockSink).beginRequest(Mockito.eq(ruid), Mockito.anyLong(), Mockito.eq(type.name()),
				Mockito.eq(description));
	}

	@Test
	public void remembers_ruid_when_finishing_event() {
		ActivityScope scope = new ActivityScope();
		ActivityScopeControl scopeControl = scope.createController(mockSink);
		String ruid = randomString();
		AppRequestType type = AppRequestType.HttpRequest;
		scopeControl.beginRequest(ruid, type, randomString());
		scopeControl.finishRequest(type);
		Mockito.verify(mockSink).finishRequest(Mockito.eq(ruid), Mockito.anyLong(), Mockito.eq(type.name()),
				Mockito.anyLong());
	}

	@Test
	public void does_not_delegate_provision_of_request_activity() {
		ActivityScope scope = new ActivityScope();
		Provider<RequestActivity> provider = scope.scope(Key.get(RequestActivity.class),
				invalid_provider(RequestActivity.class));
		scope.createController(mockSink).beginRequest(AppRequestType.HttpRequest, randomString());
		provider.get();
	}

	@Test
	public void does_delegate_provision_of_other_types() {
		StringGeneratingProvider unscoped = new StringGeneratingProvider();
		ActivityScope scope = new ActivityScope();
		Provider<String> provider = scope.scope(Key.get(String.class), unscoped);
		scope.createController(mockSink).beginRequest(AppRequestType.HttpRequest, randomString());
		String providedFirst = provider.get();
		MatcherAssert.assertThat(unscoped.generated, Matchers.contains(providedFirst));
	}

	@Test
	public void provides_identical_other_type_object_on_subsequent_use() {
		StringGeneratingProvider unscoped = new StringGeneratingProvider();
		ActivityScope scope = new ActivityScope();
		Provider<String> provider = scope.scope(Key.get(String.class), unscoped);
		scope.createController(mockSink).beginRequest(AppRequestType.HttpRequest, randomString());
		String providedFirst = provider.get();
		String providedSecond = provider.get();
		Assert.assertSame(providedFirst, providedSecond);
		MatcherAssert.assertThat(unscoped.generated, Matchers.contains(providedFirst));
	}

	@Test
	public void provides_request_activity_that_remembers_request_details() {
		ActivityScope scope = new ActivityScope();
		Provider<RequestActivity> provider = scope.scope(Key.get(RequestActivity.class),
				invalid_provider(RequestActivity.class));
		scope.createController(mockSink).beginRequest(AppRequestType.HttpRequest, randomString());
		RequestActivity requestActivity = provider.get();
		AppEventType eventType = AppEventType.WorkQueueItem;
		String eventDescription = randomString();
		requestActivity.beginEvent(eventType, eventDescription);
		Mockito.verify(mockSink).beginEvent(Mockito.anyString(), Mockito.anyLong(), Mockito.anyLong(),
				Mockito.eq(eventType.name()), Mockito.eq(eventDescription));
	}

	@Test
	public void request_activity_is_not_available_in_a_different_thread() throws Exception {
		ActivityScope scope = new ActivityScope();
		final Provider<RequestActivity> provider = scope.scope(Key.get(RequestActivity.class),
				invalid_provider(RequestActivity.class));
		scope.createController(mockSink).beginRequest(AppRequestType.HttpRequest, randomString());
		final List<Object> producedByThread = new ArrayList<>();
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					producedByThread.add(provider.get());
				} catch (Exception e) {
					producedByThread.add(e);
				}
			}
		});
		thread.start();
		thread.join();
		MatcherAssert.assertThat(producedByThread, Matchers.contains(Matchers.instanceOf(OutOfScopeException.class)));
	}

	@Test
	public void different_request_activity_available_in_other_thread() throws Exception {
		ActivityScope scope = new ActivityScope();
		final Provider<RequestActivity> provider = scope.scope(Key.get(RequestActivity.class),
				invalid_provider(RequestActivity.class));
		final ActivityScopeControl controller = scope.createController(mockSink);
		controller.beginRequest(AppRequestType.HttpRequest, randomString());
		final RequestActivity ourActivity = provider.get();
		final List<Object> producedByThread = new ArrayList<>();
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					controller.beginRequest(AppRequestType.HttpRequest, randomString());
					producedByThread.add(provider.get());
				} catch (Exception e) {
					producedByThread.add(e);
				}
			}
		});
		thread.start();
		thread.join();
		MatcherAssert.assertThat(
				producedByThread,
				Matchers.<Object> contains(Matchers.allOf(Matchers.instanceOf(RequestActivity.class),
						Matchers.not(Matchers.<Object> sameInstance(ourActivity)))));
	}

	@Test(expected = OutOfScopeException.class)
	public void cannot_get_request_activity_without_having_begun_a_request() {
		ActivityScope scope = new ActivityScope();
		Provider<RequestActivity> provider = scope.scope(Key.get(RequestActivity.class),
				invalid_provider(RequestActivity.class));
		provider.get();
	}

	@Test(expected = OutOfScopeException.class)
	public void cannot_finish_request_without_having_begun_one() {
		ActivityScope scope = new ActivityScope();
		ActivityScopeControl scopeControl = scope.createController(mockSink);
		scopeControl.finishRequest(AppRequestType.HttpRequest);
	}

	@Test(expected = IllegalStateException.class)
	public void cannot_finish_request_with_mismatching_type() {
		ActivityScope scope = new ActivityScope();
		ActivityScopeControl scopeControl = scope.createController(mockSink);
		scopeControl.beginRequest(AppRequestType.HttpRequest, randomString());
		scopeControl.finishRequest(AppRequestType.ScheduledJob);
	}

	private static String randomString() {
		Random random = new Random();
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		int len = 10;
		StringBuilder builder = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
		}
		return builder.toString();
	}

	private static <T> Provider<T> invalid_provider(Class<T> clazz) {
		return new Provider<T>() {
			@Override
			public T get() {
				throw new AssertionError("unscoped provider was called");
			}
		};
	}

	public static class StringGeneratingProvider implements Provider<String> {
		public final List<String> generated = new ArrayList<>();

		@Override
		public String get() {
			String value = randomString();
			generated.add(value);
			return value;
		}
	}

	private static Matcher<String> like_a_uuid() {
		return new TypeSafeDiagnosingMatcher<String>() {
			private final Pattern UUID_PATTERN = Pattern.compile(
					"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", Pattern.CASE_INSENSITIVE);

			@Override
			protected boolean matchesSafely(String item, Description mismatchDescription) {
				if (!UUID_PATTERN.matcher(item).matches()) {
					mismatchDescription.appendText("Does not look like a UUID: ").appendValue(item);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("like a UUID");
			}
		};
	}
}
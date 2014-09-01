package org.araqnid.stuff;

import java.lang.reflect.Field;

import org.araqnid.stuff.Activator.ActivationListener;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;
import com.google.inject.Provider;
import com.google.inject.util.Providers;

import static com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import static com.google.common.util.concurrent.Service.State.FAILED;
import static com.google.common.util.concurrent.Service.State.RUNNING;
import static com.google.common.util.concurrent.Service.State.STARTING;
import static com.google.common.util.concurrent.Service.State.STOPPING;
import static com.google.common.util.concurrent.Service.State.TERMINATED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ServiceActivatorTest {
	private final Service mockService = mock(Service.class);
	@SuppressWarnings("unchecked")
	private final Provider<Service> mockServiceProvider = mock(Provider.class);
	private final TestService testService = new TestService();

	private final class TestService extends AbstractService {
		@Override
		protected void doStop() {
		}

		@Override
		protected void doStart() {
		}

		public void finishStarting() {
			assertEquals(state(), State.STARTING);
			notifyStarted();
		}

		public void finishStopping() {
			assertEquals(state(), State.STOPPING);
			notifyStopped();
		}

		public void failStarting(Throwable t) {
			assertEquals(state(), State.STARTING);
			notifyFailed(t);
		}
	}

	@Before
	public void setUp() {
		when(mockServiceProvider.get()).thenReturn(mockService);
	}

	@Test
	public void start_does_nothing_when_autostart_not_enabled() {
		ServiceActivator<?> activator = new ServiceActivator<Service>(mockServiceProvider, false);
		activator.startAsync();
		assertThat(activator, isInState(RUNNING));
		verifyZeroInteractions(mockService, mockServiceProvider);
	}

	@Test
	public void start_begins_activation_when_autostart_enabled() {
		ServiceActivator<?> activator = new ServiceActivator<Service>(mockServiceProvider, true);
		activator.startAsync();
		assertThat(activator, isInState(STARTING));
		verify(mockServiceProvider).get();
		verify(mockService).startAsync();
	}

	@Test
	public void start_completes_after_underlying_service_starts_when_autostart_enabled() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), true);
		activator.startAsync();
		testService.finishStarting();
		assertThat(activator, isInState(RUNNING));
	}

	@Test
	public void calling_activate_starts_underlying_service() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), false);
		activator.startAsync();
		activator.activate();
		assertThat(testService, isInState(STARTING));
	}

	@Test
	public void activation_listener_called_after_activate_finishes_starting_underlying_service() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), false);
		activator.startAsync();
		activator.activate();
		ActivationListener listener = mock(ActivationListener.class);
		activator.addActivationListener(listener, sameThreadExecutor());
		testService.finishStarting();
		verify(listener).activated();
		verifyNoMoreInteractions(listener);
	}

	@Test
	public void activation_listener_called_after_autostart() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), true);
		activator.startAsync();
		ActivationListener listener = mock(ActivationListener.class);
		activator.addActivationListener(listener, sameThreadExecutor());
		testService.finishStarting();
		verify(listener).activated();
		verifyNoMoreInteractions(listener);
	}

	@Test
	public void activation_listener_called_after_registration_after_service_already_activated() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), false);
		activator.startAsync();
		activator.activate();
		testService.finishStarting();
		ActivationListener listener = mock(ActivationListener.class);
		activator.addActivationListener(listener, sameThreadExecutor());
		verify(listener).activated();
		verifyNoMoreInteractions(listener);
	}

	@Test
	public void calling_deactivate_stops_underlying_service() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), false);
		activator.startAsync();
		activator.activate();
		testService.finishStarting();
		activator.deactivate();
		assertThat(testService, isInState(STOPPING));
	}

	@Test
	public void calling_deactivate_when_inactive_does_nothing() {
		ServiceActivator<?> activator = new ServiceActivator<Service>(mockServiceProvider, false);
		activator.startAsync();
		activator.deactivate();
		verifyZeroInteractions(mockServiceProvider);
	}

	@Test
	public void stopping_activator_stops_underlying_service() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), false);
		activator.startAsync();
		activator.activate();
		testService.finishStarting();
		activator.stopAsync();
		assertThat(testService, isInState(STOPPING));
	}

	@Test
	public void activator_finishes_stopping_when_underlying_service_does() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), false);
		activator.startAsync();
		activator.activate();
		testService.finishStarting();
		activator.stopAsync();
		testService.finishStopping();
		assertThat(activator, isInState(TERMINATED));
	}

	@Test
	public void activation_listener_called_after_deactivate_finishes_stopping_underlying_service() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), false);
		activator.startAsync();
		activator.activate();
		testService.finishStarting();
		ActivationListener listener = mock(ActivationListener.class);
		activator.addActivationListener(listener, sameThreadExecutor());
		activator.deactivate();
		testService.finishStopping();
		verify(listener).activated();
		verify(listener).deactivated();
	}

	@Test
	public void activation_listener_called_after_stopping_autostarted_service() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), true);
		activator.startAsync();
		testService.finishStarting();
		ActivationListener listener = mock(ActivationListener.class);
		activator.addActivationListener(listener, sameThreadExecutor());
		activator.deactivate();
		testService.finishStopping();
		verify(listener).activated();
		verify(listener).deactivated();
	}

	@Test
	public void activation_listener_called_after_stopping_activator() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), true);
		activator.startAsync();
		testService.finishStarting();
		ActivationListener listener = mock(ActivationListener.class);
		activator.addActivationListener(listener, sameThreadExecutor());
		activator.stopAsync();
		testService.finishStopping();
		verify(listener).activated();
		verify(listener).deactivated();
	}

	@Test
	public void service_field_nulled_after_deactivating() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), true);
		activator.startAsync();
		testService.finishStarting();
		activator.deactivate();
		testService.finishStopping();
		assertThat(activator, hasServiceField(nullValue()));
	}

	@Test
	public void service_field_nulled_after_stopping() {
		ServiceActivator<TestService> activator = new ServiceActivator<TestService>(Providers.of(testService), true);
		activator.startAsync();
		testService.finishStarting();
		activator.stopAsync();
		testService.finishStopping();
		assertThat(activator, hasServiceField(nullValue()));
	}

	@Test
	public void activation_listener_added_while_service_is_deactivating_receives_no_activated_event() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), false);
		activator.startAsync();
		activator.activate();
		testService.finishStarting();
		activator.deactivate();
		ActivationListener listener = mock(ActivationListener.class);
		activator.addActivationListener(listener, sameThreadExecutor());
		testService.finishStopping();
		verify(listener, never()).activated();
	}

	@Test
	@Ignore("not implemented yet")
	public void activation_listener_added_while_service_is_deactivating_receives_no_deactivated_event() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), false);
		activator.startAsync();
		activator.activate();
		testService.finishStarting();
		activator.deactivate();
		ActivationListener listener = mock(ActivationListener.class);
		activator.addActivationListener(listener, sameThreadExecutor());
		testService.finishStopping();
		verify(listener, never()).deactivated();
	}

	@Test
	public void activation_listener_called_when_service_spontaneously_stops() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), false);
		activator.startAsync();
		activator.activate();
		testService.finishStarting();
		ActivationListener listener = mock(ActivationListener.class);
		activator.addActivationListener(listener, sameThreadExecutor());
		testService.stopAsync();
		testService.finishStopping();
		verify(listener).deactivated();
	}

	@Test
	public void service_field_nulled_when_service_spontaneously_stops() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), false);
		activator.startAsync();
		activator.activate();
		testService.finishStarting();
		testService.stopAsync();
		testService.finishStopping();
		assertThat(activator, hasServiceField(nullValue()));
	}

	@Test
	public void activator_goes_straight_to_failed_if_autostarting_service_fails() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), true);
		activator.startAsync();
		testService.failStarting(new Exception());
		assertThat(activator, isInState(FAILED));
	}

	@Test
	public void activator_goes_to_failed_if_activating_service_fails() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), false);
		activator.startAsync();
		activator.activate();
		testService.failStarting(new Exception());
		assertThat(activator, isInState(FAILED));
	}

	@Test
	public void service_can_be_obtained_only_when_it_is_running() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), false);
		assertThat(activator.getActiveService(), isAbsent());
		activator.startAsync();
		activator.activate();
		assertThat(activator.getActiveService(), isAbsent());
		testService.finishStarting();
		assertThat(activator.getActiveService(), isPresent(sameInstance(testService)));
		activator.deactivate();
		assertThat(activator.getActiveService(), isAbsent());
	}

	private static Matcher<Optional<?>> isAbsent() {
		return new TypeSafeDiagnosingMatcher<Optional<?>>() {
			@Override
			protected boolean matchesSafely(Optional<?> item, Description mismatchDescription) {
				if (item.isPresent()) {
					mismatchDescription.appendText("value was present: ").appendValue(item.get());
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("no value");
			}
		};
	}

	private static Matcher<Optional<?>> isPresent(final Matcher<?> valueMatcher) {
		return new TypeSafeDiagnosingMatcher<Optional<?>>() {
			@Override
			protected boolean matchesSafely(Optional<?> item, Description mismatchDescription) {
				if (!item.isPresent()) {
					mismatchDescription.appendText("value was absent");
					return false;
				}
				if (!valueMatcher.matches(item.get())) {
					valueMatcher.describeMismatch(item.get(), mismatchDescription);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("value present ").appendDescriptionOf(valueMatcher);
			}
		};
	}

	private static Matcher<Service> isInState(final State expected) {
		return new TypeSafeDiagnosingMatcher<Service>() {
			@Override
			protected boolean matchesSafely(Service item, Description mismatchDescription) {
				State actual = item.state();
				if (actual != expected) {
					mismatchDescription.appendText("state is ").appendValue(actual);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("service in state ").appendValue(expected);
			}
		};
	}

	private static Matcher<ServiceActivator<?>> hasServiceField(final Matcher<? super Service> serviceMatcher) {
		return new TypeSafeDiagnosingMatcher<ServiceActivator<?>>() {
			@Override
			protected boolean matchesSafely(ServiceActivator<?> item, Description mismatchDescription) {
				Service service;
				try {
					Field field = ServiceActivator.class.getDeclaredField("service");
					field.setAccessible(true);
					service = (Service) field.get(item);
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException("Unable to get 'service' field from " + item);
				}
				if (!serviceMatcher.matches(service)) {
					mismatchDescription.appendText("service field ");
					serviceMatcher.describeMismatch(service, mismatchDescription);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("activator with service field ").appendDescriptionOf(serviceMatcher);
			}
		};
	}
}

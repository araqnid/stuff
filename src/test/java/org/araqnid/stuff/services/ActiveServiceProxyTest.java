package org.araqnid.stuff.services;

import org.araqnid.stuff.services.ActiveServiceProxy;
import org.araqnid.stuff.services.ServiceActivator;
import org.hamcrest.Matchers;
import org.junit.Test;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Provider;
import com.google.inject.util.Providers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ActiveServiceProxyTest {
	@SuppressWarnings("unchecked")
	private final Provider<Service> mockServiceProvider = mock(Provider.class);
	private final TestService testService = new TestService();

	private interface TestServiceInterface {
		void someServiceMethod();
	}

	private final class TestService extends AbstractService implements TestServiceInterface {
		private final TestServiceInterface backend = mock(TestServiceInterface.class);

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

		@Override
		public void someServiceMethod() {
			backend.someServiceMethod();
		}
	}

	@Test
	public void service_proxy_can_be_obtained_immediately() {
		ServiceActivator<?> activator = new ServiceActivator<Service>(mockServiceProvider, false);
		assertThat(ActiveServiceProxy.create(activator, TestServiceInterface.class),
				Matchers.instanceOf(TestServiceInterface.class));
		assertThat(ActiveServiceProxy.create(activator, TestServiceInterface.class), Matchers.instanceOf(Service.class));
	}

	@Test(expected = ActiveServiceProxy.ServiceNotActiveException.class)
	public void initial_service_proxy_is_unusable() {
		ServiceActivator<?> activator = new ServiceActivator<Service>(mockServiceProvider, false);
		TestServiceInterface proxy = ActiveServiceProxy.create(activator, TestServiceInterface.class);
		proxy.someServiceMethod();
	}

	@Test
	public void after_activation_service_proxy_delegates_calls_to_service() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), false);
		TestServiceInterface proxy = ActiveServiceProxy.create(activator, TestServiceInterface.class);
		activator.startAsync();
		activator.activate();
		testService.finishStarting();
		proxy.someServiceMethod();
		verify(testService.backend).someServiceMethod();
	}

	@Test(expected = ActiveServiceProxy.ServiceNotActiveException.class)
	public void after_deactivation_starts_service_proxy_is_unusable() {
		ServiceActivator<?> activator = new ServiceActivator<TestService>(Providers.of(testService), false);
		TestServiceInterface proxy = ActiveServiceProxy.create(activator, TestServiceInterface.class);
		activator.startAsync();
		activator.activate();
		testService.finishStarting();
		activator.deactivate();
		proxy.someServiceMethod();
	}

	@Test
	public void service_proxies_have_usable_tostring_even_when_not_delegating() {
		ServiceActivator<?> activator = new ServiceActivator<Service>(mockServiceProvider, false);
		TestServiceInterface proxy = ActiveServiceProxy.create(activator, TestServiceInterface.class);
		assertThat(proxy.toString(), notNullValue());
	}

	@Test
	public void service_proxies_are_considered_equal() {
		ServiceActivator<?> activator = new ServiceActivator<Service>(mockServiceProvider, false);
		TestServiceInterface proxy1 = ActiveServiceProxy.create(activator, TestServiceInterface.class);
		TestServiceInterface proxy2 = ActiveServiceProxy.create(activator, TestServiceInterface.class);
		assertThat(proxy1, equalTo(proxy2));
	}
}

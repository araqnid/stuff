package org.araqnid.stuff.services;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Test;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class SpooledEventProcessorTest {
	private TestService loader = new TestService();
	private TestService processor = new TestService();
	private SpooledEventProcessor orchestrator = new SpooledEventProcessor(loader, processor);

	@Test
	public void services_are_new_before_starting() {
		TestService loader = new TestService();
		TestService processor = new TestService();
		SpooledEventProcessor service = new SpooledEventProcessor(loader, processor);
		assertThat(service, isInState(Service.State.NEW));
		assertThat(loader, isInState(Service.State.NEW));
		assertThat(processor, isInState(Service.State.NEW));
	}

	@Test
	public void starting_orchestrator_starts_loader() {
		TestService loader = new TestService();
		TestService processor = new TestService();
		SpooledEventProcessor service = new SpooledEventProcessor(loader, processor);
		service.startAsync();
		assertThat(service, isInState(Service.State.STARTING));
		assertThat(loader, isInState(Service.State.STARTING));
		assertThat(processor, isInState(Service.State.NEW));
	}

	@Test
	public void orchestrator_finishes_starting_when_loader_finishes_starting() {
		orchestrator.startAsync();
		loader.finishStarting();
		assertThat(orchestrator, isInState(Service.State.RUNNING));
		assertThat(loader, isInState(Service.State.RUNNING));
		assertThat(processor, isInState(Service.State.NEW));
	}

	@Test
	public void orchestrator_starts_processor_when_loader_stops() {
		orchestrator.startAsync();
		loader.finishStarting();
		loader.stopAsync();
		assertThat(orchestrator, isInState(Service.State.RUNNING));
		assertThat(loader, isInState(Service.State.STOPPING));
		assertThat(processor, isInState(Service.State.STARTING));
	}

	@Test
	public void orchestrator_stops_processor_when_it_is_itself_stopped() {
		orchestrator.startAsync();
		loader.finishStarting();
		loader.stopAsync();
		loader.finishStopping();
		processor.finishStarting();
		orchestrator.stopAsync();
		assertThat(orchestrator, isInState(Service.State.STOPPING));
		assertThat(loader, isInState(Service.State.TERMINATED));
		assertThat(processor, isInState(Service.State.STOPPING));
	}

	@Test
	public void orchestrator_stops_when_processor_stops() {
		orchestrator.startAsync();
		loader.finishStarting();
		loader.stopAsync();
		loader.finishStopping();
		processor.finishStarting();
		orchestrator.stopAsync();
		processor.finishStopping();
		assertThat(orchestrator, isInState(Service.State.TERMINATED));
		assertThat(loader, isInState(Service.State.TERMINATED));
		assertThat(processor, isInState(Service.State.TERMINATED));
	}

	@Test
	public void orchestrator_propagates_stop_to_loader_while_it_is_running() {
		orchestrator.startAsync();
		loader.finishStarting();
		orchestrator.stopAsync();
		assertThat(orchestrator, isInState(Service.State.STOPPING));
		assertThat(loader, isInState(Service.State.STOPPING));
		assertThat(processor, isInState(Service.State.NEW));
	}

	@Test
	public void orchestrator_terminates_without_starting_procesor_if_stopped_while_loading() {
		orchestrator.startAsync();
		loader.finishStarting();
		orchestrator.stopAsync();
		loader.finishStopping();
		assertThat(orchestrator, isInState(Service.State.TERMINATED));
		assertThat(loader, isInState(Service.State.TERMINATED));
		assertThat(processor, isInState(Service.State.NEW));
	}

	@Test
	public void orchestrator_propagates_stop_to_processor_while_it_is_starting() {
		orchestrator.startAsync();
		loader.finishStarting();
		loader.stopAsync();
		loader.finishStopping();
		orchestrator.stopAsync();
		assertThat(orchestrator, isInState(Service.State.STOPPING));
		assertThat(loader, isInState(Service.State.TERMINATED));
		assertThat(processor, isInState(Service.State.STOPPING));
	}

	@Test
	public void orchestrator_propagates_stop_to_processor_while_it_is_running() {
		orchestrator.startAsync();
		loader.finishStarting();
		loader.stopAsync();
		loader.finishStopping();
		processor.finishStarting();
		orchestrator.stopAsync();
		assertThat(orchestrator, isInState(Service.State.STOPPING));
		assertThat(loader, isInState(Service.State.TERMINATED));
		assertThat(processor, isInState(Service.State.STOPPING));
	}

	@Test
	public void if_processor_has_already_stopped_orchestrator_stops_immediately() {
		orchestrator.startAsync();
		loader.finishStarting();
		loader.stopAsync();
		loader.finishStopping();
		processor.finishStarting();
		processor.stopAsync();
		processor.finishStopping();
		orchestrator.stopAsync();
		assertThat(orchestrator, isInState(Service.State.TERMINATED));
		assertThat(loader, isInState(Service.State.TERMINATED));
		assertThat(processor, isInState(Service.State.TERMINATED));
	}

	@Test
	public void orchestrator_fails_without_starting_procesor_if_loader_fails_to_start() {
		orchestrator.startAsync();
		loader.failStarting(new RuntimeException("fail"));
		assertThat(orchestrator, isInState(Service.State.FAILED));
		assertThat(loader, isInState(Service.State.FAILED));
		assertThat(processor, isInState(Service.State.NEW));
	}

	@Test
	public void orchestrator_fails_without_starting_procesor_if_loader_fails_while_running() {
		orchestrator.startAsync();
		loader.finishStarting();
		loader.failRunning(new RuntimeException("fail"));
		assertThat(orchestrator, isInState(Service.State.FAILED));
		assertThat(loader, isInState(Service.State.FAILED));
		assertThat(processor, isInState(Service.State.NEW));
	}

	private final class TestService extends AbstractService {
		@Override
		protected void doStop() {
		}

		@Override
		protected void doStart() {
		}

		public void finishStarting() {
			assertEquals(state(), Service.State.STARTING);
			notifyStarted();
		}

		public void finishStopping() {
			assertEquals(state(), Service.State.STOPPING);
			notifyStopped();
		}

		public void failStarting(Throwable t) {
			assertEquals(state(), Service.State.STARTING);
			notifyFailed(t);
		}

		public void failRunning(Throwable t) {
			assertEquals(state(), Service.State.RUNNING);
			notifyFailed(t);
		}
	}

	private static Matcher<Service> isInState(final Service.State expected) {
		return new TypeSafeDiagnosingMatcher<Service>() {
			@Override
			protected boolean matchesSafely(Service item, Description mismatchDescription) {
				Service.State actual = item.state();
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
}

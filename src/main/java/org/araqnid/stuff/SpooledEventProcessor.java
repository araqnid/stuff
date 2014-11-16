package org.araqnid.stuff;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

// load events from spool and then start online processor
public class SpooledEventProcessor extends AbstractService {
	private final Service loader;
	private final Service processor;
	private boolean finishedLoading;

	public SpooledEventProcessor(Service loader, Service processor) {
		this.loader = loader;
		this.processor = processor;
	}

	@Override
	protected synchronized void doStart() {
		loader.addListener(new Service.Listener() {
			@Override
			public void running() {
				if (state() == State.STARTING) notifyStarted();
			}

			@Override
			public void failed(State from, Throwable failure) {
				if (from.compareTo(State.RUNNING) <= 0) notifyFailed(failure);
			}

			@Override
			public void stopping(State from) {
				loadingComplete();
			}

			@Override
			public void terminated(State from) {
				loadingComplete();
			}
		}, directExecutor());
		loader.startAsync();
	}

	@Override
	protected synchronized void doStop() {
		if (!finishedLoading) {
			loader.addListener(new Service.Listener() {
				@Override
				public void terminated(State from) {
					notifyStopped();
				}

				@Override
				public void failed(State from, Throwable failure) {
					notifyFailed(failure);
				}
			}, directExecutor());
			loader.stopAsync();
		}
		else {
			processor.addListener(new Service.Listener() {
				@Override
				public void terminated(State from) {
					notifyStopped();
				}

				@Override
				public void failed(State from, Throwable failure) {
					notifyFailed(failure);
				}
			}, directExecutor());
			processor.stopAsync();
		}
	}

	private synchronized void loadingComplete() {
		finishedLoading = true;
		if (state().compareTo(State.STOPPING) < 0) {
			processor.startAsync();
		}
	}
}

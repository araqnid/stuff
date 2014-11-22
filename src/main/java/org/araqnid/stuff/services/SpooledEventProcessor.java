package org.araqnid.stuff.services;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

// load events from spool and then start online processor
public class SpooledEventProcessor extends AbstractService {
	private final Service loader;
	private final Service processor;
	private boolean finishedLoading;
	private boolean finishedProcessing;

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
		else if (!finishedProcessing) {
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
		else {
			notifyStopped();
		}
	}

	private synchronized void loadingComplete() {
		if (finishedLoading) return;
		finishedLoading = true;
		if (state().compareTo(State.STOPPING) < 0) {
			processor.addListener(new Service.Listener() {
				@Override
				public void terminated(State from) {
					finishedProcessing = true;
				}

				@Override
				public void failed(State from, Throwable failure) {
					finishedProcessing = true;
				}
			}, directExecutor());
			processor.startAsync();
		}
	}
}

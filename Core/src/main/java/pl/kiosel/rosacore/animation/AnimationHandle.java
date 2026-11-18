package pl.kiosel.rosacore.animation;

import pl.kiosel.rosacore.scheduler.RosaTask;

import java.util.Objects;

public final class AnimationHandle {

	private final Runnable cleanup;
	private RosaTask task;
	private boolean running = true;

	AnimationHandle(Runnable cleanup) {
		this.cleanup = cleanup;
	}

	public synchronized boolean isRunning() {
		return running;
	}

	public void cancel() {
		RosaTask taskToCancel;
		synchronized (this) {
			if (!running) return;
			running = false;
			taskToCancel = task;
		}
		if (taskToCancel != null)
			taskToCancel.cancel();
		cleanup.run();
	}

	synchronized void attach(RosaTask task) {
		this.task = task;
		if (!running)
			task.cancel();
	}

	synchronized void complete() {
		if (!running) return;

		running = false;
		if (task != null)
			task.cancel();
	}
}

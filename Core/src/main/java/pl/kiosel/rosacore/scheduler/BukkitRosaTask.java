package pl.kiosel.rosacore.scheduler;

import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

final class BukkitRosaTask implements RosaTask {

	private final BukkitTask task;
	private final AtomicBoolean cancelled = new AtomicBoolean();

	BukkitRosaTask(BukkitTask task) {
		this.task = Objects.requireNonNull(task, "task");
	}

	@Override
	public void cancel() {
		if (this.cancelled.compareAndSet(false, true))
			this.task.cancel();
	}

	@Override
	public boolean isCancelled() {
		return this.cancelled.get();
	}

	@Override
	public boolean isScheduled() {
		return true;
	}
}

package pl.kiosel.rosacore.scheduler;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

final class FoliaRosaTask implements RosaTask {

	private final Object task;
	private final Set<FoliaRosaTask> registry;
	private final AtomicBoolean cancelled = new AtomicBoolean();

	FoliaRosaTask(Object task, Set<FoliaRosaTask> registry) {
		this.task = task;
		this.registry = registry;
	}

	@Override
	public void cancel() {
		if (this.cancelled.compareAndSet(false, true)) {
			try {
				ReflectionMethods.invoke(this.task, "cancel");
			} finally {
				this.registry.remove(this);
			}
		}
	}

	@Override
	public boolean isCancelled() {
		if (this.cancelled.get())
			return true;

		Object result = ReflectionMethods.invoke(this.task, "isCancelled");
		return result instanceof Boolean && (Boolean) result;
	}

	@Override
	public boolean isScheduled() {
		return true;
	}

	void completed() {
		this.registry.remove(this);
	}
}

package pl.kiosel.rosacore.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class FoliaRosaScheduler implements RosaScheduler {

	private final Plugin plugin;
	private final Object globalScheduler;
	private final Object asyncScheduler;
	private final Object regionScheduler;
	private final Set<FoliaRosaTask> tasks = Collections.newSetFromMap(new ConcurrentHashMap<>());

	public FoliaRosaScheduler(Plugin plugin) {
		this(plugin,
				ReflectionMethods.invoke(plugin.getServer(), "getGlobalRegionScheduler"),
				ReflectionMethods.invoke(plugin.getServer(), "getAsyncScheduler"),
				ReflectionMethods.invoke(plugin.getServer(), "getRegionScheduler")
		);
	}

	FoliaRosaScheduler(Plugin plugin, Object globalScheduler, Object asyncScheduler, Object regionScheduler) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.globalScheduler = Objects.requireNonNull(globalScheduler, "globalScheduler");
		this.asyncScheduler = Objects.requireNonNull(asyncScheduler, "asyncScheduler");
		this.regionScheduler = Objects.requireNonNull(regionScheduler, "regionScheduler");
	}

	@Override
	public RosaTask runGlobal(Runnable task) {
		return schedule(this.globalScheduler, "run", false, task, this.plugin);
	}

	@Override
	public RosaTask runGlobalLater(Runnable task, long delayTicks) {
		SchedulerChecks.delay(delayTicks);
		if (delayTicks == 0)
			return runGlobal(task);
		return schedule(this.globalScheduler, "runDelayed", false, task, this.plugin, delayTicks);
	}

	@Override
	public RosaTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
		return schedule(
				this.globalScheduler,
				"runAtFixedRate",
				true,
				task,
				this.plugin,
				SchedulerChecks.delay(delayTicks),
				SchedulerChecks.period(periodTicks)
		);
	}

	@Override
	public RosaTask runAsync(Runnable task) {
		return schedule(this.asyncScheduler, "runNow", false, task, this.plugin);
	}

	@Override
	public RosaTask runAsyncLater(Runnable task, long delayTicks) {
		SchedulerChecks.delay(delayTicks);
		if (delayTicks == 0)
			return runAsync(task);
		return schedule(this.asyncScheduler,
				"runDelayed",
				false,
				task,
				this.plugin,
				SchedulerChecks.ticksToMillis(delayTicks),
				TimeUnit.MILLISECONDS
		);
	}

	@Override
	public RosaTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
		return schedule(this.asyncScheduler,
				"runAtFixedRate",
				true,
				task,
				this.plugin,
				SchedulerChecks.ticksToMillis(delayTicks),
				SchedulerChecks.ticksToMillis(SchedulerChecks.period(periodTicks)),
				TimeUnit.MILLISECONDS
		);
	}

	@Override
	public RosaTask runAt(Location location, Runnable task) {
		return scheduleAt(this.regionScheduler,
				"run",
				false,
				task,
				this.plugin,
				Objects.requireNonNull(location, "location")
		);
	}

	@Override
	public RosaTask runAtLater(Location location, Runnable task, long delayTicks) {
		SchedulerChecks.delay(delayTicks);
		if (delayTicks == 0)
			return runAt(location, task);
		return scheduleAt(this.regionScheduler,
				"runDelayed",
				false,
				task,
				this.plugin,
				Objects.requireNonNull(location, "location"),
				delayTicks
		);
	}

	@Override
	public RosaTask runAtTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
		return scheduleAt(this.regionScheduler,
				"runAtFixedRate",
				true,
				task,
				this.plugin,
				Objects.requireNonNull(location, "location"),
				SchedulerChecks.delay(delayTicks),
				SchedulerChecks.period(periodTicks)
		);
	}

	@Override
	public RosaTask runForEntity(Entity entity, Runnable task, Runnable retired) {
		return schedule(entityScheduler(entity),
				"run",
				false,
				task,
				this.plugin,
				retired
		);
	}

	@Override
	public RosaTask runForEntityLater(Entity entity, Runnable task, Runnable retired, long delayTicks) {
		SchedulerChecks.delay(delayTicks);
		if (delayTicks == 0)
			return runForEntity(entity, task, retired);
		return schedule(entityScheduler(entity),
				"runDelayed",
				false,
				task,
				this.plugin,
				retired,
				delayTicks
		);
	}

	@Override
	public RosaTask runForEntityTimer(Entity entity, Runnable task, Runnable retired, long delayTicks, long periodTicks) {
		return schedule(entityScheduler(entity),
				"runAtFixedRate",
				true,
				task,
				this.plugin,
				retired,
				SchedulerChecks.delay(delayTicks),
				SchedulerChecks.period(periodTicks)
		);
	}

	@Override
	public void cancelAll() {
		for (FoliaRosaTask task : this.tasks.toArray(new FoliaRosaTask[0]))
			task.cancel();
		this.tasks.clear();
	}

	@Override
	public boolean isFolia() {
		return true;
	}

	private Object entityScheduler(Entity entity) {
		return ReflectionMethods.invoke(Objects.requireNonNull(entity, "entity"), "getScheduler");
	}

	private RosaTask schedule(Object scheduler, String method, boolean repeating, Runnable runnable, Object... fixedArguments) {
		return schedule(scheduler, method, repeating, runnable, 1, fixedArguments);
	}

	private RosaTask scheduleAt(Object scheduler, String method, boolean repeating, Runnable runnable, Object... fixedArguments) {
		return schedule(scheduler, method, repeating, runnable, 2, fixedArguments);
	}

	private RosaTask schedule(Object scheduler, String method, boolean repeating, Runnable runnable, int callbackIndex, Object[] fixedArguments) {
		SchedulerChecks.task(runnable);

		AtomicReference<FoliaRosaTask> reference = new AtomicReference<>();
		Consumer<Object> callback = ignored -> {
			try {
				runnable.run();
			} finally {
				if (!repeating) {
					FoliaRosaTask completed = reference.get();
					if (completed != null)
						completed.completed();
				}
			}
		};

		Object[] arguments = insertCallback(fixedArguments, callbackIndex, callback);
		Object rawTask = ReflectionMethods.invoke(scheduler, method, arguments);
		if (rawTask == null)
			return RejectedRosaTask.INSTANCE;

		FoliaRosaTask task = new FoliaRosaTask(rawTask, this.tasks);
		reference.set(task);
		this.tasks.add(task);
		return task;
	}

	private static Object[] insertCallback(Object[] fixedArguments, int callbackIndex, Consumer<Object> callback) {
		if (callbackIndex < 0 || callbackIndex > fixedArguments.length)
			throw new IllegalArgumentException("Invalid callback position: " + callbackIndex);

		Object[] arguments = new Object[fixedArguments.length + 1];
		System.arraycopy(fixedArguments, 0, arguments, 0, callbackIndex);
		arguments[callbackIndex] = callback;
		System.arraycopy(
				fixedArguments,
				callbackIndex,
				arguments,
				callbackIndex + 1,
				fixedArguments.length - callbackIndex
		);
		return arguments;
	}
}

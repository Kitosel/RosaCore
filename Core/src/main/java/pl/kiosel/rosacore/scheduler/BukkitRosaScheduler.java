package pl.kiosel.rosacore.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public final class BukkitRosaScheduler implements RosaScheduler {

	private final Plugin plugin;
	private final BukkitScheduler scheduler;

	public BukkitRosaScheduler(Plugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.scheduler = plugin.getServer().getScheduler();
	}

	@Override
	public RosaTask runGlobal(Runnable task) {
		return wrap(this.scheduler.runTask(this.plugin, SchedulerChecks.task(task)));
	}

	@Override
	public RosaTask runGlobalLater(Runnable task, long delayTicks) {
		SchedulerChecks.delay(delayTicks);
		if (delayTicks == 0)
			return runGlobal(task);
		return wrap(this.scheduler.runTaskLater(this.plugin, SchedulerChecks.task(task), delayTicks));
	}

	@Override
	public RosaTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks) {
		return wrap(this.scheduler.runTaskTimer(
				this.plugin,
				SchedulerChecks.task(task),
				SchedulerChecks.delay(delayTicks),
				SchedulerChecks.period(periodTicks)
		));
	}

	@Override
	public RosaTask runAsync(Runnable task) {
		return wrap(this.scheduler.runTaskAsynchronously(this.plugin, SchedulerChecks.task(task)));
	}

	@Override
	public RosaTask runAsyncLater(Runnable task, long delayTicks) {
		SchedulerChecks.delay(delayTicks);
		if (delayTicks == 0)
			return runAsync(task);
		return wrap(this.scheduler.runTaskLaterAsynchronously(
				this.plugin,
				SchedulerChecks.task(task),
				delayTicks
		));
	}

	@Override
	public RosaTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
		return wrap(this.scheduler.runTaskTimerAsynchronously(
				this.plugin,
				SchedulerChecks.task(task),
				SchedulerChecks.delay(delayTicks),
				SchedulerChecks.period(periodTicks)
		));
	}

	@Override
	public RosaTask runAt(Location location, Runnable task) {
		Objects.requireNonNull(location, "location");
		return runGlobal(task);
	}

	@Override
	public RosaTask runAtLater(Location location, Runnable task, long delayTicks) {
		Objects.requireNonNull(location, "location");
		return runGlobalLater(task, delayTicks);
	}

	@Override
	public RosaTask runAtTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
		Objects.requireNonNull(location, "location");
		return runGlobalTimer(task, delayTicks, periodTicks);
	}

	@Override
	public RosaTask runForEntity(Entity entity, Runnable task, Runnable retired) {
		Objects.requireNonNull(entity, "entity");
		return runGlobal(task);
	}

	@Override
	public RosaTask runForEntityLater(Entity entity, Runnable task, Runnable retired, long delayTicks) {
		Objects.requireNonNull(entity, "entity");
		return runGlobalLater(task, delayTicks);
	}

	@Override
	public RosaTask runForEntityTimer(Entity entity, Runnable task, Runnable retired, long delayTicks, long periodTicks) {
		Objects.requireNonNull(entity, "entity");
		return runGlobalTimer(task, delayTicks, periodTicks);
	}

	@Override
	public void cancelAll() {
		Bukkit.getScheduler().cancelTasks(this.plugin);
	}

	@Override
	public boolean isFolia() {
		return false;
	}

	private static RosaTask wrap(BukkitTask task) {
		return new BukkitRosaTask(task);
	}
}

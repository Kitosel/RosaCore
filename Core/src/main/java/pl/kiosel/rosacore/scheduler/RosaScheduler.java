package pl.kiosel.rosacore.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface RosaScheduler {

	RosaTask runGlobal(Runnable task);

	RosaTask runGlobalLater(Runnable task, long delayTicks);

	RosaTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks);

	RosaTask runAsync(Runnable task);

	RosaTask runAsyncLater(Runnable task, long delayTicks);

	RosaTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks);

	RosaTask runAt(Location location, Runnable task);

	RosaTask runAtLater(Location location, Runnable task, long delayTicks);

	RosaTask runAtTimer(Location location, Runnable task, long delayTicks, long periodTicks);

	RosaTask runForEntity(Entity entity, Runnable task, Runnable retired);

	RosaTask runForEntityLater(Entity entity, Runnable task, Runnable retired, long delayTicks);

	RosaTask runForEntityTimer(
			Entity entity,
			Runnable task,
			Runnable retired,
			long delayTicks,
			long periodTicks
	);

	void cancelAll();

	boolean isFolia();
}

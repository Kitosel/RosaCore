package pl.kiosel.rosacore.scheduler;

import java.util.Objects;

final class SchedulerChecks {

	private SchedulerChecks() {
	}

	static Runnable task(Runnable task) {
		return Objects.requireNonNull(task, "task");
	}

	static long delay(long delayTicks) {
		if (delayTicks < 0)
			throw new IllegalArgumentException("Delay cannot be negative");
		return delayTicks;
	}

	static long period(long periodTicks) {
		if (periodTicks <= 0)
			throw new IllegalArgumentException("Period must be greater than zero");
		return periodTicks;
	}

	static long ticksToMillis(long ticks) {
		delay(ticks);
		try {
			return Math.multiplyExact(ticks, 50L);
		} catch (ArithmeticException exception) {
			throw new IllegalArgumentException("Tick duration is too large", exception);
		}
	}
}

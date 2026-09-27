package pl.kiosel.rosacore.cooldown;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import pl.kiosel.rosacore.scheduler.RosaScheduler;
import pl.kiosel.rosacore.scheduler.RosaTask;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

public final class RosaCooldowns implements AutoCloseable {

	private static final long CLEANUP_INTERVAL_TICKS = 20L * 60L;
	private static final long NANOS_PER_MILLISECOND = 1_000_000L;

	private final Map<CooldownKey, Long> expirations = new ConcurrentHashMap<>();
	private final LongSupplier nanoTime;
	private final RosaTask cleanupTask;
	@Getter
	private volatile boolean closed;

	public RosaCooldowns(RosaScheduler scheduler) {
		this(scheduler, System::nanoTime);
	}

	RosaCooldowns(RosaScheduler scheduler, LongSupplier nanoTime) {
		this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
		this.cleanupTask = Objects.requireNonNull(scheduler, "scheduler")
				.runAsyncTimer(this::cleanup, CLEANUP_INTERVAL_TICKS, CLEANUP_INTERVAL_TICKS);
	}

	RosaCooldowns(LongSupplier nanoTime) {
		this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
		this.cleanupTask = null;
	}

	public boolean tryUse(UUID owner, String name, long seconds) {
		return tryUseNanos(key(owner, name), secondsToNanos(seconds));
	}

	public boolean tryUse(Player player, String name, long seconds) {
		Objects.requireNonNull(player, "player");
		return tryUse(player.getUniqueId(), name, seconds);
	}

	public boolean tryUse(Player player, String name, long seconds, String bypassPermission) {
		Objects.requireNonNull(player, "player");
		if (canBypass(player, bypassPermission)) return true;
		return tryUse(player.getUniqueId(), name, seconds);
	}

	public boolean tryUseTicks(UUID owner, String name, long ticks) {
		return tryUseNanos(key(owner, name), ticksToNanos(ticks));
	}

	public boolean tryUseTicks(Player player, String name, long ticks) {
		Objects.requireNonNull(player, "player");
		return tryUseTicks(player.getUniqueId(), name, ticks);
	}

	public boolean tryUseTicks(Player player, String name, long ticks, String bypassPermission) {
		Objects.requireNonNull(player, "player");
		if (canBypass(player, bypassPermission)) return true;
		return tryUseTicks(player.getUniqueId(), name, ticks);
	}

	public void start(UUID owner, String name, long seconds) {
		startNanos(key(owner, name), secondsToNanos(seconds));
	}

	public void start(Player player, String name, long seconds) {
		Objects.requireNonNull(player, "player");
		start(player.getUniqueId(), name, seconds);
	}

	public void startTicks(UUID owner, String name, long ticks) {
		startNanos(key(owner, name), ticksToNanos(ticks));
	}

	public void startTicks(Player player, String name, long ticks) {
		Objects.requireNonNull(player, "player");
		startTicks(player.getUniqueId(), name, ticks);
	}

	public boolean isActive(UUID owner, String name) {
		return remainingNanos(key(owner, name)) > 0L;
	}

	public boolean isActive(Player player, String name) {
		Objects.requireNonNull(player, "player");
		return isActive(player.getUniqueId(), name);
	}

	public long getRemainingMillis(UUID owner, String name) {
		return divideRoundedUp(remainingNanos(key(owner, name)), NANOS_PER_MILLISECOND);
	}

	public long getRemainingMillis(Player player, String name) {
		Objects.requireNonNull(player, "player");
		return getRemainingMillis(player.getUniqueId(), name);
	}

	public long getRemainingSeconds(UUID owner, String name) {
		return divideRoundedUp(remainingNanos(key(owner, name)), TimeUnit.SECONDS.toNanos(1L));
	}

	public long getRemainingSeconds(Player player, String name) {
		Objects.requireNonNull(player, "player");
		return getRemainingSeconds(player.getUniqueId(), name);
	}

	public long getRemainingTicks(UUID owner, String name) {
		return divideRoundedUp(remainingNanos(key(owner, name)), TimeUnit.MILLISECONDS.toNanos(50L));
	}

	public long getRemainingTicks(Player player, String name) {
		Objects.requireNonNull(player, "player");
		return getRemainingTicks(player.getUniqueId(), name);
	}

	public boolean reset(UUID owner, String name) {
		ensureOpen();
		return this.expirations.remove(key(owner, name)) != null;
	}

	public boolean reset(Player player, String name) {
		Objects.requireNonNull(player, "player");
		return reset(player.getUniqueId(), name);
	}

	public int resetAll(UUID owner) {
		ensureOpen();
		Objects.requireNonNull(owner, "owner");
		int removed = 0;
		for (CooldownKey key : this.expirations.keySet()) {
			if (key.owner.equals(owner) && this.expirations.remove(key) != null) removed++;
		}
		return removed;
	}

	public int resetAll(Player player) {
		Objects.requireNonNull(player, "player");
		return resetAll(player.getUniqueId());
	}

	public int clear() {
		ensureOpen();
		int size = this.expirations.size();
		this.expirations.clear();
		return size;
	}

	public int cleanup() {
		if (this.closed) return 0;
		long now = this.nanoTime.getAsLong();
		int removed = 0;
		for (Map.Entry<CooldownKey, Long> entry : this.expirations.entrySet()) {
			if (entry.getValue() <= now
					&& this.expirations.remove(entry.getKey(), entry.getValue())) {
				removed++;
			}
		}
		return removed;
	}

	public int size() {
		ensureOpen();
		cleanup();
		return this.expirations.size();
	}

	@Override
	public void close() {
		if (this.closed) return;
		this.closed = true;
		if (this.cleanupTask != null) this.cleanupTask.cancel();
		this.expirations.clear();
	}

	private boolean tryUseNanos(CooldownKey key, long durationNanos) {
		ensureOpen();
		if (durationNanos == 0L) {
			this.expirations.remove(key);
			return true;
		}

		while (true) {
			long now = this.nanoTime.getAsLong();
			Long current = this.expirations.get(key);
			if (current != null && current > now) return false;
			long expiration = saturatingAdd(now, durationNanos);
			if (current == null) {
				if (this.expirations.putIfAbsent(key, expiration) == null) return true;
			} else if (this.expirations.replace(key, current, expiration)) {
				return true;
			}
		}
	}

	private void startNanos(CooldownKey key, long durationNanos) {
		ensureOpen();
		if (durationNanos == 0L) {
			this.expirations.remove(key);
			return;
		}
		this.expirations.put(key, saturatingAdd(this.nanoTime.getAsLong(), durationNanos));
	}

	private long remainingNanos(CooldownKey key) {
		ensureOpen();
		Long expiration = this.expirations.get(key);
		if (expiration == null) return 0L;
		long remaining = expiration - this.nanoTime.getAsLong();
		if (remaining > 0L) return remaining;
		this.expirations.remove(key, expiration);
		return 0L;
	}

	private void ensureOpen() {
		if (this.closed) throw new IllegalStateException("Cooldown manager is closed");
	}

	static boolean canBypass(Permissible permissible, String permission) {
		return permission != null && !permission.trim().isEmpty() && permissible.hasPermission(permission);
	}

	private static CooldownKey key(UUID owner, String name) {
		Objects.requireNonNull(owner, "owner");
		Objects.requireNonNull(name, "name");
		String checkedName = name.trim();
		if (checkedName.isEmpty()) throw new IllegalArgumentException("Cooldown name cannot be empty");
		return new CooldownKey(owner, checkedName);
	}

	private static long secondsToNanos(long seconds) {
		if (seconds < 0L) throw new IllegalArgumentException("Cooldown seconds cannot be negative");
		return TimeUnit.SECONDS.toNanos(seconds);
	}

	private static long ticksToNanos(long ticks) {
		if (ticks < 0L) throw new IllegalArgumentException("Cooldown ticks cannot be negative");
		return TimeUnit.MILLISECONDS.toNanos(saturatingMultiply(ticks, 50L));
	}

	private static long divideRoundedUp(long value, long divisor) {
		if (value <= 0L) return 0L;
		return 1L + ((value - 1L) / divisor);
	}

	private static long saturatingAdd(long left, long right) {
		if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
		return left + right;
	}

	private static long saturatingMultiply(long left, long right) {
		if (left == 0L || right == 0L) return 0L;
		if (left > Long.MAX_VALUE / right) return Long.MAX_VALUE;
		return left * right;
	}

	private static final class CooldownKey {

		private final UUID owner;
		private final String name;

		private CooldownKey(UUID owner, String name) {
			this.owner = owner;
			this.name = name;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) return true;
			if (!(object instanceof CooldownKey)) return false;
			CooldownKey that = (CooldownKey) object;
			return this.owner.equals(that.owner) && this.name.equals(that.name);
		}

		@Override
		public int hashCode() {
			return 31 * this.owner.hashCode() + this.name.hashCode();
		}
	}
}

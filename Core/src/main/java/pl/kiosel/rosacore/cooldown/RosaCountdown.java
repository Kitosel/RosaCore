package pl.kiosel.rosacore.cooldown;

import org.bukkit.entity.Player;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.message.MessagePlaceholders;
import pl.kiosel.rosacore.message.RosaBossBar;
import pl.kiosel.rosacore.scheduler.RosaScheduler;
import pl.kiosel.rosacore.scheduler.RosaTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntConsumer;

public final class RosaCountdown implements AutoCloseable {

	private final RosaPlugin plugin;
	private final RosaScheduler scheduler;
	private final Function<Runnable, RosaTask> timerFactory;
	private final int totalSeconds;
	private final List<IntConsumer> tickListeners = new ArrayList<>();
	private final List<Runnable> finishListeners = new ArrayList<>();
	private final List<Runnable> cancelListeners = new ArrayList<>();
	private final List<BossBarDisplay> bossBarDisplays = new ArrayList<>();

	private int remainingSeconds;
	private State state = State.READY;
	private RosaTask task;

	public RosaCountdown(RosaPlugin plugin, int seconds) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.scheduler = plugin.getRosaScheduler();
		this.timerFactory = repeatingTimer(this.scheduler);
		this.totalSeconds = requireSeconds(seconds);
		this.remainingSeconds = seconds;
	}

	public RosaCountdown(RosaScheduler scheduler, int seconds) {
		this.plugin = null;
		this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
		this.timerFactory = repeatingTimer(this.scheduler);
		this.totalSeconds = requireSeconds(seconds);
		this.remainingSeconds = seconds;
	}

	RosaCountdown(int seconds, Function<Runnable, RosaTask> timerFactory) {
		this.plugin = null;
		this.scheduler = null;
		this.timerFactory = Objects.requireNonNull(timerFactory, "timerFactory");
		this.totalSeconds = requireSeconds(seconds);
		this.remainingSeconds = seconds;
	}

	public synchronized RosaCountdown onTick(IntConsumer listener) {
		ensureReady();
		this.tickListeners.add(Objects.requireNonNull(listener, "listener"));
		return this;
	}

	public RosaCountdown onTick(Player player, IntConsumer listener) {
		Player checkedPlayer = Objects.requireNonNull(player, "player");
		IntConsumer checkedListener = Objects.requireNonNull(listener, "listener");
		return onTick(seconds -> dispatch(
				checkedPlayer, () -> checkedListener.accept(seconds), true));
	}

	public synchronized RosaCountdown onFinish(Runnable listener) {
		ensureReady();
		this.finishListeners.add(Objects.requireNonNull(listener, "listener"));
		return this;
	}

	public RosaCountdown onFinish(Player player, Runnable listener) {
		Player checkedPlayer = Objects.requireNonNull(player, "player");
		Runnable checkedListener = Objects.requireNonNull(listener, "listener");
		return onFinish(() -> dispatch(checkedPlayer, checkedListener, true));
	}

	public synchronized RosaCountdown onCancel(Runnable listener) {
		ensureReady();
		this.cancelListeners.add(Objects.requireNonNull(listener, "listener"));
		return this;
	}

	public RosaCountdown onCancel(Player player, Runnable listener) {
		Player checkedPlayer = Objects.requireNonNull(player, "player");
		Runnable checkedListener = Objects.requireNonNull(listener, "listener");
		return onCancel(() -> dispatch(checkedPlayer, checkedListener, true));
	}

	public RosaCountdown actionBar(Player player, String message, Object... placeholders) {
		Player checkedPlayer = Objects.requireNonNull(player, "player");
		String checkedMessage = Objects.requireNonNull(message, "message");
		Object[] fixedPlaceholders = copyPlaceholders(placeholders);
		requirePlugin();
		return onTick(seconds -> dispatch(checkedPlayer, () -> plugin.getMessenger().actionBar(
				checkedPlayer,
				checkedMessage,
				countdownPlaceholders(seconds, fixedPlaceholders)
		), true));
	}

	public RosaCountdown title(Player player, String title, String subtitle, Object... placeholders) {
		Player checkedPlayer = Objects.requireNonNull(player, "player");
		String checkedTitle = Objects.requireNonNull(title, "title");
		String checkedSubtitle = Objects.requireNonNull(subtitle, "subtitle");
		Object[] fixedPlaceholders = copyPlaceholders(placeholders);
		requirePlugin();
		return onTick(seconds -> dispatch(checkedPlayer, () -> plugin.getMessenger().title(
				checkedPlayer,
				checkedTitle,
				checkedSubtitle,
				0, 25, 0,
				countdownPlaceholders(seconds, fixedPlaceholders)
		), true));
	}

	public RosaCountdown bossBar(Player player, String message, Object... placeholders) {
		return bossBar(player, message, RosaBossBar.Color.PURPLE, RosaBossBar.Style.SOLID,
				placeholders);
	}

	public synchronized RosaCountdown bossBar(Player player, String message,
											  RosaBossBar.Color color, RosaBossBar.Style style,
											  Object... placeholders) {
		ensureReady();
		requirePlugin();
		BossBarDisplay display = new BossBarDisplay(
				Objects.requireNonNull(player, "player"),
				Objects.requireNonNull(message, "message"),
				Objects.requireNonNull(color, "color"),
				Objects.requireNonNull(style, "style"),
				copyPlaceholders(placeholders)
		);
		this.bossBarDisplays.add(display);
		this.tickListeners.add(display::tick);
		return this;
	}

	public RosaCountdown start() {
		synchronized (this) {
			ensureReady();
			this.state = State.RUNNING;
		}

		if (this.totalSeconds == 0) {
			finish();
			return this;
		}

		try {
			notifyTick(this.totalSeconds);
		} catch (RuntimeException | Error failure) {
			cancelAfterFailure(failure);
			throw failure;
		}

		synchronized (this) {
			if (this.state != State.RUNNING) {
				return this;
			}
		}

		RosaTask scheduled;
		try {
			scheduled = Objects.requireNonNull(this.timerFactory.apply(this::advance), "scheduled task");
		} catch (RuntimeException | Error failure) {
			cancelAfterFailure(failure);
			throw failure;
		}

		boolean rejected;
		synchronized (this) {
			rejected = !scheduled.isScheduled();
			if (!rejected && this.state == State.RUNNING) {
				this.task = scheduled;
				return this;
			}
		}

		scheduled.cancel();
		if (rejected) {
			cancel();
		}
		return this;
	}

	public boolean cancel() {
		RosaTask scheduled;
		List<Runnable> listeners;
		synchronized (this) {
			if (this.state == State.FINISHED || this.state == State.CANCELLED) {
				return false;
			}
			this.state = State.CANCELLED;
			scheduled = this.task;
			this.task = null;
			listeners = new ArrayList<>(this.cancelListeners);
		}

		if (scheduled != null) {
			scheduled.cancel();
		}
		closeBossBars();
		runListeners(listeners);
		return true;
	}

	@Override
	public void close() {
		cancel();
	}

	public synchronized int getTotalSeconds() {
		return this.totalSeconds;
	}

	public synchronized int getRemainingSeconds() {
		return this.remainingSeconds;
	}

	public synchronized int getElapsedSeconds() {
		return this.totalSeconds - this.remainingSeconds;
	}

	public synchronized float getProgress() {
		return this.totalSeconds == 0
				? 0.0F
				: this.remainingSeconds / (float) this.totalSeconds;
	}

	public synchronized State getState() {
		return this.state;
	}

	public synchronized boolean isRunning() {
		return this.state == State.RUNNING;
	}

	public synchronized boolean isFinished() {
		return this.state == State.FINISHED;
	}

	public synchronized boolean isCancelled() {
		return this.state == State.CANCELLED;
	}

	public static String formatTime(int seconds) {
		requireSeconds(seconds);
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		int remaining = seconds % 60;
		if (hours > 0) {
			return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remaining);
		}
		return String.format(Locale.ROOT, "%d:%02d", minutes, remaining);
	}

	private void advance() {
		int current;
		synchronized (this) {
			if (this.state != State.RUNNING) {
				return;
			}
			this.remainingSeconds--;
			current = this.remainingSeconds;
		}

		if (current == 0) {
			finish();
			return;
		}

		try {
			notifyTick(current);
		} catch (RuntimeException | Error failure) {
			cancelAfterFailure(failure);
			throw failure;
		}
	}

	private void notifyTick(int seconds) {
		List<IntConsumer> listeners;
		synchronized (this) {
			if (this.state != State.RUNNING) {
				return;
			}
			listeners = new ArrayList<>(this.tickListeners);
		}
		for (IntConsumer listener : listeners) {
			listener.accept(seconds);
			synchronized (this) {
				if (this.state != State.RUNNING) {
					return;
				}
			}
		}
	}

	private void finish() {
		RosaTask scheduled;
		List<Runnable> listeners;
		synchronized (this) {
			if (this.state != State.RUNNING) {
				return;
			}
			this.state = State.FINISHED;
			this.remainingSeconds = 0;
			scheduled = this.task;
			this.task = null;
			listeners = new ArrayList<>(this.finishListeners);
		}

		if (scheduled != null) {
			scheduled.cancel();
		}
		closeBossBars();
		runListeners(listeners);
	}

	private void cancelAfterFailure(Throwable original) {
		try {
			cancel();
		} catch (RuntimeException | Error cleanupFailure) {
			original.addSuppressed(cleanupFailure);
		}
	}

	private void closeBossBars() {
		List<BossBarDisplay> displays;
		synchronized (this) {
			displays = new ArrayList<>(this.bossBarDisplays);
			this.bossBarDisplays.clear();
		}
		RuntimeException failure = null;
		for (BossBarDisplay display : displays) {
			try {
				display.close();
			} catch (RuntimeException exception) {
				if (failure == null) failure = exception;
				else failure.addSuppressed(exception);
			}
		}
		if (failure != null) throw failure;
	}

	private void dispatch(Player player, Runnable action, boolean requireOnline) {
		if (requireOnline && !player.isOnline()) {
			return;
		}
		if (this.scheduler != null && this.scheduler.isFolia()) {
			this.scheduler.runForEntity(player, action, requireOnline ? () -> {
			} : action);
			return;
		}
		action.run();
	}

	private Object[] countdownPlaceholders(int seconds, Object[] fixed) {
		Object[] values = new Object[fixed.length + 6];
		System.arraycopy(fixed, 0, values, 0, fixed.length);
		int index = fixed.length;
		values[index] = "seconds";
		values[index + 1] = seconds;
		values[index + 2] = "time";
		values[index + 3] = formatTime(seconds);
		values[index + 4] = "total";
		values[index + 5] = this.totalSeconds;
		return values;
	}

	private static Object[] copyPlaceholders(Object[] placeholders) {
		Object[] copy = placeholders == null ? new Object[0] : placeholders.clone();
		MessagePlaceholders.pairs(copy);
		return copy;
	}

	private RosaPlugin requirePlugin() {
		if (this.plugin == null) {
			throw new IllegalStateException(
					"This countdown was created without a RosaPlugin and cannot display messages");
		}
		return this.plugin;
	}

	private synchronized void ensureReady() {
		if (this.state != State.READY) {
			throw new IllegalStateException("Countdown has already been started or cancelled");
		}
	}

	private static int requireSeconds(int seconds) {
		if (seconds < 0) {
			throw new IllegalArgumentException("Countdown seconds cannot be negative");
		}
		return seconds;
	}

	private static Function<Runnable, RosaTask> repeatingTimer(RosaScheduler scheduler) {
		return runnable -> scheduler.runGlobalTimer(runnable, 20L, 20L);
	}

	private static void runListeners(List<Runnable> listeners) {
		RuntimeException failure = null;
		for (Runnable listener : listeners) {
			try {
				listener.run();
			} catch (RuntimeException exception) {
				if (failure == null) failure = exception;
				else failure.addSuppressed(exception);
			}
		}
		if (failure != null) throw failure;
	}

	public enum State {
		READY,
		RUNNING,
		FINISHED,
		CANCELLED
	}

	private final class BossBarDisplay {

		private final Player player;
		private final String message;
		private final RosaBossBar.Color color;
		private final RosaBossBar.Style style;
		private final Object[] placeholders;
		private RosaBossBar bar;
		private boolean closed;

		private BossBarDisplay(Player player, String message, RosaBossBar.Color color,
							   RosaBossBar.Style style, Object[] placeholders) {
			this.player = player;
			this.message = message;
			this.color = color;
			this.style = style;
			this.placeholders = placeholders;
		}

		private void tick(int seconds) {
			dispatch(this.player, () -> update(seconds), true);
		}

		private synchronized void update(int seconds) {
			if (this.closed) return;
			float progress = seconds / (float) totalSeconds;
			Object[] values = countdownPlaceholders(seconds, this.placeholders);
			if (this.bar == null) {
				this.bar = plugin.getBossBars().create(
						this.message, progress, this.color, this.style, values).show(this.player);
				return;
			}
			this.bar.setText(this.message, values).setProgress(progress);
		}

		private void close() {
			RosaBossBar current;
			synchronized (this) {
				if (this.closed) return;
				this.closed = true;
				current = this.bar;
				this.bar = null;
			}
			if (current != null) {
				dispatch(this.player, current::close, false);
			}
		}
	}
}

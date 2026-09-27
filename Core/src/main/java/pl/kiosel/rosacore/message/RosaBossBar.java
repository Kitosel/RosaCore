package pl.kiosel.rosacore.message;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;

import java.util.*;

public final class RosaBossBar implements AutoCloseable {

	private final RosaBossBarManager owner;
	private final RosaMessenger messenger;
	private final BossBar handle;
	private final Map<UUID, Audience> viewers = new LinkedHashMap<>();

	private String text;
	private float progress;
	private Color color;
	private Style style;
	private boolean closed;

	RosaBossBar(RosaBossBarManager owner, RosaMessenger messenger, String text, float progress,
				Color color, Style style, Object... placeholders) {
		this.owner = Objects.requireNonNull(owner, "owner");
		this.messenger = Objects.requireNonNull(messenger, "messenger");
		this.text = Objects.requireNonNull(text, "text");
		this.progress = validateProgress(progress);
		this.color = Objects.requireNonNull(color, "color");
		this.style = Objects.requireNonNull(style, "style");
		this.handle = BossBar.bossBar(
				this.messenger.component(text, placeholders),
				this.progress,
				this.color.adventure(),
				this.style.adventure()
		);
	}

	public synchronized RosaBossBar show(Player player) {
		ensureOpen();
		Objects.requireNonNull(player, "player");
		Audience audience = this.messenger.playerAudience(player);
		audience.showBossBar(this.handle);
		Audience previous = this.viewers.put(player.getUniqueId(), audience);
		if (previous != null && previous != audience) previous.hideBossBar(this.handle);
		return this;
	}

	public synchronized RosaBossBar show(Player... players) {
		Objects.requireNonNull(players, "players");
		for (Player player : players) show(player);
		return this;
	}

	public synchronized RosaBossBar hide(Player player) {
		ensureOpen();
		Objects.requireNonNull(player, "player");
		Audience audience = this.viewers.remove(player.getUniqueId());
		if (audience != null) audience.hideBossBar(this.handle);
		return this;
	}

	public synchronized RosaBossBar hideAll() {
		ensureOpen();
		hideAllInternal();
		return this;
	}

	public synchronized boolean isShown(Player player) {
		ensureOpen();
		return this.viewers.containsKey(Objects.requireNonNull(player, "player").getUniqueId());
	}

	public synchronized Set<UUID> getViewerIds() {
		ensureOpen();
		return Collections.unmodifiableSet(new LinkedHashSet<>(this.viewers.keySet()));
	}

	public synchronized RosaBossBar setText(String text, Object... placeholders) {
		ensureOpen();
		this.handle.name(this.messenger.component(text, placeholders));
		this.text = Objects.requireNonNull(text, "text");
		return this;
	}

	public synchronized String getText() {
		return this.text;
	}

	public synchronized RosaBossBar setProgress(float progress) {
		ensureOpen();
		float checked = validateProgress(progress);
		this.handle.progress(checked);
		this.progress = checked;
		return this;
	}

	public synchronized float getProgress() {
		return this.progress;
	}

	public synchronized RosaBossBar setColor(Color color) {
		ensureOpen();
		Color checked = Objects.requireNonNull(color, "color");
		this.handle.color(checked.adventure());
		this.color = checked;
		return this;
	}

	public synchronized Color getColor() {
		return this.color;
	}

	public synchronized RosaBossBar setStyle(Style style) {
		ensureOpen();
		Style checked = Objects.requireNonNull(style, "style");
		this.handle.overlay(checked.adventure());
		this.style = checked;
		return this;
	}

	public synchronized Style getStyle() {
		return this.style;
	}

	public synchronized RosaBossBar addFlag(Flag flag) {
		ensureOpen();
		this.handle.addFlag(Objects.requireNonNull(flag, "flag").adventure());
		return this;
	}

	public synchronized RosaBossBar removeFlag(Flag flag) {
		ensureOpen();
		this.handle.removeFlag(Objects.requireNonNull(flag, "flag").adventure());
		return this;
	}

	public synchronized RosaBossBar clearFlags() {
		ensureOpen();
		this.handle.flags(Collections.emptySet());
		return this;
	}

	public synchronized boolean hasFlag(Flag flag) {
		ensureOpen();
		return this.handle.hasFlag(Objects.requireNonNull(flag, "flag").adventure());
	}

	public synchronized Set<Flag> getFlags() {
		ensureOpen();
		Set<Flag> flags = EnumSet.noneOf(Flag.class);
		for (BossBar.Flag flag : this.handle.flags()) flags.add(Flag.valueOf(flag.name()));
		return Collections.unmodifiableSet(flags);
	}

	public synchronized boolean isClosed() {
		return this.closed;
	}

	@Override
	public synchronized void close() {
		if (this.closed) return;
		this.closed = true;
		try {
			hideAllInternal();
		} finally {
			this.owner.forget(this);
		}
	}

	private void hideAllInternal() {
		RuntimeException failure = null;
		for (Audience audience : this.viewers.values()) {
			try {
				audience.hideBossBar(this.handle);
			} catch (RuntimeException exception) {
				if (failure == null) failure = exception;
				else failure.addSuppressed(exception);
			}
		}
		this.viewers.clear();
		if (failure != null) throw failure;
	}

	private void ensureOpen() {
		if (this.closed) throw new IllegalStateException("Boss bar is closed");
	}

	static float validateProgress(float progress) {
		if (Float.isNaN(progress) || progress < 0.0F || progress > 1.0F) {
			throw new IllegalArgumentException("Boss bar progress must be between 0.0 and 1.0: " + progress);
		}
		return progress;
	}

	public enum Color {
		PINK,
		BLUE,
		RED,
		GREEN,
		YELLOW,
		PURPLE,
		WHITE;

		private BossBar.Color adventure() {
			return BossBar.Color.valueOf(name());
		}
	}

	public enum Style {
		SOLID,
		SEGMENTED_6,
		SEGMENTED_10,
		SEGMENTED_12,
		SEGMENTED_20;

		private BossBar.Overlay adventure() {
			switch (this) {
				case SOLID:
					return BossBar.Overlay.PROGRESS;
				case SEGMENTED_6:
					return BossBar.Overlay.NOTCHED_6;
				case SEGMENTED_10:
					return BossBar.Overlay.NOTCHED_10;
				case SEGMENTED_12:
					return BossBar.Overlay.NOTCHED_12;
				case SEGMENTED_20:
					return BossBar.Overlay.NOTCHED_20;
				default:
					throw new IllegalStateException("Unknown boss bar style: " + this);
			}
		}
	}

	public enum Flag {
		DARKEN_SCREEN,
		PLAY_BOSS_MUSIC,
		CREATE_WORLD_FOG;

		private BossBar.Flag adventure() {
			return BossBar.Flag.valueOf(name());
		}
	}
}

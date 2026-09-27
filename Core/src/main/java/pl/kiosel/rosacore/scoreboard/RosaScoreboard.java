package pl.kiosel.rosacore.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.message.LegacyColorizer;
import pl.kiosel.rosacore.message.MessagePlaceholders;
import pl.kiosel.rosacore.scheduler.RosaTask;
import pl.kiosel.rosacore.version.MinecraftVersion;

import java.util.*;

public abstract class RosaScoreboard implements AutoCloseable {

	public static final int MAX_LINES = 15;

	private static final MinecraftVersion FLATTENING_VERSION = MinecraftVersion.of(1, 13, 0);
	private static final String OBJECTIVE_NAME = "rosa_sidebar";

	private final RosaScoreboardManager owner;
	private final RosaPlugin plugin;
	private final Player player;
	private final LegacyColorizer colorizer;
	private final Map<Integer, String> lines = new LinkedHashMap<>();
	private final LineHandle[] handles = new LineHandle[MAX_LINES];

	private Scoreboard scoreboard;
	private Scoreboard previousScoreboard;
	private Objective objective;
	private RosaTask refreshTask;
	private String title = "";
	private String renderedTitle = "";
	private long updateIntervalTicks;
	private boolean placeholderApiEnabled = true;
	private boolean shown;
	private boolean closed;
	private ScoreboardNumberFormat numberFormat = ScoreboardNumberFormat.BLANK;

	protected RosaScoreboard(RosaPlugin plugin, Player player) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.player = Objects.requireNonNull(player, "player");
		this.owner = plugin.getScoreboards();
		this.colorizer = LegacyColorizer.forVersion(plugin.getMinecraftVersion());
	}

	protected abstract void onUpdate();

	public final RosaScoreboard show() {
		this.owner.show(this);
		return this;
	}

	public final synchronized RosaScoreboard refresh() {
		ensureOpen();
		onUpdate();
		return this;
	}

	public final synchronized RosaScoreboard setTitle(String title, Object... placeholders) {
		ensureOpen();
		this.title = title == null ? "" : title;
		this.renderedTitle = render(this.title, placeholders);
		if (this.objective != null) applyTitle();
		return this;
	}

	public final synchronized String getTitle() {
		return this.title;
	}

	public final synchronized RosaScoreboard setLine(int line, String text, Object... placeholders) {
		ensureOpen();
		validateLine(line);
		if (text == null) return clearLine(line);
		String rendered = render(text, placeholders);
		if (rendered.equals(this.lines.get(line))) return this;
		this.lines.put(line, rendered);
		if (this.scoreboard != null) renderLine(line, rendered);
		return this;
	}

	public final synchronized RosaScoreboard clearLine(int line) {
		ensureOpen();
		validateLine(line);
		this.lines.remove(line);
		if (this.scoreboard != null) removeLine(line);
		return this;
	}

	public final synchronized RosaScoreboard clearLines() {
		ensureOpen();
		this.lines.clear();
		if (this.scoreboard != null) {
			for (int line = 0; line < MAX_LINES; line++) removeLine(line);
		}
		return this;
	}

	public final synchronized String getLine(int line) {
		validateLine(line);
		return this.lines.get(line);
	}

	public final synchronized Map<Integer, String> getLines() {
		return Collections.unmodifiableMap(new LinkedHashMap<>(this.lines));
	}

	public final synchronized RosaScoreboard setUpdateInterval(long ticks) {
		ensureOpen();
		if (ticks < 0L) throw new IllegalArgumentException("Update interval cannot be negative: " + ticks);
		this.updateIntervalTicks = ticks;
		if (this.shown) scheduleRefresh();
		return this;
	}

	public final synchronized long getUpdateInterval() {
		return this.updateIntervalTicks;
	}

	public final synchronized RosaScoreboard setPlaceholderApiEnabled(boolean enabled) {
		ensureOpen();
		this.placeholderApiEnabled = enabled;
		return this;
	}

	public final synchronized boolean isPlaceholderApiEnabled() {
		return this.placeholderApiEnabled;
	}

	public final synchronized RosaScoreboard setNumberFormat(ScoreboardNumberFormat format) {
		ensureOpen();
		this.numberFormat = Objects.requireNonNull(format, "format");
		if (this.objective != null) applyNumberFormat();
		return this;
	}

	public final RosaScoreboard hideNumbers() {
		return setNumberFormat(ScoreboardNumberFormat.BLANK);
	}

	public final RosaScoreboard showNumbers() {
		return setNumberFormat(ScoreboardNumberFormat.DEFAULT);
	}

	public final synchronized ScoreboardNumberFormat getNumberFormat() {
		return this.numberFormat;
	}

	public final boolean supportsModernFormats() {
		return this.owner.supportsModernFormats();
	}

	public final Player getPlayer() {
		return this.player;
	}

	public final RosaPlugin getPlugin() {
		return this.plugin;
	}

	public final synchronized Scoreboard getBukkitScoreboard() {
		return this.scoreboard;
	}

	public final synchronized boolean isShown() {
		return this.shown;
	}

	public final synchronized boolean isClosed() {
		return this.closed;
	}

	@Override
	public final void close() {
		this.owner.forget(this);
		closeFromManager(true);
	}

	RosaScoreboardManager getOwner() {
		return this.owner;
	}

	synchronized void openFromManager() {
		ensureOpen();
		if (this.shown) {
			refresh();
			return;
		}
		ScoreboardManager manager = Bukkit.getScoreboardManager();
		if (manager == null) throw new IllegalStateException("Bukkit scoreboard manager is unavailable");

		this.previousScoreboard = this.player.getScoreboard();
		this.scoreboard = manager.getNewScoreboard();
		try {
			this.objective = this.scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy");
			this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
			applyTitle();
			createLineHandles();
			applyNumberFormat();
			for (Map.Entry<Integer, String> entry : this.lines.entrySet()) {
				renderLine(entry.getKey(), entry.getValue());
			}
			this.shown = true;
			refresh();
			this.player.setScoreboard(this.scoreboard);
			scheduleRefresh();
		} catch (RuntimeException | Error exception) {
			this.shown = false;
			this.scoreboard = null;
			this.previousScoreboard = null;
			this.objective = null;
			Arrays.fill(this.handles, null);
			throw exception;
		}
	}

	synchronized void closeFromManager(boolean restorePrevious) {
		if (this.closed) return;
		this.closed = true;
		this.shown = false;
		cancelRefresh();

		Scoreboard current = this.scoreboard;
		if (restorePrevious && current != null && this.player.isOnline()
				&& this.player.getScoreboard() == current && this.previousScoreboard != null) {
			this.player.setScoreboard(this.previousScoreboard);
		}
		this.scoreboard = null;
		this.previousScoreboard = null;
		this.objective = null;

		Arrays.fill(this.handles, null);
	}

	private void createLineHandles() {
		ChatColor[] colors = ChatColor.values();
		for (int line = 0; line < MAX_LINES; line++) {
			String entry = colors[line].toString();
			Team team = this.scoreboard.registerNewTeam(String.format("rosa_line_%02d", line));
			team.addEntry(entry);
			this.handles[line] = new LineHandle(entry, team);
		}
	}

	private void renderLine(int line, String text) {
		LineHandle handle = this.handles[line];
		if (handle == null) return;
		Score score = this.objective.getScore(handle.entry);
		boolean modernName = this.owner.getPaperFeatures().setCustomName(score, text);
		if (modernName) {
			handle.team.setPrefix("");
			handle.team.setSuffix("");
		} else {
			ScoreboardText.Parts parts = ScoreboardText.split(text, linePartLimit());
			handle.team.setPrefix(parts.getPrefix());
			handle.team.setSuffix(parts.getSuffix());
		}
		score.setScore(MAX_LINES - line);
		handle.active = true;
	}

	private void removeLine(int line) {
		LineHandle handle = this.handles[line];
		if (handle == null || !handle.active) return;
		this.scoreboard.resetScores(handle.entry);
		handle.team.setPrefix("");
		handle.team.setSuffix("");
		handle.active = false;
	}

	private void applyTitle() {
		this.objective.setDisplayName(ScoreboardText.truncate(this.renderedTitle, titleLimit()));
	}

	private void applyNumberFormat() {
		String value = this.numberFormat.getValue();
		String renderedValue = value == null ? "" : render(value);
		this.owner.getPaperFeatures().apply(this.objective, this.numberFormat, renderedValue);
	}

	private String render(String text, Object... placeholderPairs) {
		String rendered = text == null ? "" : text.replace("\\n", "\n");
		MessagePlaceholders placeholders = MessagePlaceholders.pairs(
				placeholderPairs == null ? new Object[0] : placeholderPairs);
		for (Map.Entry<String, String> entry : placeholders.asMap().entrySet()) {
			rendered = rendered.replace('%' + entry.getKey() + '%', entry.getValue());
		}
		return this.colorizer.colorize(rendered);
	}

	private void scheduleRefresh() {
		cancelRefresh();
		if (this.updateIntervalTicks == 0L || !this.shown) return;
		this.refreshTask = this.plugin.getRosaScheduler().runForEntityTimer(
				this.player,
				() -> {
					if (!this.player.isOnline()) {
						this.owner.retire(this);
						return;
					}
					refresh();
				},
				() -> this.owner.retire(this),
				this.updateIntervalTicks,
				this.updateIntervalTicks
		);
	}

	private void cancelRefresh() {
		if (this.refreshTask == null) return;
		this.refreshTask.cancel();
		this.refreshTask = null;
	}

	private int linePartLimit() {
		return this.plugin.getMinecraftVersion().isAtLeast(FLATTENING_VERSION) ? 64 : 16;
	}

	private int titleLimit() {
		return this.plugin.getMinecraftVersion().isAtLeast(FLATTENING_VERSION) ? 128 : 32;
	}

	private static void validateLine(int line) {
		if (line < 0 || line >= MAX_LINES) {
			throw new IllegalArgumentException("Scoreboard line must be between 0 and 14: " + line);
		}
	}

	private void ensureOpen() {
		if (this.closed) throw new IllegalStateException("Scoreboard is closed");
	}

	private static final class LineHandle {

		private final String entry;
		private final Team team;
		private boolean active;

		private LineHandle(String entry, Team team) {
			this.entry = entry;
			this.team = team;
		}
	}
}

package pl.kiosel.rosacore.hologram;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.message.LegacyColorizer;
import pl.kiosel.rosacore.message.MessagePlaceholders;
import pl.kiosel.rosacore.scheduler.RosaTask;

import java.util.*;

public abstract class RosaHologram implements AutoCloseable {

	public static final double DEFAULT_LINE_SPACING = 0.27D;

	private final UUID id = UUID.randomUUID();
	private final RosaHologramManager owner;
	private final RosaPlugin plugin;
	private final LegacyColorizer colorizer;
	private final List<String> lines = new ArrayList<>();
	private final List<LineHandle> handles = new ArrayList<>();

	private Location location;
	private OfflinePlayer placeholderPlayer;
	private RosaTask refreshTask;
	private long updateIntervalTicks;
	private double lineSpacing = DEFAULT_LINE_SPACING;
	private boolean placeholderApiEnabled = true;
	private boolean shadowed;
	private boolean seeThrough;
	private boolean defaultBackground;
	private boolean updating;
	private boolean spawned;
	private boolean shown;
	private boolean closed;

	protected RosaHologram(RosaPlugin plugin, Location location) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.location = validateLocation(location).clone();
		this.owner = plugin.getHolograms();
		this.colorizer = LegacyColorizer.forVersion(plugin.getMinecraftVersion());
	}

	protected abstract void onUpdate();

	public final RosaHologram show() {
		this.owner.show(this);
		return this;
	}

	public final RosaHologram refresh() {
		ensureOpen();
		this.updating = true;
		try {
			onUpdate();
		} finally {
			this.updating = false;
		}
		if (this.shown) renderLines();
		return this;
	}

	public final RosaHologram setLines(String... lines) {
		Objects.requireNonNull(lines, "lines");
		return setLines(Arrays.asList(lines));
	}

	public final RosaHologram setLines(Collection<String> lines) {
		ensureOpen();
		Objects.requireNonNull(lines, "lines");
		List<String> preparedLines = new ArrayList<>(lines.size());
		for (String line : lines) {
			preparedLines.add(prepare(Objects.requireNonNull(line, "line")));
		}
		this.lines.clear();
		this.lines.addAll(preparedLines);
		renderIfNeeded();
		return this;
	}

	public final RosaHologram setLine(int line, String text, Object... placeholders) {
		ensureOpen();
		validateLine(line);
		while (this.lines.size() <= line) this.lines.add("");
		this.lines.set(line, prepare(Objects.requireNonNull(text, "text"), placeholders));
		renderIfNeeded();
		return this;
	}

	public final RosaHologram addLine(String text, Object... placeholders) {
		ensureOpen();
		this.lines.add(prepare(Objects.requireNonNull(text, "text"), placeholders));
		renderIfNeeded();
		return this;
	}

	public final RosaHologram removeLine(int line) {
		ensureOpen();
		validateExistingLine(line);
		this.lines.remove(line);
		renderIfNeeded();
		return this;
	}

	public final RosaHologram clearLines() {
		ensureOpen();
		this.lines.clear();
		renderIfNeeded();
		return this;
	}

	public final String getLine(int line) {
		validateExistingLine(line);
		return this.lines.get(line);
	}

	public final List<String> getLines() {
		return Collections.unmodifiableList(new ArrayList<>(this.lines));
	}

	public final RosaHologram setLocation(Location location) {
		ensureOpen();
		Location checked = validateLocation(location).clone();
		if (this.location.equals(checked)) return this;
		despawnLines();
		this.location = checked;
		if (this.shown) {
			renderLines();
			scheduleRefresh();
		}
		return this;
	}

	public final Location getLocation() {
		return this.location.clone();
	}

	public final RosaHologram setLineSpacing(double lineSpacing) {
		ensureOpen();
		this.lineSpacing = validateLineSpacing(lineSpacing);
		renderIfNeeded();
		return this;
	}

	public final double getLineSpacing() {
		return this.lineSpacing;
	}

	public final RosaHologram setUpdateInterval(long ticks) {
		ensureOpen();
		if (ticks < 0L) throw new IllegalArgumentException("Update interval cannot be negative: " + ticks);
		this.updateIntervalTicks = ticks;
		if (this.shown) scheduleRefresh();
		return this;
	}

	public final long getUpdateInterval() {
		return this.updateIntervalTicks;
	}

	public final RosaHologram setPlaceholderPlayer(OfflinePlayer player) {
		ensureOpen();
		this.placeholderPlayer = player;
		renderIfNeeded();
		return this;
	}

	public final OfflinePlayer getPlaceholderPlayer() {
		return this.placeholderPlayer;
	}

	public final RosaHologram setPlaceholderApiEnabled(boolean enabled) {
		ensureOpen();
		this.placeholderApiEnabled = enabled;
		renderIfNeeded();
		return this;
	}

	public final boolean isPlaceholderApiEnabled() {
		return this.placeholderApiEnabled;
	}

	public final RosaHologram setShadowed(boolean shadowed) {
		ensureOpen();
		this.shadowed = shadowed;
		renderIfNeeded();
		return this;
	}

	public final boolean isShadowed() {
		return this.shadowed;
	}

	public final RosaHologram setSeeThrough(boolean seeThrough) {
		ensureOpen();
		this.seeThrough = seeThrough;
		renderIfNeeded();
		return this;
	}

	public final boolean isSeeThrough() {
		return this.seeThrough;
	}

	public final RosaHologram setDefaultBackground(boolean defaultBackground) {
		ensureOpen();
		this.defaultBackground = defaultBackground;
		renderIfNeeded();
		return this;
	}

	public final boolean hasDefaultBackground() {
		return this.defaultBackground;
	}

	public final boolean supportsTextDisplays() {
		return this.owner.supportsTextDisplays();
	}

	public final UUID getId() {
		return this.id;
	}

	public final RosaPlugin getPlugin() {
		return this.plugin;
	}

	public final boolean isSpawned() {
		return this.spawned;
	}

	public final boolean isShown() {
		return this.shown;
	}

	public final boolean isClosed() {
		return this.closed;
	}

	@Override
	public final void close() {
		this.owner.forget(this);
		closeFromManager();
	}

	protected RosaHologramManager getOwner() {
		return this.owner;
	}

	protected void openFromManager() {
		ensureOpen();
		if (this.shown) {
			refresh();
			return;
		}
		this.shown = true;
		try {
			refresh();
			scheduleRefresh();
		} catch (RuntimeException | Error exception) {
			this.shown = false;
			despawnLines();
			throw exception;
		}
	}

	protected void unloadFromManager() {
		if (!this.shown || this.closed) return;
		despawnLines();
	}

	protected void reloadFromManager() {
		if (!this.shown || this.closed) return;
		refresh();
	}

	protected void closeFromManager() {
		if (this.closed) return;
		this.closed = true;
		this.shown = false;
		cancelRefresh();
		despawnLines();
	}

	protected boolean isInChunk(Chunk chunk) {
		return this.location.getWorld() == chunk.getWorld()
				&& (this.location.getBlockX() >> 4) == chunk.getX()
				&& (this.location.getBlockZ() >> 4) == chunk.getZ();
	}

	protected boolean isInWorld(World world) {
		return this.location.getWorld() == world;
	}

	private void renderIfNeeded() {
		if (this.shown && !this.updating) renderLines();
	}

	private void renderLines() {
		if (!isAnchorChunkLoaded()) {
			despawnLines();
			return;
		}
		HologramEntityFactory factory = this.owner.getEntityFactory();
		while (this.handles.size() > this.lines.size()) {
			LineHandle removed = this.handles.remove(this.handles.size() - 1);
			factory.remove(removed.entity);
		}

		for (int line = 0; line < this.lines.size(); line++) {
			String text = render(this.lines.get(line));
			Location lineLocation = this.location.clone().subtract(0.0D, this.lineSpacing * line, 0.0D);
			LineHandle handle = line < this.handles.size() ? this.handles.get(line) : null;
			if (handle == null || !factory.isUsable(handle.entity)) {
				if (handle != null) factory.remove(handle.entity);
				Entity entity = factory.spawn(this.id, lineLocation, text,
						this.shadowed, this.seeThrough, this.defaultBackground);
				LineHandle replacement = new LineHandle(entity, lineLocation, text,
						this.shadowed, this.seeThrough, this.defaultBackground);
				if (handle == null) this.handles.add(replacement);
				else this.handles.set(line, replacement);
				continue;
			}
			if (!handle.matches(lineLocation, text, this.shadowed, this.seeThrough, this.defaultBackground)) {
				factory.update(handle.entity, lineLocation, text,
						this.shadowed, this.seeThrough, this.defaultBackground);
				handle.update(lineLocation, text, this.shadowed, this.seeThrough, this.defaultBackground);
			}
		}
		this.spawned = !this.handles.isEmpty();
	}

	private void despawnLines() {
		HologramEntityFactory factory = this.owner.getEntityFactory();
		for (LineHandle handle : this.handles) factory.remove(handle.entity);
		this.handles.clear();
		this.spawned = false;
	}

	private boolean isAnchorChunkLoaded() {
		World world = this.location.getWorld();
		return world.isChunkLoaded(this.location.getBlockX() >> 4, this.location.getBlockZ() >> 4);
	}

	private String prepare(String text, Object... placeholderPairs) {
		String prepared = text.replace("\\n", "\n");
		MessagePlaceholders placeholders = MessagePlaceholders.pairs(
				placeholderPairs == null ? new Object[0] : placeholderPairs);
		for (Map.Entry<String, String> entry : placeholders.asMap().entrySet()) {
			prepared = prepared.replace('%' + entry.getKey() + '%', entry.getValue());
		}
		return prepared;
	}

	private String render(String text) {
		return this.colorizer.colorize(text);
	}

	private void scheduleRefresh() {
		cancelRefresh();
		if (this.updateIntervalTicks == 0L || !this.shown) return;
		this.refreshTask = this.plugin.getRosaScheduler().runAtTimer(
				this.location.clone(), this::refresh,
				this.updateIntervalTicks, this.updateIntervalTicks);
	}

	private void cancelRefresh() {
		if (this.refreshTask == null) return;
		this.refreshTask.cancel();
		this.refreshTask = null;
	}

	protected static double validateLineSpacing(double lineSpacing) {
		if (Double.isNaN(lineSpacing) || Double.isInfinite(lineSpacing) || lineSpacing <= 0.0D) {
			throw new IllegalArgumentException("Hologram line spacing must be greater than zero: " + lineSpacing);
		}
		return lineSpacing;
	}

	private static Location validateLocation(Location location) {
		Objects.requireNonNull(location, "location");
		if (location.getWorld() == null) throw new IllegalArgumentException("Hologram location has no world");
		if (!finite(location.getX()) || !finite(location.getY()) || !finite(location.getZ())) {
			throw new IllegalArgumentException("Hologram location contains a non-finite coordinate");
		}
		return location;
	}

	private static boolean finite(double value) {
		return !Double.isNaN(value) && !Double.isInfinite(value);
	}

	private static void validateLine(int line) {
		if (line < 0) throw new IllegalArgumentException("Hologram line cannot be negative: " + line);
	}

	private void validateExistingLine(int line) {
		if (line < 0 || line >= this.lines.size()) {
			throw new IllegalArgumentException("Hologram line does not exist: " + line);
		}
	}

	private void ensureOpen() {
		if (this.closed) throw new IllegalStateException("Hologram is closed");
	}

	private static final class LineHandle {

		private final Entity entity;
		private Location location;
		private String text;
		private boolean shadowed;
		private boolean seeThrough;
		private boolean defaultBackground;

		private LineHandle(Entity entity, Location location, String text,
						   boolean shadowed, boolean seeThrough, boolean defaultBackground) {
			this.entity = entity;
			update(location, text, shadowed, seeThrough, defaultBackground);
		}

		private boolean matches(Location location, String text,
								boolean shadowed, boolean seeThrough, boolean defaultBackground) {
			return this.location.equals(location) && this.text.equals(text)
					&& this.shadowed == shadowed && this.seeThrough == seeThrough
					&& this.defaultBackground == defaultBackground;
		}

		private void update(Location location, String text,
							boolean shadowed, boolean seeThrough, boolean defaultBackground) {
			this.location = location.clone();
			this.text = text;
			this.shadowed = shadowed;
			this.seeThrough = seeThrough;
			this.defaultBackground = defaultBackground;
		}
	}
}

package pl.kiosel.rosacore.scoreboard;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.kiosel.rosacore.RosaPlugin;

import java.util.*;

public final class RosaScoreboardManager implements Listener, AutoCloseable {

	@Getter
	private final RosaPlugin plugin;
	private final PaperScoreboardFeatures paperFeatures;
	private final Map<UUID, RosaScoreboard> active = new LinkedHashMap<>();
	private boolean closed;

	public RosaScoreboardManager(RosaPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		ClassLoader serverLoader = plugin.getServer().getClass().getClassLoader();
		this.paperFeatures = new PaperScoreboardFeatures(serverLoader);
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public synchronized RosaScoreboard get(Player player) {
		return player == null ? null : this.active.get(player.getUniqueId());
	}

	public synchronized boolean has(Player player) {
		return get(player) != null;
	}

	public synchronized int size() {
		return this.active.size();
	}

	public synchronized List<RosaScoreboard> getActive() {
		return Collections.unmodifiableList(new ArrayList<>(this.active.values()));
	}

	public boolean supportsModernFormats() {
		return this.paperFeatures.isSupported();
	}

	public synchronized boolean isClosed() {
		return this.closed;
	}

	public void close(Player player) {
		if (player == null) return;
		RosaScoreboard scoreboard;
		synchronized (this) {
			scoreboard = this.active.remove(player.getUniqueId());
		}
		if (scoreboard != null) scoreboard.closeFromManager(true);
	}

	@Override
	public void close() {
		List<RosaScoreboard> snapshot;
		synchronized (this) {
			if (this.closed) return;
			this.closed = true;
			snapshot = new ArrayList<>(this.active.values());
			this.active.clear();
		}
		for (RosaScoreboard scoreboard : snapshot) scoreboard.closeFromManager(true);
		HandlerList.unregisterAll(this);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		RosaScoreboard scoreboard;
		synchronized (this) {
			scoreboard = this.active.remove(event.getPlayer().getUniqueId());
		}
		if (scoreboard != null) scoreboard.closeFromManager(false);
	}

	void show(RosaScoreboard scoreboard) {
		RosaScoreboard previous;
		synchronized (this) {
			ensureOpen();
			if (scoreboard.getOwner() != this) {
				throw new IllegalArgumentException("The scoreboard belongs to another RosaScoreboardManager");
			}
			UUID playerId = scoreboard.getPlayer().getUniqueId();
			previous = this.active.remove(playerId);
		}

		if (previous == scoreboard) {
			boolean rejected;
			synchronized (this) {
				rejected = this.closed;
				if (!rejected) this.active.put(scoreboard.getPlayer().getUniqueId(), scoreboard);
			}
			if (rejected) {
				scoreboard.closeFromManager(true);
				throw new IllegalStateException("Scoreboard manager was closed while refreshing a scoreboard");
			}
			scoreboard.refresh();
			return;
		}
		if (previous != null) previous.closeFromManager(true);
		scoreboard.openFromManager();

		boolean rejected;
		synchronized (this) {
			rejected = this.closed;
			if (!rejected) this.active.put(scoreboard.getPlayer().getUniqueId(), scoreboard);
		}
		if (rejected) {
			scoreboard.closeFromManager(true);
			throw new IllegalStateException("Scoreboard manager was closed while opening a scoreboard");
		}
	}

	synchronized void forget(RosaScoreboard scoreboard) {
		this.active.remove(scoreboard.getPlayer().getUniqueId(), scoreboard);
	}

	void retire(RosaScoreboard scoreboard) {
		forget(scoreboard);
		scoreboard.closeFromManager(false);
	}

	PaperScoreboardFeatures getPaperFeatures() {
		return this.paperFeatures;
	}

	private void ensureOpen() {
		if (this.closed) throw new IllegalStateException("Scoreboard manager is closed");
	}
}

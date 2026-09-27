package pl.kiosel.rosacore.hologram;

import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import pl.kiosel.rosacore.RosaPlugin;

import java.util.*;

public final class RosaHologramManager implements Listener, AutoCloseable {

	@Getter
	private final RosaPlugin plugin;
	private final HologramEntityFactory entityFactory;
	private final Map<UUID, RosaHologram> active = new LinkedHashMap<>();
	@Getter private boolean closed;

	public RosaHologramManager(RosaPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.entityFactory = new HologramEntityFactory(plugin);
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public synchronized RosaHologram get(UUID id) {
		return id == null ? null : this.active.get(id);
	}

	public synchronized boolean has(UUID id) {
		return get(id) != null;
	}

	public synchronized int size() {
		return this.active.size();
	}

	public synchronized List<RosaHologram> getActive() {
		return Collections.unmodifiableList(new ArrayList<>(this.active.values()));
	}

	public boolean supportsTextDisplays() {
		return this.entityFactory.supportsTextDisplays();
	}

	public void close(UUID id) {
		if (id == null) return;
		RosaHologram hologram;
		synchronized (this) {
			hologram = this.active.remove(id);
		}
		if (hologram != null) hologram.closeFromManager();
	}

	@Override
	public void close() {
		List<RosaHologram> snapshot;
		synchronized (this) {
			if (this.closed) return;
			this.closed = true;
			snapshot = new ArrayList<>(this.active.values());
			this.active.clear();
		}
		RuntimeException failure = null;
		for (RosaHologram hologram : snapshot) {
			try {
				hologram.closeFromManager();
			} catch (RuntimeException exception) {
				if (failure == null) failure = exception;
				else failure.addSuppressed(exception);
			}
		}
		HandlerList.unregisterAll(this);
		if (failure != null) throw failure;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onChunkUnload(ChunkUnloadEvent event) {
		for (RosaHologram hologram : inChunk(event.getChunk())) hologram.unloadFromManager();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onChunkLoad(ChunkLoadEvent event) {
		for (RosaHologram hologram : inChunk(event.getChunk())) hologram.reloadFromManager();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWorldUnload(WorldUnloadEvent event) {
		List<RosaHologram> removed = new ArrayList<>();
		synchronized (this) {
			for (RosaHologram hologram : new ArrayList<>(this.active.values())) {
				if (!hologram.isInWorld(event.getWorld())) continue;
				this.active.remove(hologram.getId(), hologram);
				removed.add(hologram);
			}
		}
		for (RosaHologram hologram : removed) hologram.closeFromManager();
	}

	void show(RosaHologram hologram) {
		synchronized (this) {
			ensureOpen();
			if (hologram.getOwner() != this) {
				throw new IllegalArgumentException("The hologram belongs to another RosaHologramManager");
			}
			if (this.active.get(hologram.getId()) == hologram) {
				hologram.refresh();
				return;
			}
			this.active.put(hologram.getId(), hologram);
		}

		try {
			hologram.openFromManager();
		} catch (RuntimeException | Error exception) {
			forget(hologram);
			hologram.closeFromManager();
			throw exception;
		}
	}

	public void forget(RosaHologram hologram) {
		this.active.remove(hologram.getId(), hologram);
	}

	HologramEntityFactory getEntityFactory() {
		return this.entityFactory;
	}

	private List<RosaHologram> inChunk(Chunk chunk) {
		if (this.closed) return Collections.emptyList();
		List<RosaHologram> matching = new ArrayList<>();
		for (RosaHologram hologram : this.active.values()) {
			if (hologram.isInChunk(chunk)) matching.add(hologram);
		}
		return matching;
	}

	private void ensureOpen() {
		if (this.closed) throw new IllegalStateException("Hologram manager is closed");
	}
}

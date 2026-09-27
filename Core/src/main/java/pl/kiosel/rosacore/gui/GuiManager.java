package pl.kiosel.rosacore.gui;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import pl.kiosel.rosacore.RosaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiManager implements AutoCloseable {

	@Getter
	private final RosaPlugin plugin;
	private final GuiListener listener = new GuiListener();
	private final Map<UUID, Gui> openGuis = new ConcurrentHashMap<>();
	private final Set<UUID> transitions = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final Set<UUID> silentClosures = Collections.newSetFromMap(new ConcurrentHashMap<>());
	@Getter
	private volatile boolean initialized;
	@Getter
	private volatile boolean closed;

	public GuiManager(RosaPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	public synchronized void init() {
		if (closed) throw new IllegalStateException("GuiManager is already closed");
		if (initialized) return;
		plugin.getServer().getPluginManager().registerEvents(listener, plugin);
		initialized = true;
	}

	public void openGUI(Player player, Gui gui) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(gui, "gui");
		if (closed || !plugin.isEnabled()) return;
		if (!initialized) init();

		plugin.getRosaScheduler().runForEntity(player, () -> openNow(player, gui),
				() -> openGuis.remove(player.getUniqueId()));
	}

	public void showGUI(Player player, Gui gui) {
		openGUI(player, gui);
	}

	public Gui getOpenGui(Player player) {
		Objects.requireNonNull(player, "player");
		return openGuis.get(player.getUniqueId());
	}

	public void closeGUI(Player player) {
		Objects.requireNonNull(player, "player");
		if (closed) return;
		plugin.getRosaScheduler().runForEntity(player, player::closeInventory,
				() -> openGuis.remove(player.getUniqueId()));
	}

	public void closeAll() {
		List<UUID> players = new ArrayList<>(openGuis.keySet());
		for (UUID uuid : players) {
			Player player = Bukkit.getPlayer(uuid);
			if (player != null) closeGUI(player);
			else openGuis.remove(uuid);
		}
	}

	@Override
	public synchronized void close() {
		if (closed) return;
		closed = true;
		closeAllNow();
		HandlerList.unregisterAll(listener);
		initialized = false;
	}

	public void refreshGui(Gui gui, List<Player> viewers) {
		if (closed) return;
		for (Player player : viewers) {
			if (player != null && player.isOnline() && openGuis.get(player.getUniqueId()) == gui) {
				openGUI(player, gui);
			}
		}
	}

	public void closeGui(Gui gui, boolean silent) {
		if (closed) return;
		for (Player player : gui.getPlayers()) {
			UUID uuid = player.getUniqueId();
			if (silent) silentClosures.add(uuid);
			plugin.getRosaScheduler().runForEntity(player, player::closeInventory,
					() -> openGuis.remove(uuid, gui));
		}
	}

	private void openNow(Player player, Gui gui) {
		if (closed || !player.isOnline()) return;
		UUID uuid = player.getUniqueId();
		transitions.add(uuid);
		openGuis.put(uuid, gui);
		try {
			if (gui instanceof AnvilGui) {
				((AnvilGui) gui).openAnvil(this, player);
			} else {
				Inventory inventory = gui.getOrCreateInventory(this);
				player.openInventory(inventory);
			}
			gui.handleOpen(this, player);
		} catch (RuntimeException exception) {
			openGuis.remove(uuid, gui);
			throw exception;
		} finally {
			transitions.remove(uuid);
		}
	}

	private void closeAllNow() {
		for (UUID uuid : new ArrayList<>(openGuis.keySet())) {
			Player player = Bukkit.getPlayer(uuid);
			if (player != null) {
				silentClosures.add(uuid);
				try {
					player.closeInventory();
				} catch (RuntimeException ignored) {
				}
			}
		}
		openGuis.clear();
		transitions.clear();
		silentClosures.clear();
	}

	private GuiHolder holder(Inventory inventory) {
		if (inventory == null) return null;
		InventoryHolder holder = inventory.getHolder();
		if (!(holder instanceof GuiHolder)) return null;
		GuiHolder guiHolder = (GuiHolder) holder;
		return guiHolder.getManager() == this ? guiHolder : null;
	}

	private Gui guiFor(Player player, Inventory inventory) {
		GuiHolder holder = holder(inventory);
		if (holder != null) return holder.getGui();

		Gui gui = openGuis.get(player.getUniqueId());
		Inventory guiInventory = gui == null ? null : gui.getInventoryInternal();
		return guiInventory != null && guiInventory.equals(inventory) ? gui : null;
	}

	private final class GuiListener implements Listener {

		@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
		public void onClick(InventoryClickEvent event) {
			if (!(event.getWhoClicked() instanceof Player)) return;
			Player player = (Player) event.getWhoClicked();
			Gui gui = guiFor(player, event.getInventory());
			if (gui == null) return;
			int rawSlot = event.getRawSlot();
			int topSize = event.getInventory().getSize();

			if (rawSlot < 0) {
				if (!gui.handleOutsideClick(GuiManager.this, player, event)) event.setCancelled(true);
				return;
			}

			if (rawSlot < topSize) {
				if (!gui.isUnlocked(rawSlot)) event.setCancelled(true);
				gui.handleTopClick(GuiManager.this, player, event);
				return;
			}

			boolean handled = gui.handlePlayerClick(GuiManager.this, player, event);
			boolean mayMoveToGui = event.isShiftClick()
					|| event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
					|| event.getClick() == ClickType.DOUBLE_CLICK;
			if (!gui.acceptsItems() || mayMoveToGui) event.setCancelled(true);
			if (handled) gui.playDefaultSound(player);
		}

		@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
		public void onDrag(InventoryDragEvent event) {
			if (!(event.getWhoClicked() instanceof Player)) return;
			Player player = (Player) event.getWhoClicked();
			Gui gui = guiFor(player, event.getInventory());
			if (gui == null) return;
			int topSize = event.getInventory().getSize();
			for (Integer slot : event.getRawSlots()) {
				if (slot < topSize && !gui.isUnlocked(slot)) {
					event.setCancelled(true);
					break;
				}
			}
			gui.handleDrag(GuiManager.this, player, event);
		}

		@EventHandler(priority = EventPriority.MONITOR)
		public void onClose(InventoryCloseEvent event) {
			if (!(event.getPlayer() instanceof Player)) return;
			Player player = (Player) event.getPlayer();
			Gui closedGui = guiFor(player, event.getInventory());
			if (closedGui == null) return;
			UUID uuid = player.getUniqueId();
			Gui current = openGuis.get(uuid);

			if (transitions.contains(uuid) || (current != null && current != closedGui)) return;
			openGuis.remove(uuid, closedGui);

			if (silentClosures.remove(uuid) || closed) return;
			if (!closedGui.allowsDropItems()) player.setItemOnCursor(null);
			plugin.getRosaScheduler().runForEntityLater(player, () -> {
				if (closed || !player.isOnline()) return;
				if (!closedGui.allowsClose()) {
					openGUI(player, closedGui);
					return;
				}
				closedGui.handleClose(GuiManager.this, player);
				if (closedGui.getParent() != null && openGuis.get(uuid) == null) {
					openGUI(player, closedGui.getParent());
				}
			}, () -> openGuis.remove(uuid), 1L);
		}

		@EventHandler
		public void onPluginDisable(PluginDisableEvent event) {
			if (event.getPlugin() == plugin) close();
		}
	}
}

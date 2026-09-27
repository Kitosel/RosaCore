package pl.kiosel.rosacore.nms;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.nms.api.anvil.CustomAnvil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CustomAnvilManager implements Listener, AutoCloseable {

	private final Map<UUID, CustomAnvil> active = new HashMap<>();

	public CustomAnvilManager(RosaPlugin plugin) {
		if (plugin == null) throw new NullPointerException("plugin");
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public CustomAnvil open(CustomAnvil anvil) {
		if (anvil == null) throw new NullPointerException("anvil");
		Player player = anvil.getPlayer();
		close(player);
		anvil.open();
		active.put(player.getUniqueId(), anvil);
		return anvil;
	}

	public void close(Player player) {
		if (player == null) return;
		CustomAnvil anvil = active.remove(player.getUniqueId());
		if (anvil != null) {
			discardVirtualItems(anvil);
			anvil.close();
		}
	}

	public CustomAnvil getOpen(Player player) {
		return player == null ? null : active.get(player.getUniqueId());
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;
		CustomAnvil anvil = getOpen((Player) event.getWhoClicked());
		if (anvil == null || !isAnvilInventory(anvil, event.getInventory())) return;
		int slot = event.getRawSlot();
		if (slot < CustomAnvil.LEFT_INPUT_SLOT || slot > CustomAnvil.RESULT_SLOT) return;
		if (!anvil.handleClick(slot, event.getCurrentItem())) event.setCancelled(true);
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player)) return;
		Player player = (Player) event.getPlayer();
		CustomAnvil anvil = getOpen(player);
		if (anvil == null || !isAnvilInventory(anvil, event.getInventory())) return;
		discardVirtualItems(anvil);
		active.remove(player.getUniqueId());
		anvil.handleClose();
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		CustomAnvil anvil = active.remove(event.getPlayer().getUniqueId());
		if (anvil != null) {
			discardVirtualItems(anvil);
			anvil.handleClose();
		}
	}

	@Override
	public void close() {
		for (CustomAnvil anvil : active.values().toArray(new CustomAnvil[0])) {
			discardVirtualItems(anvil);
			anvil.close();
		}
		active.clear();
		HandlerList.unregisterAll(this);
	}

	private static boolean isAnvilInventory(CustomAnvil anvil, Inventory inventory) {
		Inventory anvilInventory = anvil.getInventory();
		return anvilInventory != null && anvilInventory.equals(inventory);
	}

	private static void discardVirtualItems(CustomAnvil anvil) {
		for (int slot = CustomAnvil.LEFT_INPUT_SLOT; slot <= CustomAnvil.RESULT_SLOT; slot++) {
			anvil.setItem(slot, null);
		}
	}
}

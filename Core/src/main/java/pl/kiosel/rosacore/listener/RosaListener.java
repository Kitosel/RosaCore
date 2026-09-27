package pl.kiosel.rosacore.listener;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.compatibility.ZMaterial;
import pl.kiosel.rosacore.material.ItemTag;
import pl.kiosel.rosacore.message.MessageCatalog;
import pl.kiosel.rosacore.message.RosaMessenger;

import java.util.Objects;

public abstract class RosaListener implements Listener {

	@Getter
	private final RosaPlugin plugin;

	public RosaListener(RosaPlugin plugin) {
		this.plugin = plugin;
	}

	public boolean isAvailable() {
		return true;
	}

	public void cancel(Cancellable event) {
		event.setCancelled(true);
	}

	public void noCancel(Cancellable event) {
		event.setCancelled(false);
	}

	public boolean hasTag(ItemStack item, String key) {
		return ItemTag.has(item, key);
	}

	public boolean isSameType(ItemStack item1, ItemStack item2) {
		return isSameType(item1.getType(), item2.getType());
	}

	public boolean isSameType(ZMaterial item1, ZMaterial item2) {
		Objects.requireNonNull(item1);
		Objects.requireNonNull(item2);
		return item1.matches(item2);
	}

	public boolean isSameType(Material item1, Material item2) {
		Objects.requireNonNull(item1);
		Objects.requireNonNull(item2);
		return item1.equals(item2);
	}

	public MessageCatalog getMessage() {
		return plugin.getMessages();
	}

	public RosaMessenger getMessenger() {
		return plugin.getMessenger();
	}
}
package pl.kiosel.rosacore.nms.api.anvil;

import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface AnvilClickHandler {

	boolean onClick(CustomAnvil anvil, int slot, ItemStack currentItem);
}

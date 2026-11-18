package pl.kiosel.rosacore.nms.api.entity;

import org.bukkit.inventory.ItemStack;

public interface FakeEntityFactory {

	FakeItem createItem(ItemStack itemStack);
}

package pl.kiosel.rosacore.nms.api.entity;

import org.bukkit.inventory.ItemStack;

public interface FakeItem extends FakeEntity {

	ItemStack getItemStack();

	void setItemStack(ItemStack itemStack);
}

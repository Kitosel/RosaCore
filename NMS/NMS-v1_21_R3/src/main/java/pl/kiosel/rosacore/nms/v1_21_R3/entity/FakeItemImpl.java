package pl.kiosel.rosacore.nms.v1_21_R3.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.nms.api.entity.FakeItem;

import java.util.Objects;

final class FakeItemImpl extends FakeEntityImpl implements FakeItem {

	private ItemStack itemStack;

	FakeItemImpl(ItemStack itemStack) {
		this.itemStack = copyItem(itemStack);
	}

	@Override
	public ItemStack getItemStack() {
		return itemStack.clone();
	}

	@Override
	public void setItemStack(ItemStack itemStack) {
		this.itemStack = copyItem(itemStack);
		if (entity != null) {
			((ItemEntity) entity).setItem(CraftItemStack.asNMSCopy(this.itemStack));
			sendMetadataToViewers();
		}
	}

	@Override
	protected Entity createEntity(Location location) {
		ItemEntity item = new ItemEntity(
				((CraftWorld) location.getWorld()).getHandle(),
				location.getX(), location.getY(), location.getZ(),
				CraftItemStack.asNMSCopy(itemStack));
		item.makeFakeItem();
		item.setNoGravity(true);
		return item;
	}

	private ItemStack copyItem(ItemStack itemStack) {
		return Objects.requireNonNull(itemStack, "itemStack").clone();
	}
}

package pl.kiosel.rosacore.nms.v1_21_R5.entity;

import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.nms.api.entity.FakeItem;
import pl.kiosel.rosacore.nms.api.entity.FakeEntityFactory;

public final class FakeEntityFactoryImpl implements FakeEntityFactory {

	@Override
	public FakeItem createItem(ItemStack itemStack) {
		return new FakeItemImpl(itemStack);
	}
}

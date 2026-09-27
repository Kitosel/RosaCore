package pl.kiosel.rosacore.nms.v1_18_R1.anvil;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;

final class NMSAnvilContainer extends AnvilMenu {

	private final NMSCustomAnvil anvil;

	NMSAnvilContainer(int windowId, Inventory inventory, ContainerLevelAccess access,
	                  NMSCustomAnvil anvil) {
		super(windowId, inventory, access);
		this.anvil = anvil;
		this.checkReachable = false;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void setItemName(String text) {
		super.setItemName(text);
		anvil.handleTextChange(text == null ? "" : text);
	}
}

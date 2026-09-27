package pl.kiosel.rosacore.nms.v1_20_R3.anvil;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;

final class NMSAnvilContainer extends AnvilMenu {

	private final NMSCustomAnvil anvil;

	NMSAnvilContainer(int windowId, Inventory inventory, ContainerLevelAccess access, NMSCustomAnvil anvil) {
		super(windowId, inventory, access);
		this.anvil = anvil;
		this.checkReachable = false;
	}

	@Override
	protected boolean mayPickup(Player entityhuman, boolean flag) {
		return true;
	}

	@Override
	public boolean setItemName(String text) {
		boolean accepted = super.setItemName(text);
		anvil.receiveText(text);
		return accepted;
	}
}

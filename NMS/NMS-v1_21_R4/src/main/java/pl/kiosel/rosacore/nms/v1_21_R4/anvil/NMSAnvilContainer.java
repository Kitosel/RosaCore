package pl.kiosel.rosacore.nms.v1_21_R3.anvil;

import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerAnvil;

final class NMSAnvilContainer extends ContainerAnvil {

	private final NMSCustomAnvil anvil;

	NMSAnvilContainer(int windowId, PlayerInventory inventory, ContainerAccess access, NMSCustomAnvil anvil) {
		super(windowId, inventory, access);
		this.anvil = anvil;
		this.checkReachable = false;
	}

	@Override
	public boolean b(EntityHuman player) {
		return true;
	}

	@Override
	public boolean a(String text) {
		boolean accepted = super.a(text);
		anvil.receiveText(text);
		return accepted;
	}
}

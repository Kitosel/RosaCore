package pl.kiosel.rosacore.nms.v1_16_R3.anvil;

import net.minecraft.server.v1_16_R3.ContainerAccess;
import net.minecraft.server.v1_16_R3.ContainerAnvil;
import net.minecraft.server.v1_16_R3.EntityHuman;
import net.minecraft.server.v1_16_R3.PlayerInventory;

final class NMSAnvilContainer extends ContainerAnvil {

	private final NMSCustomAnvil anvil;

	NMSAnvilContainer(int windowId, PlayerInventory inventory, ContainerAccess access,
	                  NMSCustomAnvil anvil) {
		super(windowId, inventory, access);
		this.anvil = anvil;
		this.checkReachable = false;
	}

	@Override
	public boolean canUse(EntityHuman player) {
		return true;
	}

	@Override
	public void a(String text) {
		super.a(text);
		anvil.handleTextChange(text == null ? "" : text);
	}
}

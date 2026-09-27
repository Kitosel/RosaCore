package pl.kiosel.rosacore.nms.v1_16_R1.anvil;

import net.minecraft.server.v1_16_R1.ContainerAccess;
import net.minecraft.server.v1_16_R1.ContainerAnvil;
import net.minecraft.server.v1_16_R1.EntityHuman;
import net.minecraft.server.v1_16_R1.PlayerInventory;

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

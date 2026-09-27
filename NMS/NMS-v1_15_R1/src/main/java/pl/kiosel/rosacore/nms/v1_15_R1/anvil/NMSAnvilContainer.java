package pl.kiosel.rosacore.nms.v1_15_R1.anvil;

import net.minecraft.server.v1_15_R1.*;

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

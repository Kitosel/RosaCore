package pl.kiosel.rosacore.nms.v1_13_R2.anvil;

import net.minecraft.server.v1_13_R2.*;

final class NMSAnvilContainer extends ContainerAnvil {

	private final NMSCustomAnvil anvil;

	NMSAnvilContainer(PlayerInventory inventory, World world, BlockPosition position,
	                  EntityHuman player, NMSCustomAnvil anvil) {
		super(inventory, world, position, player);
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

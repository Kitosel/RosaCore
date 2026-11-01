package pl.kiosel.rosacore.nms.v1_21_R3.anvil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import pl.kiosel.rosacore.nms.api.NMSUtils;
import pl.kiosel.rosacore.nms.api.anvil.AbstractCustomAnvil;

final class NMSCustomAnvil extends AbstractCustomAnvil {

	private final ServerPlayer entity;
	private final int windowId;
	private NMSAnvilContainer container;

	NMSCustomAnvil(Player player, ServerPlayer entity) {
		super(player);
		this.entity = entity;

		windowId = entity.nextContainerCounter();
		container = new NMSAnvilContainer(windowId, entity.getInventory(),
				ContainerLevelAccess.create((Level) NMSUtils.getWorldHandle(player), new BlockPos(0, 0, 0)), this);
	}

	@Override
	protected Inventory openContainer() {
		entity.connection.send(new ClientboundOpenScreenPacket(windowId, MenuType.ANVIL, Component.nullToEmpty(NMSUtils.color(getTitle()))));

		entity.containerMenu = container;

		entity.initMenu(container);

		return container.getBukkitView().getTopInventory();
	}

	@Override
	protected void applyRenameText(String text) {
		if (container != null) container.setItemName(text);
	}

	@Override
	protected void releaseContainer() {
		container = null;
	}

	void receiveText(String text) {
		receiveRenameText(text);
	}
}

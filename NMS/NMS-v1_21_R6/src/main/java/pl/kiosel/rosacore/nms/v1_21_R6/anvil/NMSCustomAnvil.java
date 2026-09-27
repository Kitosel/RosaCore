package pl.kiosel.rosacore.nms.v1_21_R5.anvil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R5.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import pl.kiosel.rosacore.nms.api.NMSUtils;
import pl.kiosel.rosacore.nms.api.anvil.AbstractCustomAnvil;

final class NMSCustomAnvil extends AbstractCustomAnvil {

	private NMSAnvilContainer container;

	NMSCustomAnvil(Player player) {
		super(player);
	}

	@Override
	protected Inventory openContainer() {
		ServerPlayer handle = ((CraftPlayer) getPlayer()).getHandle();
		Location location = getPlayer().getLocation();
		BlockPos position = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		int windowId = handle.nextContainerCounter();
		container = new NMSAnvilContainer(windowId, handle.getInventory(),
				ContainerLevelAccess.create(((CraftWorld) getPlayer().getWorld()).getHandle(), position), this);
		handle.connection.send(new ClientboundOpenScreenPacket(
				windowId, MenuType.ANVIL, Component.nullToEmpty(NMSUtils.color(getTitle()))));
		handle.containerMenu = container;
		handle.initMenu(container);
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

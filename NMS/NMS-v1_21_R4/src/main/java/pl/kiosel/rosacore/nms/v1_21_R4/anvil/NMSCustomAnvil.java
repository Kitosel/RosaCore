package pl.kiosel.rosacore.nms.v1_21_R3.anvil;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.Containers;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
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
		EntityPlayer handle = ((CraftPlayer) getPlayer()).getHandle();
		Location location = getPlayer().getLocation();
		BlockPosition position = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		int windowId = handle.nextContainerCounter();
		container = new NMSAnvilContainer(windowId, handle.gi(),
				ContainerAccess.a(((CraftWorld) getPlayer().getWorld()).getHandle(), position), this);
		handle.f.a(new PacketPlayOutOpenWindow(
				windowId, Containers.i, IChatBaseComponent.a(NMSUtils.color(getTitle()))));
		handle.cd = container;
		handle.a(container);
		return container.getBukkitView().getTopInventory();
	}

	@Override
	protected void applyRenameText(String text) {
		if (container != null) container.a(text);
	}

	@Override
	protected void releaseContainer() {
		container = null;
	}

	void receiveText(String text) {
		receiveRenameText(text);
	}
}

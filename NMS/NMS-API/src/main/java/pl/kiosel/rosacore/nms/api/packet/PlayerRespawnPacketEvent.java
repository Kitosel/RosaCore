package pl.kiosel.rosacore.nms.api.packet;

import org.bukkit.entity.Player;

public final class PlayerRespawnPacketEvent extends TabOutboundPacketEvent {

	public PlayerRespawnPacketEvent(Player viewer, boolean rosaCorePacket) {
		super(viewer, rosaCorePacket);
	}
}

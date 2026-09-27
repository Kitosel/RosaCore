package pl.kiosel.rosacore.nms.api.packet;

import org.bukkit.entity.Player;

import java.util.UUID;

public final class PlayerSpawnPacketEvent extends TabOutboundPacketEvent {

	private final UUID spawnedUniqueId;

	public PlayerSpawnPacketEvent(Player viewer, boolean rosaCorePacket, UUID spawnedUniqueId) {
		super(viewer, rosaCorePacket);
		this.spawnedUniqueId = spawnedUniqueId;
	}

	public UUID getSpawnedUniqueId() {
		return spawnedUniqueId;
	}
}

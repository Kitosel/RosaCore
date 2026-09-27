package pl.kiosel.rosacore.nms.api.packet;

public interface TabPacketListener {

	default void onPlayerInfo(PlayerInfoPacketEvent event) {
	}

	default void onPlayerSpawn(PlayerSpawnPacketEvent event) {
	}

	default void onPlayerRespawn(PlayerRespawnPacketEvent event) {
	}
}

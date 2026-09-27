package pl.kiosel.rosacore.nms.v1_17_R1.server;

import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.nms.api.server.NmsServer;

public class ServerImpl implements NmsServer {

	@Override
	public double[] getRecentTps() {
		return ((CraftServer) Bukkit.getServer()).getServer().recentTps;
	}

	@Override
	public double getTpsInLastMinute() {
		return getRecentTps()[0];
	}

	@Override
	public void instantRespawn(Player player) {
		if (player.isOnline()) {
			CraftPlayer craftPlayer = (CraftPlayer) player;
			craftPlayer.getHandle().connection.send(new ClientboundGameEventPacket(
					ClientboundGameEventPacket.IMMEDIATE_RESPAWN,
					1.0f
			));
		}
	}

	@Override
	public int getReloadCount() {
		return ((CraftServer) Bukkit.getServer()).reloadCount;
	}
}

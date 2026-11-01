package pl.kiosel.rosacore.nms.v26.server;

import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.nms.api.server.NmsServer;
import pl.kiosel.rosacore.nms.api.server.ServerTps;

public class ServerImpl implements NmsServer {

	@Override
	public double[] getRecentTps() {
		try {
			return ((CraftServer) Bukkit.getServer()).getServer().recentTps;
		} catch (Throwable e) {
			try {
				CraftServer server = (CraftServer) Bukkit.getServer();
				if (ServerTps.read(server, server.getServer()) != null) {
					return ServerTps.read(server, server.getServer());
				}
			} catch (Throwable ex) {
				throw new RuntimeException("Unable to get recent TPS", ex);
			}
		}
		return new double[0];
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

package pl.kiosel.rosacore.nms.v1_18_R2.server;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
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
	public int getReloadCount() {
		return ((CraftServer) Bukkit.getServer()).reloadCount;
	}

	@Override
	public int getPlayerPing(Player player) {
		Preconditions.checkNotNull(player, "player can't be null!");
		return ((CraftPlayer) player).getHandle().e;
	}
}

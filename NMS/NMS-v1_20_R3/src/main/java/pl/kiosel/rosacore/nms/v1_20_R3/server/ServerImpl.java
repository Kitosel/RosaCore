package pl.kiosel.rosacore.nms.v1_17_R1.server;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;
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
}

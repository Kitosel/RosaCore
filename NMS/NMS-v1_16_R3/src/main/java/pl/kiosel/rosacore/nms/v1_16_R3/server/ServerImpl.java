package pl.kiosel.rosacore.nms.v1_16_R3.server;

import com.google.common.base.Preconditions;
import net.minecraft.server.v1_16_R3.PacketPlayInClientCommand;
import net.minecraft.server.v1_16_R3.PlayerConnection;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
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
	public void instantRespawn(Player player) {
		if (player.isOnline()) {
			CraftPlayer craftPlayer = (CraftPlayer) player;
			PlayerConnection connection = craftPlayer.getHandle().playerConnection;
			PacketPlayInClientCommand respawnPacket = new PacketPlayInClientCommand(PacketPlayInClientCommand.EnumClientCommand.PERFORM_RESPAWN);
			connection.a(respawnPacket);
		}
	}

	@Override
	public int getPlayerPing(Player player) {
		Preconditions.checkNotNull(player, "player can't be null!");
		return ((CraftPlayer) player).getHandle().ping;
	}
}

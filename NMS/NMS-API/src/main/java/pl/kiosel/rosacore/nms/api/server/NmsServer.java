package pl.kiosel.rosacore.nms.api.server;

import org.bukkit.entity.Player;

public interface Server {

    double[] getRecentTps();
    double getTpsInLastMinute();
    int getReloadCount();
    int getPlayerPing(Player player);

}

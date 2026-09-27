package pl.kiosel.rosacore.nms.api.server;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public interface NmsServer {

	double[] getRecentTps();

	double getTpsInLastMinute();

	int getReloadCount();

	void instantRespawn(Player player);

	default int getPlayerPing(Player player) {
		try {
			Class<?> playerClass = player.getClass();
			Method getPingMethod = playerClass.getMethod("getPing");
			return (int) getPingMethod.invoke(player);

		} catch (NoSuchMethodException e) {
			return -1;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
}
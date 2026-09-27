package pl.kiosel.rosacore.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabUtils {

	private TabUtils() {
	}

	public static List<String> onlinePlayers() {
		List<String> players = new ArrayList<>();

		for (Player ps : Bukkit.getOnlinePlayers()) {
			players.add(ps.getName());
		}
		return players;
	}

	public static List<String> offlinePlayers() {
		List<String> players = new ArrayList<>();

		for (OfflinePlayer ps : Bukkit.getOfflinePlayers()) {
			players.add(ps.getName());
		}
		return players;
	}

	public static List<String> returnEmpty() {
		return Collections.emptyList();
	}

	public static List<String> returnWith(String args, List<String> sa) {
		return StringUtil.copyPartialMatches(args, sa, new ArrayList<>());
	}
}

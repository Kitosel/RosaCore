package pl.kiosel.rosacore.utils;

import org.bukkit.Bukkit;

import java.util.*;

public final class TabUtils {

	private TabUtils() {
	}

	public static List<String> returnEmpty() {
		return Collections.emptyList();
	}

	public static List<String> returnWith(String input, Collection<String> candidates) {
		if (candidates == null || candidates.isEmpty()) return Collections.emptyList();
		String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
		List<String> result = new ArrayList<>();
		for (String candidate : candidates) {
			if (candidate != null && candidate.toLowerCase(Locale.ROOT).startsWith(prefix)) {
				result.add(candidate);
			}
		}
		return result;
	}

	public static List<String> onlinePlayers() {
		List<String> result = new ArrayList<>();
		Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
		return result;
	}
}

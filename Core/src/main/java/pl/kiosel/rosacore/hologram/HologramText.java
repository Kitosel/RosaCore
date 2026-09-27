package pl.kiosel.rosacore.hologram;

import org.bukkit.ChatColor;

final class HologramText {

	private HologramText() {
	}

	static String truncate(String text, int limit) {
		if (text == null || text.length() <= limit) return text == null ? "" : text;
		String result = text.substring(0, limit);
		if (!result.isEmpty() && result.charAt(result.length() - 1) == ChatColor.COLOR_CHAR) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}
}

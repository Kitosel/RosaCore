package pl.kiosel.rosacore.scoreboard;

import org.bukkit.ChatColor;

final class ScoreboardText {

	private ScoreboardText() {
	}

	static Parts split(String text, int partLimit) {
		if (partLimit < 2) throw new IllegalArgumentException("partLimit must be at least 2");
		String value = text == null ? "" : text;
		String prefix = safePrefix(value, partLimit);
		String remaining = value.substring(prefix.length());
		String continuation = ChatColor.getLastColors(prefix);
		String suffix = safePrefix(continuation + remaining, partLimit);
		return new Parts(prefix, suffix);
	}

	static String truncate(String text, int limit) {
		if (limit < 0) throw new IllegalArgumentException("limit cannot be negative");
		return safePrefix(text == null ? "" : text, limit);
	}

	private static String safePrefix(String text, int limit) {
		if (text.length() <= limit) return text;
		int end = limit;
		if (end > 0 && text.charAt(end - 1) == ChatColor.COLOR_CHAR) end--;
		return text.substring(0, end);
	}

	static final class Parts {

		private final String prefix;
		private final String suffix;

		Parts(String prefix, String suffix) {
			this.prefix = prefix;
			this.suffix = suffix;
		}

		String getPrefix() {
			return this.prefix;
		}

		String getSuffix() {
			return this.suffix;
		}
	}
}

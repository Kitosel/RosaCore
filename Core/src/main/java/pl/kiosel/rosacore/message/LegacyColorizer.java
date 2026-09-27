package pl.kiosel.rosacore.message;

import lombok.Getter;
import org.bukkit.ChatColor;
import pl.kiosel.rosacore.version.MinecraftVersion;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public final class LegacyColorizer {

	private static final MinecraftVersion HEX_VERSION = MinecraftVersion.of(1, 16, 0);
	private static final Pattern HEX_COLOR = Pattern.compile("(?i)&#([0-9a-f]{6})");
	private static final LegacyColor[] LEGACY_COLORS = {
			new LegacyColor('0', 0x000000), new LegacyColor('1', 0x0000AA),
			new LegacyColor('2', 0x00AA00), new LegacyColor('3', 0x00AAAA),
			new LegacyColor('4', 0xAA0000), new LegacyColor('5', 0xAA00AA),
			new LegacyColor('6', 0xFFAA00), new LegacyColor('7', 0xAAAAAA),
			new LegacyColor('8', 0x555555), new LegacyColor('9', 0x5555FF),
			new LegacyColor('a', 0x55FF55), new LegacyColor('b', 0x55FFFF),
			new LegacyColor('c', 0xFF5555), new LegacyColor('d', 0xFF55FF),
			new LegacyColor('e', 0xFFFF55), new LegacyColor('f', 0xFFFFFF)
	};

	private final boolean hexSupported;

	public LegacyColorizer(boolean hexSupported) {
		this.hexSupported = hexSupported;
	}

	public static LegacyColorizer forVersion(MinecraftVersion version) {
		Objects.requireNonNull(version, "version");
		return new LegacyColorizer(version.isAtLeast(HEX_VERSION));
	}

	public static LegacyColorizer legacyOnly() {
		return new LegacyColorizer(false);
	}

	public String colorize(String text) {
		Objects.requireNonNull(text, "text");
		Matcher matcher = HEX_COLOR.matcher(text);
		StringBuffer converted = new StringBuffer();
		while (matcher.find()) {
			int rgb = Integer.parseInt(matcher.group(1), 16);
			String replacement = this.hexSupported
					? modernHex(matcher.group(1))
					: "&" + nearestLegacy(rgb).code;
			matcher.appendReplacement(converted, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(converted);
		return ChatColor.translateAlternateColorCodes('&', converted.toString());
	}

	public String stripColors(String text) {
		return ChatColor.stripColor(this.colorize(text));
	}

	private static String modernHex(String hex) {
		StringBuilder result = new StringBuilder("§x");
		for (int index = 0; index < hex.length(); index++) {
			result.append('§').append(hex.charAt(index));
		}
		return result.toString();
	}

	private static LegacyColor nearestLegacy(int rgb) {
		int red = (rgb >> 16) & 0xFF;
		int green = (rgb >> 8) & 0xFF;
		int blue = rgb & 0xFF;
		LegacyColor nearest = LEGACY_COLORS[0];
		long nearestDistance = Long.MAX_VALUE;
		for (LegacyColor color : LEGACY_COLORS) {
			int candidateRed = (color.rgb >> 16) & 0xFF;
			int candidateGreen = (color.rgb >> 8) & 0xFF;
			int candidateBlue = color.rgb & 0xFF;
			long redDistance = red - candidateRed;
			long greenDistance = green - candidateGreen;
			long blueDistance = blue - candidateBlue;
			long distance = redDistance * redDistance
					+ greenDistance * greenDistance + blueDistance * blueDistance;
			if (distance < nearestDistance) {
				nearest = color;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	private static final class LegacyColor {

		private final char code;
		private final int rgb;

		private LegacyColor(char code, int rgb) {
			this.code = code;
			this.rgb = rgb;
		}
	}
}

package pl.kiosel.rosacore.utils;

import lombok.Getter;
import org.bukkit.ChatColor;
import pl.kiosel.rosacore.compatibility.ZColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorUtils {

	private static final Map<ZColor, ColorSet<Integer, Integer, Integer>> COLOR_MAP = new HashMap<>();

	static {
		COLOR_MAP.put(ZColor.BLACK, new ColorSet<>(0, 0, 0));
		COLOR_MAP.put(ZColor.DARK_BLUE, new ColorSet<>(0, 0, 170));
		COLOR_MAP.put(ZColor.DARK_GREEN, new ColorSet<>(0, 170, 0));
		COLOR_MAP.put(ZColor.DARK_AQUA, new ColorSet<>(0, 170, 170));
		COLOR_MAP.put(ZColor.DARK_RED, new ColorSet<>(170, 0, 0));
		COLOR_MAP.put(ZColor.DARK_PURPLE, new ColorSet<>(170, 0, 170));
		COLOR_MAP.put(ZColor.GOLD, new ColorSet<>(255, 170, 0));
		COLOR_MAP.put(ZColor.GRAY, new ColorSet<>(170, 170, 170));
		COLOR_MAP.put(ZColor.DARK_GRAY, new ColorSet<>(85, 85, 85));
		COLOR_MAP.put(ZColor.BLUE, new ColorSet<>(85, 85, 255));
		COLOR_MAP.put(ZColor.GREEN, new ColorSet<>(85, 255, 85));
		COLOR_MAP.put(ZColor.AQUA, new ColorSet<>(85, 255, 255));
		COLOR_MAP.put(ZColor.RED, new ColorSet<>(255, 85, 85));
		COLOR_MAP.put(ZColor.LIGHT_PURPLE, new ColorSet<>(255, 85, 255));
		COLOR_MAP.put(ZColor.YELLOW, new ColorSet<>(255, 255, 85));
		COLOR_MAP.put(ZColor.WHITE, new ColorSet<>(255, 255, 255));
	}

	@Getter
	private static class ColorSet<R, G, B> {
		R red;
		G green;
		B blue;

		ColorSet(R red, G green, B blue) {
			this.red = red;
			this.green = green;
			this.blue = blue;
		}

	}

	public static ZColor fromRGB(int r, int g, int b) {
		if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
			throw new IllegalArgumentException("RGB values must be between 0 and 255");
		}
		ZColor closest = null;
		long closestDistance = Long.MAX_VALUE;
		for (ZColor color : ZColor.values()) {
			ColorSet<Integer, Integer, Integer> set = COLOR_MAP.get(color);
			if (set == null) continue;
			long red = r - set.getRed();
			long green = g - set.getGreen();
			long blue = b - set.getBlue();
			long distance = red * red + green * green + blue * blue;
			if (distance < closestDistance) {
				closestDistance = distance;
				closest = color;
			}
		}
		return closest;
	}

	public static String color(String message) {
		return message == null ? null : ChatColor.translateAlternateColorCodes('&', message);
	}

	public static String tl(String message) {
		return color(message);
	}

	public static List<String> listColor(List<String> lore) {
		List<String> colored = new ArrayList<>(lore);
		colored.replaceAll(ColorUtils::color);
		return colored;
	}
}

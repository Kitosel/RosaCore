package pl.kiosel.rosacore.compatibility;

import lombok.Getter;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum ZColor {
	BLACK('0', ChatColor.BLACK, true),
	DARK_BLUE('1', ChatColor.DARK_BLUE, true),
	DARK_GREEN('2', ChatColor.DARK_GREEN, true),
	DARK_AQUA('3', ChatColor.DARK_AQUA, true),
	DARK_RED('4', ChatColor.DARK_RED, true),
	DARK_PURPLE('5', ChatColor.DARK_PURPLE, true),
	GOLD('6', ChatColor.GOLD, true),
	GRAY('7', ChatColor.GRAY, true),
	DARK_GRAY('8', ChatColor.DARK_GRAY, true),
	BLUE('9', ChatColor.BLUE, true),
	GREEN('a', ChatColor.GREEN, true),
	AQUA('b', ChatColor.AQUA, true),
	RED('c', ChatColor.RED, true),
	LIGHT_PURPLE('d', ChatColor.LIGHT_PURPLE, true),
	YELLOW('e', ChatColor.YELLOW, true),
	WHITE('f', ChatColor.WHITE, true),
	OBFUSCATED('k', ChatColor.MAGIC, false),
	BOLD('l', ChatColor.BOLD, false),
	STRIKETHROUGH('m', ChatColor.STRIKETHROUGH, false),
	UNDERLINED('n', ChatColor.UNDERLINE, false),
	ITALIC('o', ChatColor.ITALIC, false),
	RESET('r', ChatColor.RESET, false);

	private final char code;
	private final ChatColor chatColor;
	private final boolean isColor;

	private static final Map<Character, ZColor> BY_CHAR = new HashMap<>();

	ZColor(char code, ChatColor chatColor, boolean isColor) {
		this.code = code;
		this.chatColor = chatColor;
		this.isColor = isColor;
	}

	static {
		for (ZColor color : values()) {
			BY_CHAR.put(color.code, color);
		}
	}

	public static ZColor getByChar(char code) {
		return BY_CHAR.get(code);
	}

}

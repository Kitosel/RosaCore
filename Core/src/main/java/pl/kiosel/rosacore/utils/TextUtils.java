package pl.kiosel.rosacore.utils;

import org.bukkit.configuration.ConfigurationSection;
import pl.kiosel.rosacore.config.ConfigView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class TextUtils {

	private TextUtils() {
	}

	public static boolean isEmpty(String value) {
		return value == null || value.isEmpty();
	}

	public static List<String> fromString(String value) {
		return isEmpty(value) ? new ArrayList<>() : new ArrayList<>(Arrays.asList(value.split(";")));
	}

	public static String join(Collection<String> values) {
		return join(values, ";");
	}

	public static String join(Collection<String> values, String separator) {
		return String.join(separator, values);
	}

	public static String readText(ConfigView config, String path, String fallback) {
		String value = readOptionalText(config, path);
		return value == null ? fallback : value;
	}

	public static String readOptionalText(ConfigView config, String path) {
		if (!config.contains(path)) return null;
		List<String> lines = config.getStringList(path);
		if (!lines.isEmpty()) return String.join("\n", lines);
		return config.getString(path, "");
	}

	public static String readText(ConfigurationSection config, String path, String fallback) {
		String value = readOptionalText(config, path);
		return value == null ? fallback : value;
	}

	public static String readOptionalText(ConfigurationSection config, String path) {
		if (!config.contains(path)) return null;
		return config.isList(path) ? String.join("\n", config.getStringList(path)) : config.getString(path, "");
	}
}

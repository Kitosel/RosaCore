package pl.kiosel.rosacore.command;

import org.bukkit.command.CommandSender;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.utils.ColorUtils;

import java.util.*;
import java.util.logging.Level;

final class CommandSupport {

	private CommandSupport() {
	}

	public static String[] safeArgs(String[] args) {
		return args == null ? new String[0] : args;
	}

	public static String[] tail(String[] args) {
		return args.length < 2 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
	}

	public static String normalizeName(String value) {
		Objects.requireNonNull(value, "command");
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty() || normalized.indexOf(' ') >= 0) {
			throw new IllegalArgumentException("Command name cannot be empty or contain spaces");
		}
		return normalized;
	}

	public static List<String> normalizeAliases(List<String> aliases) {
		Objects.requireNonNull(aliases, "aliases");
		List<String> normalized = new ArrayList<>();
		for (String alias : aliases) normalized.add(normalizeName(alias));
		return normalized;
	}

	public static String emptyToNull(String value) {
		return value == null || value.trim().isEmpty() ? null : value.trim();
	}

	public static void sendOptionalMessage(CommandSender sender, String message) {
		if (message != null && !message.trim().isEmpty()) {
			sender.sendMessage(ColorUtils.color(message));
		}
	}

	public static void reportError(RosaPlugin plugin, CommandSender sender, String command,
								   String action, RuntimeException exception) {
		String message = "Error while " + action + " command '" + command + "'";
		sender.sendMessage(ColorUtils.color("&cError while " + action + " command &7'&e" + command + "&7' &cCheck console for more information."));
		plugin.getLogger().log(Level.SEVERE, message, exception);
		if (plugin.getDebug() != null) plugin.getDebug().debug(message, exception);
	}
}

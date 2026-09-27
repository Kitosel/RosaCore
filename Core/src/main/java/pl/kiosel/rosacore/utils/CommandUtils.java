package pl.kiosel.rosacore.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import pl.kiosel.rosacore.command.RosaSubCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandUtils {

	private CommandUtils() {
	}

	public static void help(List<RosaSubCommand> subCommands, CommandSender sender, String label, String c1, String c2, String c3, String nosubcommand) {
		String options = subCommands.stream()
				.filter(sub -> hasPermission(sender, sub.getPermission()))
				.map(RosaSubCommand::getName)
				.collect(Collectors.joining(c1 + "|" + c2));

		if (!options.isEmpty()) {
			sender.sendMessage(ColorUtils.color(c1 + "/" + label + c1 + " <" + c2 + options + c1 + ">"));
		} else {
			sender.sendMessage(ColorUtils.color(c1 + "/" + label + " - " + c3 + nosubcommand));
		}
	}

	public static List<String> onlinePlayers() {
		return onlinePlayers(null);
	}

	public static List<String> onlinePlayers(CommandSender sender) {
		List<String> players = new ArrayList<>();
		Player viewer = sender instanceof Player ? (Player) sender : null;

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (viewer == null || viewer.canSee(player)) {
				players.add(player.getName());
			}
		}
		return players;
	}

	public static List<String> onlinePlayers(CommandSender sender, String input) {
		return returnWith(input, onlinePlayers(sender));
	}

	public static List<String> offlinePlayers() {
		List<String> players = new ArrayList<>();

		for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
			String name = player.getName();
			if (name != null && !name.trim().isEmpty()) players.add(name);
		}
		return players;
	}

	public static List<String> offlinePlayers(String input) {
		return returnWith(input, offlinePlayers());
	}

	public static List<String> returnEmpty() {
		return Collections.emptyList();
	}

	public static List<String> returnWith(String args, List<String> sa) {
		return returnWith(args, (Iterable<String>) sa);
	}

	public static List<String> returnWith(String input, Iterable<String> candidates) {
		if (candidates == null) return Collections.emptyList();

		List<String> sanitized = new ArrayList<>();
		for (String candidate : candidates) {
			if (candidate == null) continue;
			sanitized.add(candidate);
		}

		List<String> matches = StringUtil.copyPartialMatches(
				input == null ? "" : input,
				sanitized,
				new ArrayList<>()
		);
		matches.sort(String.CASE_INSENSITIVE_ORDER);
		return matches;
	}

	private static boolean hasPermission(CommandSender sender, String permission) {
		return permission == null || permission.trim().isEmpty() || sender.hasPermission(permission);
	}
}

package pl.kiosel.rosacore.command;

import org.bukkit.command.CommandSender;
import pl.kiosel.rosacore.utils.CommandUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public interface TabCompleteUtils {

	List<String> EMPTY = Collections.emptyList();

	default List<String> onlinePlayers() {
		return CommandUtils.onlinePlayers();
	}

	default List<String> onlinePlayers(CommandSender sender) {
		return CommandUtils.onlinePlayers(sender);
	}

	default List<String> onlinePlayers(CommandSender sender, String input) {
		return CommandUtils.onlinePlayers(sender, input);
	}

	default List<String> onlinePlayers(CommandSender sender, String[] args) {
		return onlinePlayers(sender, lastArgument(args));
	}

	default List<String> offlinePlayers() {
		return CommandUtils.offlinePlayers();
	}

	default List<String> offlinePlayers(String input) {
		return CommandUtils.offlinePlayers(input);
	}

	default List<String> offlinePlayers(String[] args) {
		return offlinePlayers(lastArgument(args));
	}

	default List<String> complete(String... args) {
		return complete(Arrays.asList(args));
	}

	default List<String> complete(List<String> args) {
		return safeList(args);
	}

	default List<String> complete(String input, Iterable<String> candidates) {
		return CommandUtils.returnWith(input, candidates);
	}

	default List<String> complete(String[] args, Iterable<String> candidates) {
		return complete(lastArgument(args), candidates);
	}

	default List<String> safeList(List<String> values) {
		return values == null ? Collections.emptyList() : values;
	}

	default String lastArgument(String[] args) {
		return args == null || args.length == 0 || args[args.length - 1] == null
				? ""
				: args[args.length - 1];
	}

	default String getPlayerOnlyMessage() {
		return "&cThis subcommand can only be used by a player.";
	}

	default String getNoPermissionMessage() {
		return "&cYou do not have permission to use this subcommand.";
	}

	default String getUnavailableMessage() {
		return "&cYou cannot use this subcommand right now.";
	}

	default String getUnknownSubCommandMessage() {
		return "&cUnknown subcommand. Use &e/{label}&c.";
	}

}

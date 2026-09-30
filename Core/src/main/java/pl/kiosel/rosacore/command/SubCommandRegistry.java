package pl.kiosel.rosacore.command;

import org.bukkit.command.CommandSender;
import pl.kiosel.rosacore.utils.CommandUtils;

import java.util.*;

final class SubCommandRegistry {

	private volatile Map<String, RosaSubCommand> commands = Collections.emptyMap();
	private volatile List<RosaSubCommand> values = Collections.emptyList();

	synchronized void add(RosaSubCommand subCommand) {
		Objects.requireNonNull(subCommand, "subCommand");
		List<RosaSubCommand> updated = new ArrayList<>(values);
		updated.add(subCommand);
		replace(updated);
	}

	synchronized void replace(Iterable<? extends RosaSubCommand> subCommands) {
		Objects.requireNonNull(subCommands, "subCommands");
		Map<String, RosaSubCommand> refreshed = new LinkedHashMap<>();
		List<RosaSubCommand> unique = new ArrayList<>();

		for (RosaSubCommand subCommand : subCommands) {
			Objects.requireNonNull(subCommand, "subCommand");
			List<String> keys = new ArrayList<>();
			keys.add(CommandSupport.normalizeName(subCommand.getName()));
			for (String alias : subCommand.getAliases()) {
				keys.add(CommandSupport.normalizeName(alias));
			}

			for (String key : keys) {
				RosaSubCommand previous = refreshed.get(key);
				if (previous != null && previous != subCommand) {
					throw new IllegalArgumentException("Duplicate subcommand or alias: " + key);
				}
			}
			for (String key : keys) refreshed.put(key, subCommand);
			if (!unique.contains(subCommand)) unique.add(subCommand);
		}

		this.commands = Collections.unmodifiableMap(refreshed);
		this.values = Collections.unmodifiableList(unique);
	}

	RosaSubCommand find(String input) {
		if (input == null) return null;
		return commands.get(input.trim().toLowerCase(Locale.ROOT));
	}

	boolean isEmpty() {
		return commands.isEmpty();
	}

	List<RosaSubCommand> values() {
		return values;
	}

	List<String> completeNames(CommandSender sender, String input) {
		List<String> names = new ArrayList<>();
		for (RosaSubCommand subCommand : values()) {
			if (subCommand.canShow(sender)) names.add(subCommand.getName());
		}
		return CommandUtils.returnWith(input, names);
	}
}

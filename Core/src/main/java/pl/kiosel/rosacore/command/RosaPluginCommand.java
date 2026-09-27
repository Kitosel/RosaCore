package pl.kiosel.rosacore.command;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.message.MessageCatalog;
import pl.kiosel.rosacore.utils.ColorUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public abstract class RosaPluginCommand implements CommandExecutor, TabCompleter, TabCompleteUtils {

	protected final RosaPlugin plugin;
	protected final PluginCommand command;
	private final SubCommandRegistry subCommands = new SubCommandRegistry();

	protected RosaPluginCommand(RosaPlugin plugin, String commandName) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.command = plugin.getCommand(Objects.requireNonNull(commandName, "commandName"));
		if (this.command == null) {
			throw new IllegalArgumentException("Command '" + commandName + "' is missing from plugin.yml");
		}
		this.command.setExecutor(this);
		this.command.setTabCompleter(this);
	}

	protected final void addSubCommand(RosaSubCommand subCommand) {
		subCommands.add(subCommand);
	}

	protected final void addSubcommand(RosaSubCommand subCommand) {
		addSubCommand(subCommand);
	}

	public final List<RosaSubCommand> getSubCommands() {
		return subCommands.values();
	}

	public boolean isPlayerOnly() {
		return false;
	}

	public boolean isAvailable(CommandSender sender) {
		return true;
	}

	@Override
	public final boolean onCommand(CommandSender sender, Command ignored, String label, String[] args) {
		String[] safeArgs = CommandSupport.safeArgs(args);
		if (isPlayerOnly() && !(sender instanceof Player)) {
			sender.sendMessage(ColorUtils.color(getPlayerOnlyMessage()));
			return true;
		}
		try {
			if (!isAvailable(sender)) {
				CommandSupport.sendOptionalMessage(sender, getUnavailableMessage());
				return true;
			}
			if (safeArgs.length > 0) {
				RosaSubCommand subCommand = subCommands.find(safeArgs[0]);
				if (subCommand != null) {
					if (!subCommand.canUse(sender, true)) return true;
					subCommand.dispatch(sender, CommandSupport.tail(safeArgs));
					return true;
				}
				if (!subCommands.isEmpty()) {
					sender.sendMessage(ColorUtils.color(getUnknownSubCommandMessage()
							.replace("{label}", label)));
					return true;
				}
			}
			return onExecute(sender, label, safeArgs);
		} catch (RuntimeException exception) {
			CommandSupport.reportError(plugin, sender, command.getName(), "executing", exception);
			return false;
		}
	}

	@Override
	public final List<String> onTabComplete(CommandSender sender, Command ignored,
											String alias, String[] args) {
		String[] safeArgs = CommandSupport.safeArgs(args);
		if (isPlayerOnly() && !(sender instanceof Player)) return Collections.emptyList();
		try {
			if (!isAvailable(sender)) return Collections.emptyList();
			if (!subCommands.isEmpty() && safeArgs.length == 1) {
				return subCommands.completeNames(sender, safeArgs[0]);
			}
			if (safeArgs.length > 1) {
				RosaSubCommand subCommand = subCommands.find(safeArgs[0]);
				if (subCommand != null && subCommand.canShow(sender)) {
					String[] subArgs = CommandSupport.tail(safeArgs);
					return subCommand.completeArguments(sender, subArgs);
				}
			}
			return complete(safeArgs, safeList(onTabComplete(sender, safeArgs)));
		} catch (RuntimeException exception) {
			CommandSupport.reportError(plugin, sender, command.getName(), "tab-completing", exception);
			return Collections.emptyList();
		}
	}

	public abstract boolean onExecute(CommandSender sender, String label, String[] args);

	public abstract List<String> onTabComplete(CommandSender sender, String[] args);

	protected void sendOptionalMessage(CommandSender sender, String message) {
		CommandSupport.sendOptionalMessage(sender, message);
	}

	public MessageCatalog getMessage() {
		return plugin.getMessages();
	}
}

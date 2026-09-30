package pl.kiosel.rosacore.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.message.MessageCatalog;
import pl.kiosel.rosacore.utils.ColorUtils;
import pl.kiosel.rosacore.utils.CommandUtils;

import java.util.*;

public abstract class RosaCommand extends Command implements TabCompleteUtils {

	protected final String command;
	protected final RosaPlugin plugin;
	protected String permission;
	private final SubCommandRegistry subCommands = new SubCommandRegistry();

	protected RosaCommand(RosaPlugin plugin, String command, List<String> aliases, String permission) {
		this(plugin, command, aliases);
		setPermission(permission);
	}

	protected RosaCommand(RosaPlugin plugin, String command, List<String> aliases) {
		this(plugin, command);
		setAlias(aliases);
	}

	protected RosaCommand(RosaPlugin plugin, String command) {
		super(CommandSupport.normalizeName(command));
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.command = CommandSupport.normalizeName(command);
		setDescription("Command " + this.command);
		setUsage("/" + this.command);
	}

	public final void setAlias(List<String> aliases) {
		setAliases(CommandSupport.normalizeAliases(aliases));
	}

	public final void setAlias(String... aliases) {
		setAliases(CommandSupport.normalizeAliases(Arrays.asList(aliases)));
	}

	@Override
	public final void setPermission(String permission) {
		this.permission = CommandSupport.emptyToNull(permission);
		super.setPermission(this.permission);
	}

	protected final void addSubCommand(RosaSubCommand subCommand) {
		subCommands.add(subCommand);
	}

	protected final void setSubCommands(Iterable<? extends RosaSubCommand> commands) {
		subCommands.replace(commands);
	}

	protected final void setSubCommands(RosaSubCommand... commands) {
		setSubCommands(Arrays.asList(commands));
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

	public final void register() {
		register(plugin.getName().toLowerCase(Locale.ROOT));
	}

	public final void register(String prefix) {
		plugin.registerCommand(prefix, this);
	}

	@Override
	public final boolean execute(CommandSender sender, String commandLabel, String[] args) {
		Objects.requireNonNull(sender, "sender");
		String[] safeArgs = CommandSupport.safeArgs(args);
		if (!testPermission(sender)) return true;
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
					return onUnknownSubCommand(sender, commandLabel, safeArgs);
				}
			}
			return onExecute(sender, commandLabel, safeArgs);
		} catch (RuntimeException exception) {
			CommandSupport.reportError(plugin, sender, command, "executing", exception);
			return false;
		}
	}

	@Override
	public final List<String> tabComplete(CommandSender sender, String alias, String[] args) {
		Objects.requireNonNull(sender, "sender");
		String[] safeArgs = CommandSupport.safeArgs(args);
		if (!testPermissionSilent(sender) || (isPlayerOnly() && !(sender instanceof Player))) {
			return Collections.emptyList();
		}

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
			CommandSupport.reportError(plugin, sender, command, "tab-completing", exception);
			return Collections.emptyList();
		}
	}

	public abstract boolean onExecute(CommandSender sender, String label, String[] args);

	public abstract List<String> onTabComplete(CommandSender sender, String[] args);

	protected boolean onUnknownSubCommand(CommandSender sender, String label, String[] args) {
		sender.sendMessage(ColorUtils.color(getUnknownSubCommandMessage().replace("{label}", label)));
		return true;
	}

	public void sendHelp(CommandSender sender, String label) {
		this.sendHelp(sender, label, "&7", "&6", "&c");
	}

	public void sendHelp(CommandSender sender, String label, String defaultColor, String subcmdColor, String errorColor) {
		this.sendHelp(sender, label, defaultColor, subcmdColor, errorColor, "No subcommands found.");
	}

	public void sendHelp(CommandSender sender, String label, String defaultColor, String subcmdColor, String errorColor, String nosubcmd) {
		CommandUtils.help(this.getSubCommands(), sender, label, defaultColor, subcmdColor, errorColor, nosubcmd);
	}

	protected void sendOptionalMessage(CommandSender sender, String message) {
		CommandSupport.sendOptionalMessage(sender, message);
	}

	public MessageCatalog getMessage() {
		return plugin.getMessages();
	}
}

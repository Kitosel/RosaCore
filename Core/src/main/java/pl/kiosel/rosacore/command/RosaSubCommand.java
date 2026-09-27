package pl.kiosel.rosacore.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.message.MessageCatalog;
import pl.kiosel.rosacore.utils.ColorUtils;

import java.util.*;

public abstract class RosaSubCommand implements TabCompleteUtils {

	protected final RosaPlugin plugin;
	private final SubCommandRegistry subCommands = new SubCommandRegistry();

	protected RosaSubCommand(RosaPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	public abstract String getName();

	public abstract String getDescription();

	public abstract String getUsage();

	public abstract String getPermission();

	public abstract void run(CommandSender sender, String[] args);

	protected final void addSubCommand(RosaSubCommand subCommand) {
		if (subCommand == this) {
			throw new IllegalArgumentException("A subcommand cannot contain itself");
		}
		this.subCommands.add(subCommand);
	}

	protected final void setSubCommands(Iterable<? extends RosaSubCommand> commands) {
		Objects.requireNonNull(commands, "commands");
		List<RosaSubCommand> updated = new ArrayList<>();
		for (RosaSubCommand command : commands) {
			if (command == this) {
				throw new IllegalArgumentException("A subcommand cannot contain itself");
			}
			updated.add(command);
		}
		this.subCommands.replace(updated);
	}

	protected final void setSubCommands(RosaSubCommand... commands) {
		setSubCommands(Arrays.asList(commands));
	}

	public final List<RosaSubCommand> getSubCommands() {
		return this.subCommands.values();
	}

	public List<String> getAliases() {
		return Collections.emptyList();
	}

	public boolean isPlayerOnly() {
		return false;
	}

	public boolean isAvailable(CommandSender sender) {
		return true;
	}

	public boolean isVisible(CommandSender sender) {
		return true;
	}

	public List<String> tabComplete(CommandSender sender, String[] args) {
		return Collections.emptyList();
	}

	protected void onUnknownSubCommand(CommandSender sender, String input) {
		sendOptionalMessage(sender, getUnknownSubCommandMessage().replace("{label}", getName()));
	}

	protected void sendUsage(CommandSender sender) {
		sendUsage(sender, "&7Usage: &c", "");
	}

	protected void sendUsage(CommandSender sender, String prefix, String suffix) {
		sender.sendMessage(ColorUtils.color(prefix + getUsage().replace("%name%", getName()) + suffix));
	}

	final boolean canUse(CommandSender sender, boolean sendMessage) {
		String permission = getPermission();
		if (permission != null && !permission.trim().isEmpty() && !sender.hasPermission(permission)) {
			if (sendMessage) sendOptionalMessage(sender, getNoPermissionMessage());
			return false;
		}
		if (isPlayerOnly() && !(sender instanceof Player)) {
			if (sendMessage) sendOptionalMessage(sender, getPlayerOnlyMessage());
			return false;
		}
		if (!isAvailable(sender)) {
			if (sendMessage) sendOptionalMessage(sender, getUnavailableMessage());
			return false;
		}
		return true;
	}

	final boolean canShow(CommandSender sender) {
		return canUse(sender, false) && isVisible(sender);
	}

	final void dispatch(CommandSender sender, String[] args) {
		String[] safeArgs = CommandSupport.safeArgs(args);
		if (safeArgs.length > 0) {
			RosaSubCommand child = this.subCommands.find(safeArgs[0]);
			if (child != null) {
				if (child.canUse(sender, true)) {
					child.dispatch(sender, CommandSupport.tail(safeArgs));
				}
				return;
			}
			if (!this.subCommands.isEmpty()) {
				onUnknownSubCommand(sender, safeArgs[0]);
				return;
			}
		}
		run(sender, safeArgs);
	}

	final List<String> completeArguments(CommandSender sender, String[] args) {
		String[] safeArgs = CommandSupport.safeArgs(args);
		if (!this.subCommands.isEmpty()) {
			if (safeArgs.length <= 1) {
				return this.subCommands.completeNames(sender, lastArgument(safeArgs));
			}
			RosaSubCommand child = this.subCommands.find(safeArgs[0]);
			if (child == null || !child.canShow(sender)) {
				return Collections.emptyList();
			}
			return child.completeArguments(sender, CommandSupport.tail(safeArgs));
		}
		return complete(safeArgs, safeList(tabComplete(sender, safeArgs)));
	}

	protected void sendOptionalMessage(CommandSender sender, String message) {
		CommandSupport.sendOptionalMessage(sender, message);
	}

	final boolean matches(String input) {
		if (getName().equalsIgnoreCase(input)) return true;
		for (String alias : getAliases()) {
			if (alias != null && alias.equalsIgnoreCase(input)) return true;
		}
		return false;
	}

	public MessageCatalog getMessage() {
		return plugin.getMessages();
	}
}

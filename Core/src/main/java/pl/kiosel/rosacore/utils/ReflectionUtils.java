package pl.kiosel.rosacore.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReflectionUtils {

	public final static double JAVA_VERSION = getVersion();
	private static final String system_os = System.getProperty("os.name").toLowerCase();

	public static boolean registerCommand(String prefix, Command command) {
		try {
			return getCommandMap().register(prefix, command);
		} catch (ReflectiveOperationException e) {
			Bukkit.getLogger().log(java.util.logging.Level.SEVERE,
					"Could not access Bukkit command map while registering /" + command.getName(), e);
			return false;
		}
	}

	public static void syncCommands() {
		try {
			Method method = Bukkit.getServer().getClass().getMethod("syncCommands");
			method.invoke(Bukkit.getServer());
		} catch (NoSuchMethodException exception) {
			updatePlayerCommands();
		} catch (ReflectiveOperationException exception) {
			Bukkit.getLogger().log(java.util.logging.Level.WARNING,
					"Could not synchronize the Bukkit command dispatcher", exception);
			updatePlayerCommands();
		}
	}

	public static void unregisterCommand(Map<String, Command> knownCommands, Command command) throws ReflectiveOperationException {
		if (command == null) {
			return;
		}
		List<String> registeredLabels = new ArrayList<>();
		for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
			if (entry.getValue() == command) {
				registeredLabels.add(entry.getKey());
			}
		}
		for (String label : registeredLabels) {
			knownCommands.remove(label, command);
		}
		command.unregister(getCommandMap());
	}

	public static CommandMap getCommandMap() throws ReflectiveOperationException {
		try {
			Method method = Bukkit.getServer().getClass().getMethod("getCommandMap");
			Object commandMap = method.invoke(Bukkit.getServer());
			if (commandMap instanceof CommandMap) return (CommandMap) commandMap;
		} catch (NoSuchMethodException ignored) {
		}
		Object pluginManager = Bukkit.getServer().getPluginManager();
		Field field = findField(pluginManager.getClass(), "commandMap");
		return (CommandMap) field.get(pluginManager);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Command> getKnownCommands(CommandMap commandMap)
			throws ReflectiveOperationException {
		Field field = findField(commandMap.getClass(), "knownCommands");
		return (Map<String, Command>) field.get(commandMap);
	}

	private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
		Class<?> current = type;
		while (current != null) {
			try {
				Field field = current.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException ignored) {
				current = current.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static void updatePlayerCommands() {
		try {
			Method method = Class.forName("org.bukkit.entity.Player").getMethod("updateCommands");
			for (Object player : Bukkit.getOnlinePlayers()) method.invoke(player);
		} catch (ReflectiveOperationException ignored) {
		}
	}

	private static double getVersion() {
		String version = System.getProperty("java.version");
		int i = version.indexOf('.');

		if (i != -1 && (i = version.indexOf('.', i + 1)) != -1) {
			return Double.parseDouble(version.substring(0, i));
		}

		return Double.NaN;
	}
}

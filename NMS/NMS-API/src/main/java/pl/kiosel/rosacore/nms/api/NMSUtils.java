package pl.kiosel.rosacore.nms.api;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class NMSUtils {

	private NMSUtils() {
	}

	public static String color(String value) {
		return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
	}

	public static Field findField(Class<?> type, String name) throws NoSuchFieldException {
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

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void applyGameMode(Object nmsPlayer, GameMode gameMode) {
		if (nmsPlayer == null || gameMode == null) return;
		Object manager = findGameModeOwner(nmsPlayer);
		if (manager == null) return;
		for (Method method : manager.getClass().getMethods()) {
			Class<?>[] parameters = method.getParameterTypes();
			if (Modifier.isStatic(method.getModifiers()) || parameters.length != 1 || !parameters[0].isEnum()) continue;
			Class<? extends Enum> enumType = (Class<? extends Enum>) parameters[0];
			if (!isGameModeEnum(enumType)) continue;
			try {
				Enum value = Enum.valueOf(enumType, gameMode.name());
				method.setAccessible(true);
				method.invoke(manager, value);
				return;
			} catch (ReflectiveOperationException | IllegalArgumentException ignored) {
			}
		}
	}

	private static Object findGameModeOwner(Object nmsPlayer) {
		Class<?> current = nmsPlayer.getClass();
		while (current != null && current != Object.class) {
			for (Field field : current.getDeclaredFields()) {
				String type = field.getType().getSimpleName().toLowerCase(java.util.Locale.ROOT);
				if (!type.contains("interactmanager") && !type.contains("gamemode")) continue;
				try {
					field.setAccessible(true);
					Object value = field.get(nmsPlayer);
					if (value != null) return value;
				} catch (IllegalAccessException | RuntimeException ignored) {
				}
			}
			current = current.getSuperclass();
		}
		return nmsPlayer;
	}

	private static boolean isGameModeEnum(Class<?> type) {
		boolean survival = false;
		boolean creative = false;
		for (Object constant : type.getEnumConstants()) {
			String name = ((Enum<?>) constant).name();
			survival |= "SURVIVAL".equals(name);
			creative |= "CREATIVE".equals(name);
		}
		return survival && creative;
	}
}

package pl.kiosel.rosacore.nms.api.tablist;

import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Optional;

public final class TabListSkins {

	private TabListSkins() {
	}

	public static TabListSkin fromPlayer(Player player) {
		if (player == null) return null;
		try {
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Object profile = findProfile(handle);
			if (profile == null) return null;

			Object properties = AuthlibProfileAccess.properties(profile);
			Object textures = invokeNamed(properties, "get", "textures");
			if (!(textures instanceof Collection<?>) || ((Collection<?>) textures).isEmpty()) return null;

			Object property = ((Collection<?>) textures).iterator().next();
			String value = stringValue(property, "getValue", "value");
			if (value == null || value.isEmpty()) return null;
			String signature = stringValue(property, "getSignature", "signature");
			return TabListSkin.of(value, signature);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	private static Object findProfile(Object handle) throws ReflectiveOperationException {
		for (Method method : methods(handle.getClass())) {
			if (Modifier.isStatic(method.getModifiers()) || method.getParameterTypes().length != 0
					|| !"GameProfile".equals(method.getReturnType().getSimpleName())) continue;
			method.setAccessible(true);
			Object profile = method.invoke(handle);
			if (profile != null) return profile;
		}
		for (Field field : fields(handle.getClass())) {
			if (!"GameProfile".equals(field.getType().getSimpleName())) continue;
			field.setAccessible(true);
			Object profile = field.get(handle);
			if (profile != null) return profile;
		}
		return null;
	}

	private static Object invokeNamed(Object owner, String name, Object argument)
			throws ReflectiveOperationException {
		for (Method method : owner.getClass().getMethods()) {
			if (!method.getName().equals(name) || method.getParameterTypes().length != 1) continue;
			return method.invoke(owner, argument);
		}
		return null;
	}

	private static String stringValue(Object owner, String... names) {
		for (String name : names) {
			for (Method method : methods(owner.getClass())) {
				if (!method.getName().equals(name) || method.getParameterTypes().length != 0) continue;
				try {
					method.setAccessible(true);
					Object value = method.invoke(owner);
					if (value instanceof Optional<?>) value = ((Optional<?>) value).orElse(null);
					if (value instanceof String) return (String) value;
				} catch (ReflectiveOperationException | RuntimeException ignored) {
				}
			}
		}
		return null;
	}

	private static Method[] methods(Class<?> type) {
		return type.getMethods();
	}

	private static java.util.List<Field> fields(Class<?> type) {
		java.util.List<Field> result = new java.util.ArrayList<>();
		Class<?> current = type;
		while (current != null && current != Object.class) {
			java.util.Collections.addAll(result, current.getDeclaredFields());
			current = current.getSuperclass();
		}
		return result;
	}
}

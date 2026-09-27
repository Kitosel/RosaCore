package pl.kiosel.rosacore.nms.api.status;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ServerStatusPacketCodec {

	private ServerStatusPacketCodec() {
	}

	static boolean supports(Object packet) {
		if (packet == null) return false;
		String name = packet.getClass().getSimpleName().toLowerCase(Locale.ROOT);
		return name.contains("statusoutserverinfo") || name.contains("statusresponsepacket");
	}

	static Object rewrite(Object packet, ServerStatusPacketEvent event) throws ReflectiveOperationException {
		Object status = status(packet);
		if (status == null) throw new NoSuchFieldException("Server status in " + packet.getClass().getName());

		Object players = players(status);
		if (players != null && updateMutablePlayers(players, event)) return packet;

		Class<?> playersClass = players == null ? playersClass(status.getClass()) : players.getClass();
		if (playersClass == null) throw new ClassNotFoundException("Server-status players component");
		Object replacementPlayers = createPlayers(playersClass, event, packet.getClass().getClassLoader());
		Object replacementStatus = rebuildStatus(status, playersClass, replacementPlayers);
		return rebuildPacket(packet, replacementStatus);
	}

	private static Object status(Object packet) {
		for (Field field : fields(packet.getClass())) {
			if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
			String typeName = field.getType().getSimpleName().toLowerCase(Locale.ROOT);
			if (!typeName.contains("serverping") && !typeName.contains("serverstatus")) continue;
			Object value = read(field, packet);
			if (value != null) return value;
		}
		return null;
	}

	private static Object players(Object status) {
		for (Field field : fields(status.getClass())) {
			if (Modifier.isStatic(field.getModifiers())) continue;
			Object value = read(field, status);
			if (value instanceof Optional<?>) value = ((Optional<?>) value).orElse(null);
			if (value != null && isPlayersType(value.getClass())) return value;
		}
		return null;
	}

	private static Class<?> playersClass(Class<?> statusClass) {
		for (Class<?> nested : statusClass.getDeclaredClasses()) {
			if (isPlayersType(nested)) return nested;
		}
		for (Field field : fields(statusClass)) {
			if (isPlayersType(field.getType())) return field.getType();
			String generic = field.getGenericType().getTypeName().toLowerCase(Locale.ROOT);
			if (!generic.contains("players") && !generic.contains("playersample")) continue;
			for (Class<?> nested : statusClass.getDeclaredClasses()) {
				if (generic.contains(nested.getSimpleName().toLowerCase(Locale.ROOT))) return nested;
			}
		}
		return null;
	}

	private static boolean isPlayersType(Class<?> type) {
		String name = type.getSimpleName().toLowerCase(Locale.ROOT);
		return name.equals("players") || name.contains("playersample") || name.contains("player_sample");
	}

	private static boolean updateMutablePlayers(Object players, ServerStatusPacketEvent event)
			throws ReflectiveOperationException {
		List<Field> integerFields = new ArrayList<>();
		Field sampleField = null;
		for (Field field : fields(players.getClass())) {
			if (Modifier.isStatic(field.getModifiers())) continue;
			if (field.getType() == int.class || field.getType() == Integer.class) integerFields.add(field);
			if (field.getType().isArray()
					&& field.getType().getComponentType().getSimpleName().equals("GameProfile")) sampleField = field;
		}
		if (integerFields.size() < 2 || sampleField == null) return false;
		try {
			write(integerFields.get(0), players, event.getMaxPlayers());
			write(integerFields.get(1), players, event.getOnlinePlayers());
			Object profiles = profileArray(sampleField.getType().getComponentType(), event.getSample());
			write(sampleField, players, profiles);
			return true;
		} catch (IllegalAccessException | RuntimeException exception) {
			return false;
		}
	}

	private static Object createPlayers(Class<?> playersClass, ServerStatusPacketEvent event, ClassLoader loader)
			throws ReflectiveOperationException {
		Class<?> profileClass = Class.forName("com.mojang.authlib.GameProfile", false, loader);
		List<Object> profiles = profiles(profileClass, event.getSample());
		for (Constructor<?> constructor : playersClass.getDeclaredConstructors()) {
			Class<?>[] types = constructor.getParameterTypes();
			if (types.length != 3 || !isInteger(types[0]) || !isInteger(types[1])
					|| !Collection.class.isAssignableFrom(types[2])) continue;
			constructor.setAccessible(true);
			return constructor.newInstance(event.getMaxPlayers(), event.getOnlinePlayers(), profiles);
		}
		throw new NoSuchMethodException("Players constructor in " + playersClass.getName());
	}

	private static Object rebuildStatus(Object status, Class<?> playersClass, Object replacementPlayers)
			throws ReflectiveOperationException {
		List<Field> fields = instanceFields(status.getClass());
		Object[] values = new Object[fields.size()];
		boolean replaced = false;
		for (int index = 0; index < fields.size(); index++) {
			Field field = fields.get(index);
			Object value = read(field, status);
			if (field.getType() == playersClass) {
				value = replacementPlayers;
				replaced = true;
			} else if (value instanceof Optional<?>) {
				Object present = ((Optional<?>) value).orElse(null);
				String generic = field.getGenericType().getTypeName();
				if ((present != null && playersClass.isInstance(present))
						|| generic.contains(playersClass.getSimpleName())) {
					value = Optional.of(replacementPlayers);
					replaced = true;
				}
			}
			values[index] = value;
		}
		if (!replaced) throw new NoSuchFieldException("Players component in " + status.getClass().getName());
		return constructMatching(status.getClass(), values);
	}

	private static Object rebuildPacket(Object packet, Object status) throws ReflectiveOperationException {
		for (Constructor<?> constructor : packet.getClass().getDeclaredConstructors()) {
			Class<?>[] types = constructor.getParameterTypes();
			if (types.length == 1 && types[0].isInstance(status)) {
				constructor.setAccessible(true);
				return constructor.newInstance(status);
			}
		}
		throw new NoSuchMethodException("Status response constructor in " + packet.getClass().getName());
	}

	private static Object constructMatching(Class<?> type, Object[] values) throws ReflectiveOperationException {
		for (Constructor<?> constructor : type.getDeclaredConstructors()) {
			Class<?>[] parameters = constructor.getParameterTypes();
			if (parameters.length != values.length || !matches(parameters, values)) continue;
			constructor.setAccessible(true);
			return constructor.newInstance(values);
		}
		throw new NoSuchMethodException("Matching constructor in " + type.getName());
	}

	private static boolean matches(Class<?>[] parameters, Object[] values) {
		for (int index = 0; index < parameters.length; index++) {
			if (values[index] == null) {
				if (parameters[index].isPrimitive()) return false;
			} else if (!wrap(parameters[index]).isInstance(values[index])) {
				return false;
			}
		}
		return true;
	}

	private static List<Object> profiles(Class<?> profileClass, Collection<ServerStatusSample> sample)
			throws ReflectiveOperationException {
		Constructor<?> constructor = profileClass.getConstructor(java.util.UUID.class, String.class);
		List<Object> result = new ArrayList<>();
		for (ServerStatusSample profile : sample) {
			result.add(constructor.newInstance(profile.getUniqueId(), profile.getName()));
		}
		return result;
	}

	private static Object profileArray(Class<?> profileClass, Collection<ServerStatusSample> sample)
			throws ReflectiveOperationException {
		List<Object> profiles = profiles(profileClass, sample);
		Object array = Array.newInstance(profileClass, profiles.size());
		for (int index = 0; index < profiles.size(); index++) Array.set(array, index, profiles.get(index));
		return array;
	}

	private static boolean isInteger(Class<?> type) {
		return type == int.class || type == Integer.class;
	}

	private static Class<?> wrap(Class<?> type) {
		if (type == int.class) return Integer.class;
		if (type == boolean.class) return Boolean.class;
		if (type == long.class) return Long.class;
		if (type == double.class) return Double.class;
		if (type == float.class) return Float.class;
		if (type == short.class) return Short.class;
		if (type == byte.class) return Byte.class;
		if (type == char.class) return Character.class;
		return type;
	}

	private static List<Field> instanceFields(Class<?> type) {
		List<Field> result = new ArrayList<>();
		for (Field field : fields(type)) if (!Modifier.isStatic(field.getModifiers())) result.add(field);
		return result;
	}

	private static List<Field> fields(Class<?> type) {
		List<Field> result = new ArrayList<>();
		Class<?> current = type;
		while (current != null && current != Object.class) {
			Collections.addAll(result, current.getDeclaredFields());
			current = current.getSuperclass();
		}
		return result;
	}

	private static Object read(Field field, Object owner) {
		try {
			field.setAccessible(true);
			return field.get(owner);
		} catch (IllegalAccessException | RuntimeException ignored) {
			return null;
		}
	}

	private static void write(Field field, Object owner, Object value) throws IllegalAccessException {
		field.setAccessible(true);
		field.set(owner, value);
	}
}

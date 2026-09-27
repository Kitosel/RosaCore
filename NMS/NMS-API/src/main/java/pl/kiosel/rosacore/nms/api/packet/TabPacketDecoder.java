package pl.kiosel.rosacore.nms.api.packet;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class TabPacketDecoder {

	private TabPacketDecoder() {
	}

	static TabOutboundPacketEvent decode(Player viewer, Object packet, boolean rosaPacket) {
		if (viewer == null || packet == null) return null;
		String name = packet.getClass().getSimpleName().toLowerCase(Locale.ROOT);

		if (name.contains("playerinfo")) {
			EnumSet<TabPacketAction> actions = actions(packet, name);
			return new PlayerInfoPacketEvent(viewer, rosaPacket, actions, entries(packet));
		}
		if (name.contains("respawn")) {
			return new PlayerRespawnPacketEvent(viewer, rosaPacket);
		}
		if (isPlayerSpawnPacket(name)) {
			UUID uniqueId = firstUuid(packet);
			if (uniqueId != null && (name.contains("player") || name.contains("named")
					|| Bukkit.getPlayer(uniqueId) != null)) {
				return new PlayerSpawnPacketEvent(viewer, rosaPacket, uniqueId);
			}
		}
		return null;
	}

	private static boolean isPlayerSpawnPacket(String name) {
		return name.contains("namedentityspawn") || name.contains("addplayerpacket")
				|| name.contains("addentitypacket");
	}

	private static EnumSet<TabPacketAction> actions(Object packet, String packetName) {
		EnumSet<TabPacketAction> result = EnumSet.noneOf(TabPacketAction.class);
		if (packetName.contains("playerinforemove")) result.add(TabPacketAction.REMOVE_PLAYER);

		for (Field field : fields(packet.getClass())) {
			Object value = read(field, packet);
			if (value instanceof Enum<?>) {
				result.add(action((Enum<?>) value));
			} else if (value instanceof Collection<?>) {
				for (Object element : (Collection<?>) value) {
					if (element instanceof Enum<?>) result.add(action((Enum<?>) element));
				}
			}
		}
		result.remove(TabPacketAction.UNKNOWN);
		if (result.isEmpty()) result.add(TabPacketAction.UNKNOWN);
		return result;
	}

	private static TabPacketAction action(Enum<?> value) {
		String name = value.name().toUpperCase(Locale.ROOT);
		if (name.contains("ADD_PLAYER")) return TabPacketAction.ADD_PLAYER;
		if (name.contains("INITIALIZE_CHAT")) return TabPacketAction.INITIALIZE_CHAT;
		if (name.contains("UPDATE_GAME_MODE")) return TabPacketAction.UPDATE_GAME_MODE;
		if (name.contains("UPDATE_LISTED")) return TabPacketAction.UPDATE_LISTED;
		if (name.contains("UPDATE_LATENCY")) return TabPacketAction.UPDATE_LATENCY;
		if (name.contains("UPDATE_DISPLAY_NAME")) return TabPacketAction.UPDATE_DISPLAY_NAME;
		if (name.contains("UPDATE_HAT")) return TabPacketAction.UPDATE_HAT;
		if (name.contains("UPDATE_LIST_ORDER")) return TabPacketAction.UPDATE_LIST_ORDER;
		if (name.contains("REMOVE_PLAYER")) return TabPacketAction.REMOVE_PLAYER;

		Object[] constants = value.getDeclaringClass().getEnumConstants();
		int ordinal = value.ordinal();
		if (constants != null && constants.length > 5) {
			switch (ordinal) {
				case 0: return TabPacketAction.ADD_PLAYER;
				case 1: return TabPacketAction.INITIALIZE_CHAT;
				case 2: return TabPacketAction.UPDATE_GAME_MODE;
				case 3: return TabPacketAction.UPDATE_LISTED;
				case 4: return TabPacketAction.UPDATE_LATENCY;
				case 5: return TabPacketAction.UPDATE_DISPLAY_NAME;
				case 6: return TabPacketAction.UPDATE_HAT;
				case 7: return TabPacketAction.UPDATE_LIST_ORDER;
				default: return TabPacketAction.UNKNOWN;
			}
		}
		switch (ordinal) {
			case 0: return TabPacketAction.ADD_PLAYER;
			case 1: return TabPacketAction.UPDATE_GAME_MODE;
			case 2: return TabPacketAction.UPDATE_LATENCY;
			case 3: return TabPacketAction.UPDATE_DISPLAY_NAME;
			case 4: return TabPacketAction.REMOVE_PLAYER;
			default: return TabPacketAction.UNKNOWN;
		}
	}

	private static List<TabPacketEntry> entries(Object packet) {
		List<TabPacketEntry> best = Collections.emptyList();
		for (Field field : fields(packet.getClass())) {
			Object value = read(field, packet);
			if (!(value instanceof Collection<?>)) continue;
			List<TabPacketEntry> decoded = decodeEntries((Collection<?>) value);
			if (decoded.size() > best.size()) best = decoded;
		}
		return best;
	}

	private static List<TabPacketEntry> decodeEntries(Collection<?> values) {
		List<TabPacketEntry> result = new ArrayList<>();
		for (Object value : values) {
			TabPacketEntry entry = entry(value);
			if (entry != null && entry.getUniqueId() != null) result.add(entry);
		}
		return result;
	}

	private static TabPacketEntry entry(Object value) {
		if (value == null || value instanceof Enum<?>) return null;
		if (value instanceof UUID) return new TabPacketEntry((UUID) value, null, null);

		Object profile = gameProfile(value);
		UUID uniqueId = profile == null ? null : uuidFromMethods(profile);
		String profileName = profile == null ? null : stringFromMethods(profile, "getName", "name");
		if (uniqueId == null) uniqueId = firstUuid(value);
		Boolean listed = listed(value);
		return uniqueId == null ? null : new TabPacketEntry(uniqueId, profileName, listed);
	}

	private static Object gameProfile(Object value) {
		if (value.getClass().getSimpleName().equals("GameProfile")) return value;
		for (Method method : value.getClass().getDeclaredMethods()) {
			if (method.getParameterTypes().length != 0
					|| !method.getReturnType().getSimpleName().equals("GameProfile")) continue;
			Object profile = invoke(method, value);
			if (profile != null) return profile;
		}
		for (Field field : fields(value.getClass())) {
			if (!field.getType().getSimpleName().equals("GameProfile")) continue;
			Object profile = read(field, value);
			if (profile != null) return profile;
		}
		return null;
	}

	private static Boolean listed(Object value) {
		for (String name : new String[]{"listed", "isListed"}) {
			try {
				Method method = value.getClass().getDeclaredMethod(name);
				Object result = invoke(method, value);
				if (result instanceof Boolean) return (Boolean) result;
			} catch (NoSuchMethodException ignored) {
			}
		}
		for (Field field : fields(value.getClass())) {
			if (field.getType() != boolean.class && field.getType() != Boolean.class) continue;
			Object result = read(field, value);
			if (result instanceof Boolean) return (Boolean) result;
		}
		return null;
	}

	private static UUID firstUuid(Object value) {
		UUID fromMethod = uuidFromMethods(value);
		if (fromMethod != null) return fromMethod;
		for (Field field : fields(value.getClass())) {
			if (field.getType() != UUID.class) continue;
			Object result = read(field, value);
			if (result instanceof UUID) return (UUID) result;
		}
		return null;
	}

	private static UUID uuidFromMethods(Object value) {
		for (Method method : value.getClass().getDeclaredMethods()) {
			if (method.getParameterTypes().length != 0 || method.getReturnType() != UUID.class
					|| Modifier.isStatic(method.getModifiers())) continue;
			Object result = invoke(method, value);
			if (result instanceof UUID) return (UUID) result;
		}
		return null;
	}

	private static String stringFromMethods(Object value, String... names) {
		for (String name : names) {
			try {
				Method method = value.getClass().getMethod(name);
				Object result = invoke(method, value);
				if (result instanceof String) return (String) result;
			} catch (NoSuchMethodException ignored) {
			}
		}
		return null;
	}

	private static List<Field> fields(Class<?> type) {
		List<Field> fields = new ArrayList<>();
		Class<?> current = type;
		while (current != null && current != Object.class) {
			Collections.addAll(fields, current.getDeclaredFields());
			current = current.getSuperclass();
		}
		return fields;
	}

	private static Object read(Field field, Object owner) {
		try {
			field.setAccessible(true);
			return field.get(owner);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	private static Object invoke(Method method, Object owner) {
		try {
			method.setAccessible(true);
			return method.invoke(owner);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}
}

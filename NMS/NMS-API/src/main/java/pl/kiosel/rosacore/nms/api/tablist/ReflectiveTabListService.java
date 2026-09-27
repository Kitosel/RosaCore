package pl.kiosel.rosacore.nms.api.tablist;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.nms.api.NMSUtils;
import pl.kiosel.rosacore.nms.api.packet.RosaPacketMarker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ReflectiveTabListService implements TabListService {

	private final Map<UUID, ReflectiveTabList> tabLists = new HashMap<>();
	private ProtocolAccess protocol;

	@Override
	public TabList create(Player player) {
		if (player == null) throw new NullPointerException("player");
		clear(player);
		if (protocol == null) protocol = new ProtocolAccess(player.getClass().getClassLoader());
		ReflectiveTabList tabList = new ReflectiveTabList(this, protocol, player);
		tabLists.put(player.getUniqueId(), tabList);
		return tabList;
	}

	@Override
	public TabList get(Player player) {
		if (player == null) return null;
		return tabLists.get(player.getUniqueId());
	}

	@Override
	public void clear(Player player) {
		if (player == null) throw new NullPointerException("player");
		ReflectiveTabList tabList = tabLists.remove(player.getUniqueId());
		if (tabList != null) tabList.clearInternal();
	}

	@Override
	public void clearAll() {
		ReflectiveTabList[] existing = tabLists.values().toArray(new ReflectiveTabList[0]);
		tabLists.clear();
		for (ReflectiveTabList tabList : existing) tabList.clearInternal();
	}

	@Override
	public int size() {
		return tabLists.size();
	}

	private void remove(ReflectiveTabList tabList) {
		tabLists.remove(tabList.getPlayer().getUniqueId(), tabList);
	}

	private static final class ReflectiveTabList implements TabList {

		private final ReflectiveTabListService service;
		private final ProtocolAccess protocol;
		private final Player player;
		private final TabListCell[] cells = new TabListCell[DEFAULT_CELL_COUNT];
		private List<UUID> renderedProfileIds;
		private TabListCell[] renderedCells;
		private int activeCellCount = DEFAULT_CELL_COUNT;
		private String header = "";
		private String footer = "";
		private String renderedHeader;
		private String renderedFooter;

		private ReflectiveTabList(ReflectiveTabListService service, ProtocolAccess protocol, Player player) {
			this.service = service;
			this.protocol = protocol;
			this.player = player;
		}

		@Override
		public Player getPlayer() {
			return player;
		}

		@Override
		public int getCellCount() {
			return DEFAULT_CELL_COUNT;
		}

		@Override
		public int getActiveCellCount() {
			return activeCellCount;
		}

		@Override
		public TabList setActiveCellCount(int cellCount) {
			if (cellCount < 0 || cellCount > DEFAULT_CELL_COUNT) {
				throw new IllegalArgumentException("Active tab-list cell count must be between 0 and 80: " + cellCount);
			}
			this.activeCellCount = cellCount;
			return this;
		}

		@Override
		public String getHeader() {
			return header;
		}

		@Override
		public TabList setHeader(String header) {
			this.header = header == null ? "" : header;
			return this;
		}

		@Override
		public String getFooter() {
			return footer;
		}

		@Override
		public TabList setFooter(String footer) {
			this.footer = footer == null ? "" : footer;
			return this;
		}

		@Override
		public TabListCell getCell(int slot) {
			validateSlot(slot);
			return cells[slot];
		}

		@Override
		public TabList setCell(int slot, TabListCell cell) {
			validateSlot(slot);
			cells[slot] = cell;
			return this;
		}

		@Override
		public TabList clearCell(int slot) {
			validateSlot(slot);
			cells[slot] = null;
			return this;
		}

		@Override
		public TabList clearCells() {
			for (int slot = 0; slot < cells.length; slot++) cells[slot] = null;
			return this;
		}

		@Override
		public TabList showRealPlayer(Player realPlayer) {
			if (canUseRealPlayer(realPlayer)) {
				protocol.sendPacket(player, protocol.createRealPlayerUpdatePacket(
						Collections.singletonList(realPlayer)));
			}
			return this;
		}

		@Override
		public TabList hideRealPlayer(Player realPlayer) {
			if (canUseRealPlayer(realPlayer)) {
				hideRealPlayer(realPlayer.getUniqueId());
			}
			return this;
		}

		@Override
		public TabList hideRealPlayer(UUID profileId) {
			if (profileId == null || !player.isOnline()) return this;
			if (player.getUniqueId().equals(profileId)) {
				protocol.sendPacket(player, protocol.createListedPacket(player, false));
			} else {
				protocol.sendPacket(player, protocol.createRemovePacket(Collections.singletonList(profileId)));
			}
			return this;
		}

		@Override
		public void sendHeaderFooter() {
			if (!player.isOnline()) return;
			protocol.sendPacket(player, protocol.createHeaderFooterPacket(header, footer));
			renderedHeader = header;
			renderedFooter = footer;
		}

		@Override
		public void send() {
			if (!player.isOnline()) return;
			TabListCell[] nextCells = Arrays.copyOf(cells, activeCellCount);
			TabListDiff diff = TabListDiff.between(renderedCells, nextCells);
			List<UUID> nextProfileIds = new ArrayList<>(activeCellCount);
			List<Object> entries = new ArrayList<>(activeCellCount);
			for (int slot = 0; slot < activeCellCount; slot++) {
				TabListCell cell = cells[slot];
				if (cell != null && !cell.isListed()) continue;
				UUID profileId = profileId(slot, cell);
				nextProfileIds.add(profileId);
				entries.add(protocol.createEntry(profileId, profileName(slot, cell), slot, cell));
			}
			if (renderedProfileIds == null) {
				hideRealProfiles();
				if (!entries.isEmpty()) protocol.sendPacket(player, protocol.createUpdatePacket(entries));
			} else if (diff.requiresRecreate()) {
				removeRenderedProfiles();
				if (!entries.isEmpty()) protocol.sendPacket(player, protocol.createUpdatePacket(entries));
			} else if (!diff.isEmpty() && !entries.isEmpty()) {
				protocol.sendPacket(player, protocol.createMetadataUpdatePacket(entries));
			}
			renderedProfileIds = nextProfileIds;
			renderedCells = nextCells;
			if (!Objects.equals(renderedHeader, header) || !Objects.equals(renderedFooter, footer)) {
				sendHeaderFooter();
			}
		}

		@Override
		public void clear() {
			clearInternal();
			service.remove(this);
		}

		private void clearInternal() {
			if (!player.isOnline()) return;
			removeRenderedProfiles();
			restoreRealProfiles();
			protocol.sendPacket(player, protocol.createHeaderFooterPacket("", ""));
			renderedHeader = "";
			renderedFooter = "";
		}

		private void hideRealProfiles() {
			List<UUID> profileIds = new ArrayList<>();
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (player.canSee(onlinePlayer)
						&& !player.getUniqueId().equals(onlinePlayer.getUniqueId())) {
					profileIds.add(onlinePlayer.getUniqueId());
				}
			}
			if (!profileIds.isEmpty()) {
				protocol.sendPacket(player, protocol.createRemovePacket(profileIds));
			}
			protocol.sendPacket(player, protocol.createListedPacket(player, false));
		}

		private void restoreRealProfiles() {
			List<Player> visiblePlayers = new ArrayList<>();
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				if (player.canSee(onlinePlayer)) visiblePlayers.add(onlinePlayer);
			}
			if (!visiblePlayers.isEmpty()) {
				protocol.sendPacket(player, protocol.createRealPlayerUpdatePacket(visiblePlayers));
			}
		}

		private void removeRenderedProfiles() {
			if (renderedProfileIds == null || !player.isOnline()) return;
			if (!renderedProfileIds.isEmpty()) {
				protocol.sendPacket(player, protocol.createRemovePacket(renderedProfileIds));
			}
			renderedProfileIds = null;
			renderedCells = null;
		}

		private boolean canUseRealPlayer(Player realPlayer) {
			return realPlayer != null && player.isOnline() && realPlayer.isOnline()
					&& player.canSee(realPlayer);
		}

		private static UUID profileId(int slot, TabListCell cell) {
			if (cell != null && cell.getUniqueId() != null) return cell.getUniqueId();
			return UUID.nameUUIDFromBytes(("rosacore:tablist:" + slot).getBytes(StandardCharsets.UTF_8));
		}

		private static String profileName(int slot, TabListCell cell) {
			if (cell != null && cell.getProfileName() != null && !cell.getProfileName().isEmpty()) {
				return cell.getProfileName();
			}
			return String.format("rosa%012d", slot);
		}

		private static void validateSlot(int slot) {
			if (slot < 0 || slot >= DEFAULT_CELL_COUNT) {
				throw new IllegalArgumentException("Tab-list slot must be between 0 and 79: " + slot);
			}
		}
	}

	private static final class ProtocolAccess {

		private final Class<?> packetInterface;
		private final Class<?> componentClass;
		private final Class<?> gameProfileClass;
		private final Class<?> propertyClass;
		private final Class<? extends Enum> gameModeClass;
		private final Constructor<?> gameProfileConstructor;
		private final Constructor<?> propertyConstructor;
		private final Constructor<?> entryConstructor;
		private final Constructor<?> updatePacketConstructor;
		private final Constructor<?> removePacketConstructor;
		private final Constructor<?> headerFooterPacketConstructor;
		private final Method realPlayerUpdateFactory;
		private final Field updateEntriesField;
		private final EnumSet<?> updateActions;
		private final EnumSet<?> metadataActions;
		private final EnumSet<?> updateListedActions;

		@SuppressWarnings({"unchecked", "rawtypes"})
		private ProtocolAccess(ClassLoader classLoader) {
			try {
				packetInterface = load(classLoader, "net.minecraft.network.protocol.Packet");
				componentClass = loadAny(classLoader,
						"net.minecraft.network.chat.IChatBaseComponent",
						"net.minecraft.network.chat.Component");
				gameProfileClass = load(classLoader, "com.mojang.authlib.GameProfile");
				propertyClass = load(classLoader, "com.mojang.authlib.properties.Property");
				gameModeClass = (Class<? extends Enum>) loadAny(classLoader,
						"net.minecraft.world.level.EnumGamemode",
						"net.minecraft.world.level.GameType");

				gameProfileConstructor = constructor(gameProfileClass, UUID.class, String.class);
				propertyConstructor = findPropertyConstructor(propertyClass);

				Class<?> updatePacketClass = load(classLoader,
						"net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
				Class<?> actionClass = findActionClass(updatePacketClass);
				entryConstructor = findEntryConstructor(updatePacketClass, gameProfileClass);
				updatePacketConstructor = findUpdatePacketConstructor(updatePacketClass);
				updateEntriesField = findListField(updatePacketClass);
				updateActions = EnumSet.allOf((Class) actionClass);
				EnumSet metadata = EnumSet.noneOf((Class) actionClass);
				addActionIfPresent(metadata, actionClass, "UPDATE_GAME_MODE");
				addActionIfPresent(metadata, actionClass, "UPDATE_LATENCY");
				addActionIfPresent(metadata, actionClass, "UPDATE_DISPLAY_NAME");
				addActionIfPresent(metadata, actionClass, "UPDATE_LIST_ORDER");
				addActionIfPresent(metadata, actionClass, "UPDATE_HAT");
				metadataActions = metadata;
				Enum updateListedAction = findAction(actionClass, "UPDATE_LISTED", 3);
				EnumSet listedActions = EnumSet.noneOf((Class) actionClass);
				listedActions.add(updateListedAction);
				updateListedActions = listedActions;
				realPlayerUpdateFactory = findRealPlayerUpdateFactory(updatePacketClass);

				Class<?> removePacketClass = load(classLoader,
						"net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket");
				removePacketConstructor = findSingleCollectionConstructor(removePacketClass);

				Class<?> headerFooterPacketClass = loadAny(classLoader,
						"net.minecraft.network.protocol.game.PacketPlayOutPlayerListHeaderFooter",
						"net.minecraft.network.protocol.game.ClientboundTabListPacket");
				headerFooterPacketConstructor = findComponentPairConstructor(headerFooterPacketClass, componentClass);
			} catch (ReflectiveOperationException exception) {
				throw new IllegalStateException("Could not initialize the modern tab-list protocol", exception);
			}
		}

		private Object createEntry(UUID id, String name, int slot, TabListCell cell) {
			return createEntry(id, name, slot, cell, true);
		}

		private Object createEntry(UUID id, String name, int slot, TabListCell cell, boolean listed) {
			try {
				Object textureProperty = cell == null || cell.getSkin() == null
						? null : createTextureProperty(cell.getSkin());
				Object profile = AuthlibProfileAccess.createProfile(
						gameProfileClass, gameProfileConstructor, id, name, textureProperty);
				Object component = component(cell == null ? "" : NMSUtils.color(cell.getText()));
				int ping = cell == null ? 0 : Math.max(0, cell.getPing());
				Class<?>[] parameterTypes = entryConstructor.getParameterTypes();
				Object[] arguments = new Object[parameterTypes.length];
				int integerIndex = 0;
				for (int index = 0; index < parameterTypes.length; index++) {
					Class<?> type = parameterTypes[index];
					if (type == UUID.class) arguments[index] = id;
					else if (type == gameProfileClass) arguments[index] = profile;
					else if (type == boolean.class || type == Boolean.class) arguments[index] = listed;
					else if (type == int.class || type == Integer.class) {
						arguments[index] = integerIndex++ == 0 ? ping : TabList.DEFAULT_CELL_COUNT - slot;
					} else if (gameModeClass.isAssignableFrom(type)) arguments[index] = gameMode(cell);
					else if (componentClass.isAssignableFrom(type)) arguments[index] = component;
					else if (type == Optional.class) arguments[index] = Optional.empty();
					else arguments[index] = null;
				}
				return entryConstructor.newInstance(arguments);
			} catch (ReflectiveOperationException | RuntimeException exception) {
				throw new IllegalStateException("Could not create a tab-list entry", exception);
			}
		}

		private Object createUpdatePacket(List<Object> entries) {
			return createUpdatePacket(updateActions, entries);
		}

		private Object createMetadataUpdatePacket(List<Object> entries) {
			return createUpdatePacket(metadataActions, entries);
		}

		private Object createListedPacket(Player player, boolean listed) {
			Object entry = createEntry(player.getUniqueId(), player.getName(), 0, null, listed);
			return createUpdatePacket(updateListedActions, Collections.singletonList(entry));
		}

		private Object createUpdatePacket(EnumSet<?> actions, List<Object> entries) {
			try {
				Object packet = updatePacketConstructor.newInstance(actions, Collections.emptyList());
				updateEntriesField.set(packet, entries);
				return packet;
			} catch (ReflectiveOperationException exception) {
				throw new IllegalStateException("Could not create a player-info update packet", exception);
			}
		}

		private Object createRemovePacket(List<UUID> profileIds) {
			try {
				return removePacketConstructor.newInstance(new ArrayList<>(profileIds));
			} catch (ReflectiveOperationException exception) {
				throw new IllegalStateException("Could not create a player-info remove packet", exception);
			}
		}

		private Object createRealPlayerUpdatePacket(List<Player> players) {
			try {
				List<Object> handles = new ArrayList<>(players.size());
				for (Player player : players) {
					handles.add(player.getClass().getMethod("getHandle").invoke(player));
				}
				return realPlayerUpdateFactory.invoke(null, handles);
			} catch (ReflectiveOperationException exception) {
				throw new IllegalStateException("Could not restore real tab-list players", exception);
			}
		}

		private Object createHeaderFooterPacket(String header, String footer) {
			try {
				return headerFooterPacketConstructor.newInstance(
						component(NMSUtils.color(header)), component(NMSUtils.color(footer)));
			} catch (ReflectiveOperationException exception) {
				throw new IllegalStateException("Could not create a tab-list header/footer packet", exception);
			}
		}

		private void sendPacket(Player player, Object packet) {
			try {
				Object handle = player.getClass().getMethod("getHandle").invoke(player);
				Object connection = findConnection(handle);
				Method sendMethod = findSendMethod(connection.getClass(), packet.getClass());
				sendMethod.invoke(connection, RosaPacketMarker.mark(packet));
			} catch (ReflectiveOperationException exception) {
				throw new IllegalStateException("Could not send an NMS packet to " + player.getName(), exception);
			}
		}

		private Object component(String text) throws ReflectiveOperationException {
			for (String name : new String[]{"literal", "a", "b"}) {
				for (Method method : componentClass.getMethods()) {
					if (!method.getName().equals(name) || !Modifier.isStatic(method.getModifiers())) continue;
					if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == String.class
							&& componentClass.isAssignableFrom(method.getReturnType())) {
						return method.invoke(null, text == null ? "" : text);
					}
				}
			}
			throw new NoSuchMethodException("No literal component factory in " + componentClass.getName());
		}

		private Object createTextureProperty(TabListSkin skin) throws ReflectiveOperationException {
			String value = skin.getValue();
			if (value == null || value.isEmpty()) return null;
			String signature = skin.getSignature();
			return propertyConstructor.getParameterTypes().length == 2
					? propertyConstructor.newInstance("textures", value)
					: propertyConstructor.newInstance("textures", value, signature == null || signature.isEmpty() ? null : signature);
		}

		private Object gameMode(TabListCell cell) {
			String wanted = cell == null || cell.getGameMode() == null
					? "SURVIVAL" : cell.getGameMode().name();
			Object[] constants = gameModeClass.getEnumConstants();
			for (Object constant : constants) {
				if (wanted.equals(((Enum<?>) constant).name())) return constant;
			}
			for (Object constant : constants) {
				if ("SURVIVAL".equals(((Enum<?>) constant).name())) return constant;
			}
			return constants[0];
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private static void addActionIfPresent(EnumSet target, Class<?> actionClass, String name) {
			for (Object constant : actionClass.getEnumConstants()) {
				if (name.equals(((Enum<?>) constant).name())) {
					target.add((Enum) constant);
					return;
				}
			}
		}

		private Object findConnection(Object handle) throws ReflectiveOperationException {
			Class<?> current = handle.getClass();
			while (current != null) {
				for (Field field : current.getDeclaredFields()) {
					String typeName = field.getType().getName();
					if (!typeName.endsWith("PlayerConnection")
							&& !typeName.endsWith("ServerGamePacketListenerImpl")) continue;
					field.setAccessible(true);
					Object connection = field.get(handle);
					if (connection != null) return connection;
				}
				current = current.getSuperclass();
			}
			throw new NoSuchFieldException("Player connection in " + handle.getClass().getName());
		}

		private Method findSendMethod(Class<?> connectionClass, Class<?> packetClass) throws NoSuchMethodException {
			Method fallback = null;
			for (Method method : connectionClass.getMethods()) {
				Class<?>[] parameters = method.getParameterTypes();
				if (parameters.length != 1 || !parameters[0].isAssignableFrom(packetClass)) continue;
				if (!packetInterface.isAssignableFrom(parameters[0])) continue;
				if (method.getName().equals("send") || method.getName().equals("a")) return method;
				fallback = method;
			}
			if (fallback != null) return fallback;
			throw new NoSuchMethodException("Packet send method in " + connectionClass.getName());
		}

		private static Class<?> findActionClass(Class<?> updatePacketClass) throws ClassNotFoundException {
			for (Class<?> nested : updatePacketClass.getDeclaredClasses()) {
				if (nested.isEnum()) return nested;
			}
			throw new ClassNotFoundException("Player-info action enum in " + updatePacketClass.getName());
		}

		private static Enum<?> findAction(Class<?> actionClass, String name, int fallbackOrdinal)
				throws ClassNotFoundException {
			Object[] constants = actionClass.getEnumConstants();
			for (Object constant : constants) {
				Enum<?> action = (Enum<?>) constant;
				if (name.equals(action.name())) return action;
			}
			if (fallbackOrdinal >= 0 && fallbackOrdinal < constants.length) {
				return (Enum<?>) constants[fallbackOrdinal];
			}
			throw new ClassNotFoundException(name + " action in " + actionClass.getName());
		}

		private static Constructor<?> findEntryConstructor(Class<?> updatePacketClass, Class<?> gameProfileClass)
				throws NoSuchMethodException {
			for (Class<?> nested : updatePacketClass.getDeclaredClasses()) {
				for (Constructor<?> constructor : nested.getDeclaredConstructors()) {
					for (Class<?> parameter : constructor.getParameterTypes()) {
						if (parameter == gameProfileClass) {
							constructor.setAccessible(true);
							return constructor;
						}
					}
				}
			}
			throw new NoSuchMethodException("Player-info entry constructor in " + updatePacketClass.getName());
		}

		private static Constructor<?> findUpdatePacketConstructor(Class<?> packetClass) throws NoSuchMethodException {
			for (Constructor<?> constructor : packetClass.getDeclaredConstructors()) {
				Class<?>[] parameters = constructor.getParameterTypes();
				if (parameters.length == 2 && EnumSet.class.isAssignableFrom(parameters[0])
						&& Collection.class.isAssignableFrom(parameters[1])) {
					constructor.setAccessible(true);
					return constructor;
				}
			}
			throw new NoSuchMethodException("Player-info update packet constructor in " + packetClass.getName());
		}

		private static Method findRealPlayerUpdateFactory(Class<?> packetClass) throws NoSuchMethodException {
			for (Method method : packetClass.getDeclaredMethods()) {
				Class<?>[] parameters = method.getParameterTypes();
				if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != packetClass) continue;
				if (parameters.length == 1 && Collection.class.isAssignableFrom(parameters[0])) {
					method.setAccessible(true);
					return method;
				}
			}
			throw new NoSuchMethodException("Real-player update factory in " + packetClass.getName());
		}

		private static Field findListField(Class<?> packetClass) throws NoSuchFieldException {
			for (Field field : packetClass.getDeclaredFields()) {
				if (List.class.isAssignableFrom(field.getType())) {
					field.setAccessible(true);
					return field;
				}
			}
			throw new NoSuchFieldException("Player-info entries in " + packetClass.getName());
		}

		private static Constructor<?> findSingleCollectionConstructor(Class<?> type) throws NoSuchMethodException {
			for (Constructor<?> constructor : type.getDeclaredConstructors()) {
				Class<?>[] parameters = constructor.getParameterTypes();
				if (parameters.length == 1 && Collection.class.isAssignableFrom(parameters[0])) {
					constructor.setAccessible(true);
					return constructor;
				}
			}
			throw new NoSuchMethodException("Collection constructor in " + type.getName());
		}

		private static Constructor<?> findComponentPairConstructor(Class<?> type, Class<?> componentClass)
				throws NoSuchMethodException {
			for (Constructor<?> constructor : type.getDeclaredConstructors()) {
				Class<?>[] parameters = constructor.getParameterTypes();
				if (parameters.length == 2 && componentClass.isAssignableFrom(parameters[0])
						&& componentClass.isAssignableFrom(parameters[1])) {
					constructor.setAccessible(true);
					return constructor;
				}
			}
			throw new NoSuchMethodException("Header/footer packet constructor in " + type.getName());
		}

		private static Constructor<?> findPropertyConstructor(Class<?> type) throws NoSuchMethodException {
			Constructor<?> unsignedConstructor = null;
			for (Constructor<?> constructor : type.getConstructors()) {
				Class<?>[] parameters = constructor.getParameterTypes();
				if (parameters.length == 3 && parameters[0] == String.class
						&& parameters[1] == String.class && parameters[2] == String.class)
					return constructor;
				if (parameters.length == 2 && parameters[0] == String.class
						&& parameters[1] == String.class)
					unsignedConstructor = constructor;
			}
			if (unsignedConstructor != null) return unsignedConstructor;
			throw new NoSuchMethodException("Authlib property constructor");
		}

		private static Constructor<?> constructor(Class<?> type, Class<?>... parameters) throws NoSuchMethodException {
			Constructor<?> constructor = type.getConstructor(parameters);
			constructor.setAccessible(true);
			return constructor;
		}

		private static Class<?> load(ClassLoader classLoader, String name) throws ClassNotFoundException {
			return Class.forName(name, false, classLoader);
		}

		private static Class<?> loadAny(ClassLoader classLoader, String... names) throws ClassNotFoundException {
			for (String name : names) {
				try {
					return load(classLoader, name);
				} catch (ClassNotFoundException ignored) {
				}
			}
			throw new ClassNotFoundException("None of the classes exist: " + java.util.Arrays.toString(names));
		}
	}
}

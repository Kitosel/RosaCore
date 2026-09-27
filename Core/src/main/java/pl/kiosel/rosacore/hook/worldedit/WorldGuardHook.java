package pl.kiosel.rosacore.hook.worldedit;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.hook.RosaHook;
import pl.kiosel.rosacore.hook.internal.HookReflection;

import java.lang.reflect.Array;
import java.util.*;

public final class WorldGuardHook extends RosaHook {

	private Adapter adapter;

	@Override
	public String getName() {
		return "WorldGuard";
	}

	@Override
	public String[] getPluginDependencies() {
		return new String[]{"WorldGuard", "WorldEdit"};
	}

	@Override
	protected boolean onEnable(RosaPlugin plugin) throws Exception {
		Plugin worldGuard = getDependencyPlugin("WorldGuard");
		Plugin worldEdit = getDependencyPlugin("WorldEdit");
		try {
			HookReflection.findClass(worldEdit, "com.sk89q.worldedit.math.BlockVector3");
			this.adapter = new ModernAdapter(worldGuard, worldEdit);
		} catch (ClassNotFoundException ignored) {
			this.adapter = new LegacyAdapter(worldGuard, worldEdit);
		}
		return true;
	}

	@Override
	protected void onDisable() {
		this.adapter = null;
	}

	public Boolean getStateFlag(Location location, String flagName) {
		return getStateFlag(location, flagName, null);
	}

	public Boolean getStateFlag(Location location, String flagName, Player player) {
		validate(location, flagName);
		if (!isEnabled() || this.adapter == null) return null;
		try {
			return this.adapter.state(location, flagName, player);
		} catch (ReflectiveOperationException | LinkageError exception) {
			throw failure("query state flag '" + flagName + "'", exception);
		}
	}

	public Boolean getStateFlag(Chunk chunk, String flagName) {
		Objects.requireNonNull(chunk, "chunk");
		validateFlag(flagName);
		if (!isEnabled() || this.adapter == null) return null;
		try {
			return this.adapter.state(chunk, flagName);
		} catch (ReflectiveOperationException | LinkageError exception) {
			throw failure("query chunk state flag '" + flagName + "'", exception);
		}
	}

	public boolean testState(Location location, String flagName, Player player, boolean defaultValue) {
		Boolean state = getStateFlag(location, flagName, player);
		return state == null ? defaultValue : state;
	}

	public boolean isPvpAllowed(Location location) {
		return testState(location, "pvp", null, true);
	}

	public boolean isBreakAllowed(Player player, Location location) {
		return testState(location, "block-break", player, true);
	}

	public boolean isBuildAllowed(Player player, Location location) {
		return testState(location, "build", player, true);
	}

	public boolean isInteractAllowed(Player player, Location location) {
		return testState(location, "use", player, true);
	}

	public boolean isExplosionsAllowed(Location location) {
		return testState(location, "other-explosion", null, true);
	}

	public boolean isMobSpawningAllowed(Location location) {
		return testState(location, "mob-spawning", null, true);
	}

	public List<String> getRegionNames(Location location) {
		Objects.requireNonNull(location, "location");
		if (location.getWorld() == null || !isEnabled() || this.adapter == null)
			return Collections.emptyList();
		try {
			return this.adapter.regions(location);
		} catch (ReflectiveOperationException | LinkageError exception) {
			throw failure("query regions at a location", exception);
		}
	}

	public List<String> getRegionNames(Chunk chunk) {
		Objects.requireNonNull(chunk, "chunk");
		if (!isEnabled() || this.adapter == null) return Collections.emptyList();
		try {
			return this.adapter.regions(chunk);
		} catch (ReflectiveOperationException | LinkageError exception) {
			throw failure("query regions in a chunk", exception);
		}
	}

	public Location getRegionCenter(World world, String regionName) {
		Objects.requireNonNull(world, "world");
		validateRegion(regionName);
		if (!isEnabled() || this.adapter == null) return null;
		try {
			return this.adapter.center(world, regionName);
		} catch (ReflectiveOperationException | LinkageError exception) {
			throw failure("find center of region '" + regionName + "'", exception);
		}
	}

	public Location getRegionCenter(Location location, String regionName) {
		Objects.requireNonNull(location, "location");
		if (location.getWorld() == null) return null;
		return getRegionCenter(location.getWorld(), regionName);
	}

	private static void validate(Location location, String flagName) {
		Objects.requireNonNull(location, "location");
		if (location.getWorld() == null) throw new IllegalArgumentException("Location has no world");
		validateFlag(flagName);
	}

	private static void validateFlag(String flagName) {
		if (flagName == null || flagName.trim().isEmpty())
			throw new IllegalArgumentException("Flag name cannot be empty");
	}

	private static void validateRegion(String regionName) {
		if (regionName == null || regionName.trim().isEmpty())
			throw new IllegalArgumentException("Region name cannot be empty");
	}

	private IllegalStateException failure(String action, Throwable throwable) {
		return new IllegalStateException("WorldGuard failed to " + action, throwable);
	}

	private interface Adapter {
		Boolean state(Location location, String flagName, Player player) throws ReflectiveOperationException;

		Boolean state(Chunk chunk, String flagName) throws ReflectiveOperationException;

		List<String> regions(Location location) throws ReflectiveOperationException;

		List<String> regions(Chunk chunk) throws ReflectiveOperationException;

		Location center(World world, String regionName) throws ReflectiveOperationException;
	}

	private abstract static class BaseAdapter implements Adapter {
		final Plugin worldGuardPlugin;
		final Plugin worldEditPlugin;
		final Class<?> stateFlagType;

		BaseAdapter(Plugin worldGuardPlugin, Plugin worldEditPlugin) throws ClassNotFoundException {
			this.worldGuardPlugin = Objects.requireNonNull(worldGuardPlugin, "worldGuardPlugin");
			this.worldEditPlugin = Objects.requireNonNull(worldEditPlugin, "worldEditPlugin");
			this.stateFlagType = wgType("com.sk89q.worldguard.protection.flags.StateFlag");
		}

		final Class<?> wgType(String name) throws ClassNotFoundException {
			return HookReflection.findClass(this.worldGuardPlugin, name);
		}

		final Class<?> weType(String name) throws ClassNotFoundException {
			return HookReflection.findClass(this.worldEditPlugin, name);
		}

		abstract Object manager(World world) throws ReflectiveOperationException;

		abstract Object applicable(Location location) throws ReflectiveOperationException;

		abstract Object applicable(Chunk chunk) throws ReflectiveOperationException;

		abstract Object flag(String name) throws ReflectiveOperationException;

		@Override
		public Boolean state(Location location, String flagName, Player player)
				throws ReflectiveOperationException {
			return queryState(applicable(location), flagName, player);
		}

		@Override
		public Boolean state(Chunk chunk, String flagName) throws ReflectiveOperationException {
			return queryState(applicable(chunk), flagName, null);
		}

		@Override
		public List<String> regions(Location location) throws ReflectiveOperationException {
			return names(applicable(location));
		}

		@Override
		public List<String> regions(Chunk chunk) throws ReflectiveOperationException {
			return names(applicable(chunk));
		}

		@Override
		public Location center(World world, String regionName) throws ReflectiveOperationException {
			Object regionManager = manager(world);
			if (regionManager == null) return null;
			Object region = HookReflection.invoke(regionManager, "getRegion", regionName);
			if (region == null) return null;

			int[] minimum = coordinates(HookReflection.invoke(region, "getMinimumPoint"));
			int[] maximum = coordinates(HookReflection.invoke(region, "getMaximumPoint"));
			return new Location(world,
					(minimum[0] + maximum[0]) / 2.0D,
					(minimum[1] + maximum[1]) / 2.0D,
					(minimum[2] + maximum[2]) / 2.0D);
		}

		final Boolean queryState(Object applicable, String flagName, Player player)
				throws ReflectiveOperationException {
			if (applicable == null) return null;
			Object stateFlag = flag(flagName);
			if (stateFlag == null || !this.stateFlagType.isInstance(stateFlag)) return null;

			Object localPlayer = null;
			if (player != null) {
				try {
					localPlayer = HookReflection.invoke(this.worldGuardPlugin, "wrapPlayer", player);
				} catch (NoSuchMethodException ignored) {
					Class<?> pluginType = wgType("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
					Object instance = HookReflection.invokeStatic(pluginType, "inst");
					localPlayer = HookReflection.invoke(instance, "wrapPlayer", player);
				}
			}

			Object flags = Array.newInstance(this.stateFlagType, 1);
			Array.set(flags, 0, stateFlag);
			Object result;
			try {
				result = HookReflection.invoke(applicable, "queryState", localPlayer, flags);
			} catch (NoSuchMethodException ignored) {
				result = HookReflection.invoke(applicable, "getFlag", stateFlag);
			}
			if (result == null) return null;
			String name = result instanceof Enum<?> ? ((Enum<?>) result).name() : String.valueOf(result);
			if ("ALLOW".equalsIgnoreCase(name)) return true;
			if ("DENY".equalsIgnoreCase(name)) return false;
			return null;
		}

		final List<String> names(Object applicable) throws ReflectiveOperationException {
			if (!(applicable instanceof Iterable<?>)) return Collections.emptyList();

			Set<String> names = new LinkedHashSet<>();
			Set<String> parents = new LinkedHashSet<>();
			for (Object region : (Iterable<?>) applicable) {
				if (region == null) continue;
				names.add(String.valueOf(HookReflection.invoke(region, "getId")));
				Object parent = HookReflection.invoke(region, "getParent");
				while (parent != null) {
					parents.add(String.valueOf(HookReflection.invoke(parent, "getId")));
					parent = HookReflection.invoke(parent, "getParent");
				}
			}
			names.removeAll(parents);
			return Collections.unmodifiableList(new ArrayList<>(names));
		}

		final int[] coordinates(Object vector) throws ReflectiveOperationException {
			return new int[]{
					coordinate(vector, "getBlockX", "getX"),
					coordinate(vector, "getBlockY", "getY"),
					coordinate(vector, "getBlockZ", "getZ")
			};
		}

		private int coordinate(Object vector, String blockName, String simpleName)
				throws ReflectiveOperationException {
			try {
				return ((Number) HookReflection.invoke(vector, blockName)).intValue();
			} catch (NoSuchMethodException ignored) {
				return ((Number) HookReflection.invoke(vector, simpleName)).intValue();
			}
		}
	}

	private static final class ModernAdapter extends BaseAdapter {
		private final Class<?> blockVectorType;
		private final Class<?> bukkitAdapterType;

		ModernAdapter(Plugin worldGuardPlugin, Plugin worldEditPlugin) throws ClassNotFoundException {
			super(worldGuardPlugin, worldEditPlugin);
			this.blockVectorType = weType("com.sk89q.worldedit.math.BlockVector3");
			this.bukkitAdapterType = weType("com.sk89q.worldedit.bukkit.BukkitAdapter");
		}

		@Override
		Object manager(World world) throws ReflectiveOperationException {
			Object container = container();
			Object adaptedWorld = HookReflection.invokeStatic(this.bukkitAdapterType, "adapt", world);
			return HookReflection.invoke(container, "get", adaptedWorld);
		}

		@Override
		Object applicable(Location location) throws ReflectiveOperationException {
			Object regionManager = manager(location.getWorld());
			if (regionManager == null) return null;
			Object vector;
			try {
				vector = HookReflection.invokeStatic(this.bukkitAdapterType, "asBlockVector", location);
			} catch (NoSuchMethodException ignored) {
				vector = HookReflection.invokeStatic(this.blockVectorType, "at",
						location.getBlockX(), location.getBlockY(), location.getBlockZ());
			}
			return HookReflection.invoke(regionManager, "getApplicableRegions", vector);
		}

		@Override
		Object applicable(Chunk chunk) throws ReflectiveOperationException {
			Object regionManager = manager(chunk.getWorld());
			if (regionManager == null) return null;
			Object first = HookReflection.invokeStatic(this.blockVectorType, "at",
					chunk.getX() << 4, 0, chunk.getZ() << 4);
			Object second = HookReflection.invokeStatic(this.blockVectorType, "at",
					(chunk.getX() << 4) + 15, chunk.getWorld().getMaxHeight(), (chunk.getZ() << 4) + 15);
			Object region = HookReflection.newInstance(
					wgType("com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion"),
					"__rosacore_query__", first, second);
			return HookReflection.invoke(regionManager, "getApplicableRegions", region);
		}

		@Override
		Object flag(String name) throws ReflectiveOperationException {
			Object worldGuard = worldGuard();
			Object registry = HookReflection.invoke(worldGuard, "getFlagRegistry");
			return HookReflection.invoke(registry, "get", name.toLowerCase(Locale.ROOT));
		}

		private Object worldGuard() throws ReflectiveOperationException {
			return HookReflection.invokeStatic(wgType("com.sk89q.worldguard.WorldGuard"), "getInstance");
		}

		private Object container() throws ReflectiveOperationException {
			Object platform = HookReflection.invoke(worldGuard(), "getPlatform");
			return HookReflection.invoke(platform, "getRegionContainer");
		}
	}

	private static final class LegacyAdapter extends BaseAdapter {
		private final Class<?> vectorType;

		LegacyAdapter(Plugin worldGuardPlugin, Plugin worldEditPlugin) throws ClassNotFoundException {
			super(worldGuardPlugin, worldEditPlugin);
			this.vectorType = weType("com.sk89q.worldedit.Vector");
		}

		@Override
		Object manager(World world) throws ReflectiveOperationException {
			return HookReflection.invoke(this.worldGuardPlugin, "getRegionManager", world);
		}

		@Override
		Object applicable(Location location) throws ReflectiveOperationException {
			Object regionManager = manager(location.getWorld());
			if (regionManager == null) return null;
			try {
				return HookReflection.invoke(regionManager, "getApplicableRegions", location);
			} catch (NoSuchMethodException ignored) {
				Object vector = HookReflection.newInstance(this.vectorType,
						location.getBlockX(), location.getBlockY(), location.getBlockZ());
				return HookReflection.invoke(regionManager, "getApplicableRegions", vector);
			}
		}

		@Override
		Object applicable(Chunk chunk) throws ReflectiveOperationException {
			Object regionManager = manager(chunk.getWorld());
			if (regionManager == null) return null;
			Class<?> blockVectorType;
			try {
				blockVectorType = weType("com.sk89q.worldedit.BlockVector");
			} catch (ClassNotFoundException ignored) {
				blockVectorType = this.vectorType;
			}
			Object first = HookReflection.newInstance(blockVectorType,
					chunk.getX() << 4, 0, chunk.getZ() << 4);
			Object second = HookReflection.newInstance(blockVectorType,
					(chunk.getX() << 4) + 15, chunk.getWorld().getMaxHeight(), (chunk.getZ() << 4) + 15);
			Object region = HookReflection.newInstance(
					wgType("com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion"),
					"__rosacore_query__", first, second);
			return HookReflection.invoke(regionManager, "getApplicableRegions", region);
		}

		@Override
		Object flag(String name) throws ReflectiveOperationException {
			try {
				Object registry = HookReflection.invoke(this.worldGuardPlugin, "getFlagRegistry");
				Object flag = HookReflection.invoke(registry, "get", name.toLowerCase(Locale.ROOT));
				if (flag != null) return flag;
			} catch (NoSuchMethodException ignored) {
				try {
					Object registry = HookReflection.readField(this.worldGuardPlugin, "flagRegistry");
					Object flag = HookReflection.invoke(registry, "get", name.toLowerCase(Locale.ROOT));
					if (flag != null) return flag;
				} catch (ReflectiveOperationException ignoredField) {
				}
			}

			Class<?> defaults = wgType("com.sk89q.worldguard.protection.flags.DefaultFlag");
			try {
				Object flags = HookReflection.readStaticField(defaults, "flagsList");
				if (flags != null && flags.getClass().isArray()) {
					int length = Array.getLength(flags);
					for (int index = 0; index < length; index++) {
						Object flag = Array.get(flags, index);
						if (flag != null && name.equalsIgnoreCase(
								String.valueOf(HookReflection.invoke(flag, "getName")))) return flag;
					}
				}
			} catch (NoSuchFieldException ignored) {
			}

			String fieldName = name.toUpperCase(Locale.ROOT).replace('-', '_');
			try {
				return HookReflection.readStaticField(defaults, fieldName);
			} catch (NoSuchFieldException ignored) {
				return null;
			}
		}
	}
}

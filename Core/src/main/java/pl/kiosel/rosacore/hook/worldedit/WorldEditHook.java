package pl.kiosel.rosacore.hook.worldedit;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.hook.RosaHook;
import pl.kiosel.rosacore.hook.internal.HookReflection;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.Objects;

public final class WorldEditHook extends RosaHook {

	private Adapter adapter;

	@Override
	public String getName() {
		return "WorldEdit";
	}

	@Override
	public String[] getPluginDependencies() {
		return new String[]{"WorldEdit"};
	}

	@Override
	protected boolean onEnable(RosaPlugin plugin) throws Exception {
		Plugin worldEdit = getDependencyPlugin("WorldEdit");
		try {
			HookReflection.findClass(worldEdit, "com.sk89q.worldedit.math.BlockVector3");
			this.adapter = new ModernAdapter(worldEdit);
		} catch (ClassNotFoundException ignored) {
			HookReflection.findClass(worldEdit, "com.sk89q.worldedit.CuboidClipboard");
			this.adapter = new LegacyAdapter(worldEdit);
		}
		return true;
	}

	@Override
	protected void onDisable() {
		this.adapter = null;
	}

	public Schematic loadSchematic(File file) throws IOException {
		requireFile(file);
		return requireAdapter().load(file);
	}

	public void pasteSchematic(File file, Location origin) throws IOException {
		pasteSchematic(loadSchematic(file), origin, false, false);
	}

	public void pasteSchematic(Schematic schematic, Location origin) throws IOException {
		pasteSchematic(schematic, origin, false, false);
	}

	public void pasteSchematic(Schematic schematic, Location origin,
							   boolean ignoreAirBlocks, boolean copyEntities) throws IOException {
		Objects.requireNonNull(schematic, "schematic");
		requireWorld(origin, "paste origin");
		Adapter active = requireAdapter();
		if (!active.id().equals(schematic.adapterId))
			throw new IOException("Schematic was loaded by a different WorldEdit API generation");
		active.paste(schematic, origin, ignoreAirBlocks, copyEntities);
	}

	public Schematic saveSchematic(File file, Location firstCorner, Location secondCorner,
								   Location origin) throws IOException {
		return saveSchematic(file, firstCorner, secondCorner, origin, false, false);
	}

	public Schematic saveSchematic(File file, Location firstCorner, Location secondCorner,
								   Location origin, boolean copyEntities,
								   boolean copyBiomes) throws IOException {
		Objects.requireNonNull(file, "file");
		World world = requireWorld(firstCorner, "first corner");
		if (requireWorld(secondCorner, "second corner") != world
				|| requireWorld(origin, "schematic origin") != world)
			throw new IOException("Schematic corners and origin must be in the same world");

		File parent = file.getAbsoluteFile().getParentFile();
		if (parent != null) Files.createDirectories(parent.toPath());
		return requireAdapter().save(file, firstCorner, secondCorner, origin, copyEntities, copyBiomes);
	}

	private Adapter requireAdapter() throws IOException {
		if (!isEnabled() || this.adapter == null)
			throw new IOException("WorldEdit hook is not enabled");
		return this.adapter;
	}

	private static void requireFile(File file) throws IOException {
		Objects.requireNonNull(file, "file");
		if (!file.isFile()) throw new IOException("Schematic does not exist: " + file.getPath());
	}

	private static World requireWorld(Location location, String description) throws IOException {
		if (location == null || location.getWorld() == null)
			throw new IOException(description + " has no world");
		return location.getWorld();
	}

	private interface Adapter {
		String id();

		Schematic load(File file) throws IOException;

		void paste(Schematic schematic, Location origin, boolean ignoreAir, boolean copyEntities)
				throws IOException;

		Schematic save(File file, Location first, Location second, Location origin,
					   boolean copyEntities, boolean copyBiomes) throws IOException;
	}

	private abstract static class BaseAdapter implements Adapter {
		final Plugin worldEditPlugin;

		BaseAdapter(Plugin worldEditPlugin) {
			this.worldEditPlugin = Objects.requireNonNull(worldEditPlugin, "worldEditPlugin");
		}

		final Class<?> type(String name) throws ClassNotFoundException {
			return HookReflection.findClass(this.worldEditPlugin, name);
		}

		final Object vector(Class<?> vectorType, int x, int y, int z) throws ReflectiveOperationException {
			try {
				return HookReflection.invokeStatic(vectorType, "at", x, y, z);
			} catch (NoSuchMethodException ignored) {
				return HookReflection.newInstance(vectorType, x, y, z);
			}
		}

		final int coordinate(Object vector, String blockMethod, String simpleMethod)
				throws ReflectiveOperationException {
			try {
				return ((Number) HookReflection.invoke(vector, blockMethod)).intValue();
			} catch (NoSuchMethodException ignored) {
				return ((Number) HookReflection.invoke(vector, simpleMethod)).intValue();
			}
		}

		final int[] coordinates(Object vector) throws ReflectiveOperationException {
			return new int[]{
					coordinate(vector, "getBlockX", "getX"),
					coordinate(vector, "getBlockY", "getY"),
					coordinate(vector, "getBlockZ", "getZ")
			};
		}

		final IOException failure(String action, Throwable throwable) {
			Throwable cause = throwable;
			while (cause instanceof InvocationTargetException
					&& ((InvocationTargetException) cause).getCause() != null)
				cause = ((InvocationTargetException) cause).getCause();
			if (cause instanceof IOException) return (IOException) cause;
			return new IOException("WorldEdit failed to " + action, cause);
		}

		final void close(Object value) {
			if (value instanceof Closeable) {
				try {
					((Closeable) value).close();
				} catch (IOException ignored) {
				}
				return;
			}
			if (value instanceof AutoCloseable) {
				try {
					((AutoCloseable) value).close();
				} catch (Exception ignored) {
				}
			}
		}
	}

	private static final class ModernAdapter extends BaseAdapter {

		private final Class<?> blockVectorType;

		ModernAdapter(Plugin worldEditPlugin) throws ClassNotFoundException {
			super(worldEditPlugin);
			this.blockVectorType = type("com.sk89q.worldedit.math.BlockVector3");
		}

		@Override
		public String id() {
			return "worldedit-7";
		}

		@Override
		public Schematic load(File file) throws IOException {
			Object reader = null;
			try (FileInputStream input = new FileInputStream(file)) {
				Class<?> formats = type("com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats");
				Object format = HookReflection.invokeStatic(formats, "findByFile", file);
				if (format == null) throw new IOException("Unknown schematic format: " + file.getName());

				reader = HookReflection.invoke(format, "getReader", input);
				Object clipboard = HookReflection.invoke(reader, "read");
				return schematic(clipboard);
			} catch (ReflectiveOperationException | LinkageError exception) {
				throw failure("load schematic " + file.getName(), exception);
			} finally {
				close(reader);
			}
		}

		@Override
		public void paste(Schematic schematic, Location origin, boolean ignoreAir,
						  boolean copyEntities) throws IOException {
			Object editSession = null;
			try {
				Object worldEdit = HookReflection.invokeStatic(type("com.sk89q.worldedit.WorldEdit"), "getInstance");
				Object adaptedWorld = HookReflection.invokeStatic(type("com.sk89q.worldedit.bukkit.BukkitAdapter"),
						"adapt", origin.getWorld());
				editSession = HookReflection.invoke(worldEdit, "newEditSession", adaptedWorld);

				Object holder = HookReflection.newInstance(
						type("com.sk89q.worldedit.session.ClipboardHolder"), schematic.handle);
				Object builder = HookReflection.invoke(holder, "createPaste", editSession);
				builder = HookReflection.invoke(builder, "to", vector(this.blockVectorType,
						origin.getBlockX(), origin.getBlockY(), origin.getBlockZ()));
				builder = HookReflection.invoke(builder, "ignoreAirBlocks", ignoreAir);
				try {
					builder = HookReflection.invoke(builder, "copyEntities", copyEntities);
				} catch (NoSuchMethodException ignored) {
				}
				Object operation = HookReflection.invoke(builder, "build");
				HookReflection.invokeStatic(type("com.sk89q.worldedit.function.operation.Operations"),
						"complete", operation);
			} catch (ReflectiveOperationException | LinkageError exception) {
				throw failure("paste schematic", exception);
			} finally {
				close(editSession);
			}
		}

		@Override
		public Schematic save(File file, Location first, Location second, Location origin,
							  boolean copyEntities, boolean copyBiomes) throws IOException {
			Object writer = null;
			try {
				Class<?> adapterType = type("com.sk89q.worldedit.bukkit.BukkitAdapter");
				Object adaptedWorld = HookReflection.invokeStatic(adapterType, "adapt", first.getWorld());
				Object minimum = vector(this.blockVectorType,
						Math.min(first.getBlockX(), second.getBlockX()),
						Math.min(first.getBlockY(), second.getBlockY()),
						Math.min(first.getBlockZ(), second.getBlockZ()));
				Object maximum = vector(this.blockVectorType,
						Math.max(first.getBlockX(), second.getBlockX()),
						Math.max(first.getBlockY(), second.getBlockY()),
						Math.max(first.getBlockZ(), second.getBlockZ()));
				Object schematicOrigin = vector(this.blockVectorType,
						origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());

				Class<?> regionType = type("com.sk89q.worldedit.regions.CuboidRegion");
				Object region;
				try {
					region = HookReflection.newInstance(regionType, adaptedWorld, minimum, maximum);
				} catch (NoSuchMethodException ignored) {
					region = HookReflection.newInstance(regionType, minimum, maximum);
				}
				Object clipboard = HookReflection.newInstance(
						type("com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard"), region);
				HookReflection.invoke(clipboard, "setOrigin", schematicOrigin);

				Object copy = HookReflection.newInstance(
						type("com.sk89q.worldedit.function.operation.ForwardExtentCopy"),
						adaptedWorld, region, clipboard, HookReflection.invoke(region, "getMinimumPoint"));
				try {
					HookReflection.invoke(copy, "setCopyingEntities", copyEntities);
				} catch (NoSuchMethodException ignored) {
				}
				try {
					HookReflection.invoke(copy, "setCopyingBiomes", copyBiomes);
				} catch (NoSuchMethodException ignored) {
				}
				HookReflection.invokeStatic(type("com.sk89q.worldedit.function.operation.Operations"),
						"complete", copy);

				Object format = findWriteFormat(file);
				try (FileOutputStream output = new FileOutputStream(file)) {
					writer = HookReflection.invoke(format, "getWriter", output);
					HookReflection.invoke(writer, "write", clipboard);
					close(writer);
					writer = null;
				}
				return schematic(clipboard);
			} catch (ReflectiveOperationException | LinkageError exception) {
				throw failure("save schematic " + file.getName(), exception);
			} finally {
				close(writer);
			}
		}

		private Object findWriteFormat(File file) throws ReflectiveOperationException {
			Class<?> formats = type("com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats");
			Object format = HookReflection.invokeStatic(formats, "findByFile", file);
			if (format != null) return format;

			Class<?> builtIn = type("com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat");
			String fileName = file.getName().toLowerCase(java.util.Locale.ROOT);
			if (fileName.endsWith(".schematic")) {
				try {
					return HookReflection.readStaticField(builtIn, "MCEDIT_SCHEMATIC");
				} catch (ReflectiveOperationException ignored) {
				}
			}
			return HookReflection.readStaticField(builtIn, "SPONGE_SCHEMATIC");
		}

		private Schematic schematic(Object clipboard) throws ReflectiveOperationException {
			Object origin = HookReflection.invoke(clipboard, "getOrigin");
			int[] originValues = coordinates(origin);
			int[] minimum = coordinates(HookReflection.invoke(clipboard, "getMinimumPoint"));
			int[] maximum = coordinates(HookReflection.invoke(clipboard, "getMaximumPoint"));
			return new Schematic(id(), clipboard,
					minimum[0] - originValues[0], minimum[1] - originValues[1], minimum[2] - originValues[2],
					maximum[0] - originValues[0], maximum[1] - originValues[1], maximum[2] - originValues[2]);
		}
	}

	private static final class LegacyAdapter extends BaseAdapter {

		private final Class<?> vectorType;
		private final Class<?> clipboardType;

		LegacyAdapter(Plugin worldEditPlugin) throws ClassNotFoundException {
			super(worldEditPlugin);
			this.vectorType = type("com.sk89q.worldedit.Vector");
			this.clipboardType = type("com.sk89q.worldedit.CuboidClipboard");
		}

		@Override
		public String id() {
			return "worldedit-legacy";
		}

		@Override
		public Schematic load(File file) throws IOException {
			try {
				Object clipboard = HookReflection.invokeStaticExact(this.clipboardType, "loadSchematic",
						new Class<?>[]{File.class}, file);
				return schematic(clipboard);
			} catch (ReflectiveOperationException | LinkageError exception) {
				throw failure("load legacy schematic " + file.getName(), exception);
			}
		}

		@Override
		public void paste(Schematic schematic, Location origin, boolean ignoreAir,
						  boolean copyEntities) throws IOException {
			Object editSession = null;
			try {
				editSession = createEditSession(origin.getWorld());
				Object target = vector(this.vectorType,
						origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
				try {
					HookReflection.invoke(schematic.handle, "paste",
							editSession, target, ignoreAir, copyEntities);
				} catch (NoSuchMethodException ignored) {
					HookReflection.invoke(schematic.handle, "paste", editSession, target, ignoreAir);
				}
			} catch (ReflectiveOperationException | LinkageError exception) {
				throw failure("paste legacy schematic", exception);
			} finally {
				close(editSession);
			}
		}

		@Override
		public Schematic save(File file, Location first, Location second, Location origin,
							  boolean copyEntities, boolean copyBiomes) throws IOException {
			Object editSession = null;
			try {
				int minX = Math.min(first.getBlockX(), second.getBlockX());
				int minY = Math.min(first.getBlockY(), second.getBlockY());
				int minZ = Math.min(first.getBlockZ(), second.getBlockZ());
				int maxX = Math.max(first.getBlockX(), second.getBlockX());
				int maxY = Math.max(first.getBlockY(), second.getBlockY());
				int maxZ = Math.max(first.getBlockZ(), second.getBlockZ());

				Object size = vector(this.vectorType, maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
				Object minimum = vector(this.vectorType, minX, minY, minZ);
				Object offset = vector(this.vectorType,
						minX - origin.getBlockX(), minY - origin.getBlockY(), minZ - origin.getBlockZ());
				Object clipboard = HookReflection.newInstance(this.clipboardType, size, minimum, offset);
				editSession = createEditSession(first.getWorld());
				HookReflection.invoke(clipboard, "copy", editSession);
				HookReflection.invokeExact(clipboard, "saveSchematic", new Class<?>[]{File.class}, file);
				return schematic(clipboard);
			} catch (ReflectiveOperationException | LinkageError exception) {
				throw failure("save legacy schematic " + file.getName(), exception);
			} finally {
				close(editSession);
			}
		}

		private Object createEditSession(World world) throws ReflectiveOperationException {
			Object worldEdit = HookReflection.invoke(this.worldEditPlugin, "getWorldEdit");
			Object factory = HookReflection.invoke(worldEdit, "getEditSessionFactory");
			Object adaptedWorld = HookReflection.newInstance(
					type("com.sk89q.worldedit.bukkit.BukkitWorld"), world);
			try {
				return HookReflection.invoke(factory, "getEditSession", adaptedWorld, -1);
			} catch (NoSuchMethodException ignored) {
				return HookReflection.invoke(factory, "getEditSession", adaptedWorld, -1, null);
			}
		}

		private Schematic schematic(Object clipboard) throws ReflectiveOperationException {
			int[] offset = coordinates(HookReflection.invoke(clipboard, "getOffset"));
			int[] size = coordinates(HookReflection.invoke(clipboard, "getSize"));
			return new Schematic(id(), clipboard,
					offset[0], offset[1], offset[2],
					offset[0] + size[0] - 1,
					offset[1] + size[1] - 1,
					offset[2] + size[2] - 1);
		}
	}

	public static final class Schematic {
		private final String adapterId;
		private final Object handle;
		@Getter private final int minimumX;
		@Getter private final int minimumY;
		@Getter private final int minimumZ;
		@Getter private final int maximumX;
		@Getter private final int maximumY;
		@Getter private final int maximumZ;

		private Schematic(String adapterId, Object handle,
						  int minimumX, int minimumY, int minimumZ,
						  int maximumX, int maximumY, int maximumZ) {
			this.adapterId = adapterId;
			this.handle = Objects.requireNonNull(handle, "handle");
			this.minimumX = minimumX;
			this.minimumY = minimumY;
			this.minimumZ = minimumZ;
			this.maximumX = maximumX;
			this.maximumY = maximumY;
			this.maximumZ = maximumZ;
		}

		public long getVolume() {
			return (long) (this.maximumX - this.minimumX + 1)
					* (this.maximumY - this.minimumY + 1)
					* (this.maximumZ - this.minimumZ + 1);
		}
	}
}

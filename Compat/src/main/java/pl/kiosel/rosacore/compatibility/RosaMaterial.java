package pl.kiosel.rosacore.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.version.MinecraftVersion;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class RosaMaterial {

	private static final ConcurrentMap<String, RosaMaterial> CACHE = new ConcurrentHashMap<>();
	private static final Method MATERIAL_IS_ITEM = findMaterialIsItem();
	private static volatile MinecraftVersion detectedServerVersion;

	private final String canonicalName;

	private RosaMaterial(String canonicalName) {
		this.canonicalName = canonicalName;
	}

	public static RosaMaterial of(String input) {
		ParsedInput parsed = ParsedInput.parse(input);
		String canonicalName = MaterialCatalog.canonicalName(
				parsed.name, parsed.data, parsed.explicitlyLegacy);
		RosaMaterial existing = CACHE.get(canonicalName);
		if (existing != null) {
			return existing;
		}
		RosaMaterial created = new RosaMaterial(canonicalName);
		RosaMaterial raced = CACHE.putIfAbsent(canonicalName, created);
		return raced == null ? created : raced;
	}

	public static Optional<RosaMaterial> parse(String input) {
		try {
			return Optional.of(of(input));
		} catch (IllegalArgumentException | NullPointerException exception) {
			return Optional.empty();
		}
	}

	public static RosaMaterial from(Material material) {
		Objects.requireNonNull(material, "material");
		boolean legacy = isLegacyMaterial(material);
		return cached(MaterialCatalog.canonicalName(stripLegacy(material.name()),
				legacy ? 0 : -1, legacy));
	}

	public static RosaMaterial from(ItemStack item) {
		Objects.requireNonNull(item, "item");
		boolean legacy = isLegacyMaterial(item.getType());
		String materialName = stripLegacy(item.getType().name());
		int data = legacy ? item.getDurability() & 0xFFFF : -1;
		return cached(MaterialCatalog.canonicalName(materialName, data, legacy));
	}

	public static RosaMaterial from(Block block) {
		Objects.requireNonNull(block, "block");
		boolean legacy = isLegacyMaterial(block.getType());
		String materialName = stripLegacy(block.getType().name());
		int data = legacy ? block.getData() & 0xFF : -1;
		return cached(MaterialCatalog.canonicalName(materialName, data, legacy));
	}

	public String getCanonicalName() {
		return this.canonicalName;
	}

	public Optional<ResolvedMaterial> resolve() {
		return this.resolve(MaterialTarget.ANY);
	}

	public Optional<ResolvedMaterial> resolveForItem() {
		return this.resolve(MaterialTarget.ITEM);
	}

	public Optional<ResolvedMaterial> resolveForBlock() {
		return this.resolve(MaterialTarget.BLOCK);
	}

	public Optional<ResolvedMaterial> resolve(MaterialTarget target) {
		Objects.requireNonNull(target, "target");
		MinecraftVersion version = runtimeVersion();
		MaterialCatalog.MaterialSpec spec = MaterialCatalog.resolve(this.canonicalName, target,
				version, name -> Material.getMaterial(name) != null);
		if (spec == null) {
			return Optional.empty();
		}
		Material material = Material.getMaterial(spec.name);
		if (material == null || !supportsTarget(material, target)) {
			return Optional.empty();
		}
		return Optional.of(new ResolvedMaterial(this.canonicalName, material, spec.data));
	}

	public boolean isSupported() {
		return this.resolve().isPresent();
	}

	public boolean isSupported(MaterialTarget target) {
		return this.resolve(target).isPresent();
	}

	public Optional<ItemStack> createItem() {
		return this.createItem(1);
	}

	public Optional<ItemStack> createItem(int amount) {
		if (amount <= 0) {
			throw new IllegalArgumentException("Item amount must be positive");
		}
		Optional<ResolvedMaterial> resolved = this.resolveForItem();
		return resolved.map(material -> material.createItem(amount));
	}

	public ItemStack requireItem() {
		return this.requireItem(1);
	}

	public ItemStack requireItem(int amount) {
		return this.resolveForItem()
				.orElseThrow(() -> new UnsupportedRosaMaterialException(
						this.canonicalName, MaterialTarget.ITEM))
				.createItem(amount);
	}

	public ItemStack createItemOr(int amount, RosaMaterial fallback) {
		Objects.requireNonNull(fallback, "fallback");
		Optional<ItemStack> item = this.createItem(amount);
		return item.isPresent() ? item.get() : fallback.requireItem(amount);
	}

	public boolean matches(ItemStack item) {
		if (item == null) {
			return false;
		}
		Optional<ResolvedMaterial> resolved = this.resolveForItem();
		return resolved.isPresent() && resolved.get().matches(item);
	}

	public boolean matches(Material material) {
		if (material == null) {
			return false;
		}
		Optional<ResolvedMaterial> resolved = this.resolve();
		return resolved.isPresent() && resolved.get().matches(material);
	}

	public boolean matches(Block block) {
		if (block == null) {
			return false;
		}
		Optional<ResolvedMaterial> resolved = this.resolveForBlock();
		return resolved.isPresent() && resolved.get().matches(block);
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof RosaMaterial)) {
			return false;
		}
		RosaMaterial other = (RosaMaterial) object;
		return this.canonicalName.equals(other.canonicalName);
	}

	@Override
	public int hashCode() {
		return this.canonicalName.hashCode();
	}

	@Override
	public String toString() {
		return this.canonicalName;
	}

	static MinecraftVersion runtimeVersion() {
		MinecraftVersion cached = detectedServerVersion;
		if (cached != null) {
			return cached;
		}
		try {
			Server server = Bukkit.getServer();
			if (server != null) {
				MinecraftVersion detected = MinecraftVersion.parse(server.getBukkitVersion());
				detectedServerVersion = detected;
				return detected;
			}
		} catch (RuntimeException ignored) {
			// Unit tests and very early bootstrap may not have a Bukkit server yet.
		}
		if (Material.getMaterial("CAVE_AIR") != null) {
			return MinecraftVersion.of(1, 13, 0);
		}
		if (Material.getMaterial("CONCRETE") != null) {
			return MinecraftVersion.of(1, 12, 0);
		}
		if (Material.getMaterial("ELYTRA") != null) {
			return MinecraftVersion.of(1, 9, 0);
		}
		return MinecraftVersion.of(1, 8, 8);
	}

	private static String stripLegacy(String name) {
		return name.startsWith("LEGACY_") ? name.substring("LEGACY_".length()) : name;
	}

	private static boolean isLegacyMaterial(Material material) {
		return material.name().startsWith("LEGACY_") || runtimeVersion().isOlderThan(
				MinecraftVersion.of(1, 13, 0));
	}

	private static boolean supportsTarget(Material material, MaterialTarget target) {
		if (target == MaterialTarget.ANY) {
			return true;
		}
		if (target == MaterialTarget.BLOCK) {
			return material.isBlock();
		}

		if (material.name().endsWith("_STAINED_GLASS_PANE")
				|| material.name().equals("STAINED_GLASS_PANE")) {
			return true;
		}
		if (MATERIAL_IS_ITEM == null) {
			return true;
		}
		try {
			boolean item = Boolean.TRUE.equals(MATERIAL_IS_ITEM.invoke(material));
			return item || Bukkit.getServer() == null;
		} catch (InvocationTargetException exception) {
			return true;
		} catch (IllegalAccessException exception) {
			return false;
		}
	}

	private static Method findMaterialIsItem() {
		try {
			return Material.class.getMethod("isItem");
		} catch (NoSuchMethodException ignored) {
			return null;
		}
	}

	private static RosaMaterial cached(String canonicalName) {
		RosaMaterial existing = CACHE.get(canonicalName);
		if (existing != null) {
			return existing;
		}
		RosaMaterial created = new RosaMaterial(canonicalName);
		RosaMaterial raced = CACHE.putIfAbsent(canonicalName, created);
		return raced == null ? created : raced;
	}

	private static final class ParsedInput {

		private final String name;
		private final int data;
		private final boolean explicitlyLegacy;

		private ParsedInput(String name, int data, boolean explicitlyLegacy) {
			this.name = name;
			this.data = data;
			this.explicitlyLegacy = explicitlyLegacy;
		}

		private static ParsedInput parse(String input) {
			Objects.requireNonNull(input, "input");
			String normalized = input.trim();
			if (normalized.isEmpty()) {
				throw new IllegalArgumentException("Material name cannot be empty");
			}
			if (normalized.regionMatches(true, 0, "minecraft:", 0, "minecraft:".length())) {
				normalized = normalized.substring("minecraft:".length());
			}
			normalized = normalized.replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);

			int data = -1;
			int separator = Math.max(normalized.lastIndexOf(':'), normalized.lastIndexOf(','));
			if (separator >= 0) {
				String dataText = normalized.substring(separator + 1).trim();
				normalized = normalized.substring(0, separator).trim();
				try {
					data = Integer.parseInt(dataText);
				} catch (NumberFormatException exception) {
					throw new IllegalArgumentException("Invalid legacy material data: " + input, exception);
				}
				if (data < 0 || data > 255) {
					throw new IllegalArgumentException("Legacy material data must be between 0 and 255");
				}
			}
			boolean explicitlyLegacy = normalized.startsWith("LEGACY_");
			normalized = stripLegacy(normalized);
			if (!normalized.matches("[A-Z0-9_]+") || normalized.matches("[0-9]+")) {
				throw new IllegalArgumentException("Invalid material name: " + input);
			}
			return new ParsedInput(normalized, data, explicitlyLegacy || data >= 0);
		}
	}
}

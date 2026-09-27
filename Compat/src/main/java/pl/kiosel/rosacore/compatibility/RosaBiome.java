package pl.kiosel.rosacore.compatibility;

import org.bukkit.block.Biome;

import java.util.Objects;
import java.util.Optional;

public final class RosaBiome {

	private static final NameAliases ALIASES = new NameAliases()
			.add("SWAMP", "SWAMPLAND")
			.add("NETHER_WASTES", "HELL")
			.add("THE_END", "SKY")
			.add("WINDSWEPT_HILLS", "EXTREME_HILLS")
			.add("SNOWY_PLAINS", "ICE_MOUNTAINS")
			.add("SNOWY_PLAINS", "ICE_PLAINS")
			.add("MUSHROOM_FIELDS", "MUSHROOM_SHORE")
			.add("MUSHROOM_FIELDS", "MUSHROOM_ISLAND")
			.add("STONY_SHORE", "STONE_BEACH")
			.add("SNOWY_BEACH", "COLD_BEACH")
			.add("DARK_FOREST", "ROOFED_FOREST")
			.add("SNOWY_TAIGA", "COLD_TAIGA")
			.add("OLD_GROWTH_PINE_TAIGA", "MEGA_TAIGA")
			.add("OLD_GROWTH_SPRUCE_TAIGA", "MEGA_SPRUCE_TAIGA")
			.add("WINDSWEPT_FOREST", "EXTREME_HILLS_PLUS")
			.add("BADLANDS", "MESA")
			.add("WOODED_BADLANDS", "MESA_PLATEAU_FOREST")
			.add("BADLANDS", "MESA_PLATEAU")
			.add("SUNFLOWER_PLAINS", "PLAINS_MOUNTAINS")
			.add("SUNFLOWER_PLAINS", "SUNFLOWER_PLAINS")
			.add("ICE_SPIKES", "ICE_PLAINS_SPIKES")
			.add("FLOWER_FOREST", "FLOWER_FOREST")
			.add("SPARSE_JUNGLE", "JUNGLE_EDGE")
			.add("WINDSWEPT_GRAVELLY_HILLS", "EXTREME_HILLS_MOUNTAINS")
			.add("WINDSWEPT_SAVANNA", "SAVANNA_MOUNTAINS")
			.add("ERODED_BADLANDS", "MESA_BRYCE")
			.add("OLD_GROWTH_BIRCH_FOREST", "BIRCH_FOREST_MOUNTAINS")
			.add("DESERT", "DESERT_HILLS")
			.add("FOREST", "FOREST_HILLS")
			.add("TAIGA", "TAIGA_HILLS")
			.add("WINDSWEPT_HILLS", "SMALL_MOUNTAINS")
			.add("JUNGLE", "JUNGLE_HILLS")
			.add("BIRCH_FOREST", "BIRCH_FOREST_HILLS")
			.add("SNOWY_TAIGA", "COLD_TAIGA_HILLS")
			.add("OLD_GROWTH_PINE_TAIGA", "MEGA_TAIGA_HILLS")
			.add("DESERT", "DESERT_MOUNTAINS")
			.add("TAIGA", "TAIGA_MOUNTAINS")
			.add("SWAMP", "SWAMPLAND_MOUNTAINS")
			.add("JUNGLE", "JUNGLE_MOUNTAINS")
			.add("SPARSE_JUNGLE", "JUNGLE_EDGE_MOUNTAINS")
			.add("SNOWY_TAIGA", "COLD_TAIGA_MOUNTAINS")
			.add("WINDSWEPT_SAVANNA", "SAVANNA_PLATEAU_MOUNTAINS")
			.add("WOODED_BADLANDS", "MESA_PLATEAU_FOREST_MOUNTAINS")
			.add("BADLANDS", "MESA_PLATEAU_MOUNTAINS")
			.add("OLD_GROWTH_BIRCH_FOREST", "BIRCH_FOREST_HILLS_MOUNTAINS")
			.add("DARK_FOREST", "ROOFED_FOREST_MOUNTAINS")
			.add("WINDSWEPT_FOREST", "EXTREME_HILLS_PLUS_MOUNTAINS")
			.add("OLD_GROWTH_SPRUCE_TAIGA", "MEGA_SPRUCE_TAIGA_HILLS");

	private final String name;

	private RosaBiome(String name) {
		this.name = name;
	}

	public static RosaBiome of(String name) {
		return new RosaBiome(ALIASES.canonical(name));
	}

	public static Optional<RosaBiome> parse(String name) {
		try {
			return Optional.of(of(name));
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}

	public static RosaBiome from(Biome biome) {
		Objects.requireNonNull(biome, "biome");
		return of(NameAliases.runtimeName(Biome.class, biome));
	}

	public String getName() {
		return this.name;
	}

	public Optional<Biome> resolve() {
		Biome direct = valueOf(this.name);
		if (direct != null) {
			return Optional.of(direct);
		}
		String legacy = ALIASES.legacy(this.name);
		return Optional.ofNullable(legacy == null ? null : valueOf(legacy));
	}

	public boolean isSupported() {
		return this.resolve().isPresent();
	}

	private static Biome valueOf(String name) {
		return NameAliases.staticValue(Biome.class, name);
	}

	@Override
	public String toString() {
		return this.name;
	}
}

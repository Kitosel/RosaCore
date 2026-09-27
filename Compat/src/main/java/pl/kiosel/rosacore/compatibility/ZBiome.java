package pl.kiosel.rosacore.compatibility;

import org.bukkit.block.Biome;

import java.util.Objects;
import java.util.Optional;

public enum ZBiome {
	// <generated-values>
	OCEAN,
	PLAINS,
	DESERT,
	WINDSWEPT_HILLS,
	FOREST,
	TAIGA,
	SWAMP,
	MANGROVE_SWAMP,
	RIVER,
	NETHER_WASTES,
	THE_END,
	FROZEN_OCEAN,
	FROZEN_RIVER,
	SNOWY_PLAINS,
	MUSHROOM_FIELDS,
	BEACH,
	JUNGLE,
	SPARSE_JUNGLE,
	DEEP_OCEAN,
	STONY_SHORE,
	SNOWY_BEACH,
	BIRCH_FOREST,
	DARK_FOREST,
	PALE_GARDEN,
	SNOWY_TAIGA,
	OLD_GROWTH_PINE_TAIGA,
	WINDSWEPT_FOREST,
	SAVANNA,
	SAVANNA_PLATEAU,
	BADLANDS,
	WOODED_BADLANDS,
	SMALL_END_ISLANDS,
	END_MIDLANDS,
	END_HIGHLANDS,
	END_BARRENS,
	WARM_OCEAN,
	LUKEWARM_OCEAN,
	COLD_OCEAN,
	DEEP_LUKEWARM_OCEAN,
	DEEP_COLD_OCEAN,
	DEEP_FROZEN_OCEAN,
	THE_VOID,
	SUNFLOWER_PLAINS,
	WINDSWEPT_GRAVELLY_HILLS,
	FLOWER_FOREST,
	ICE_SPIKES,
	OLD_GROWTH_BIRCH_FOREST,
	OLD_GROWTH_SPRUCE_TAIGA,
	WINDSWEPT_SAVANNA,
	ERODED_BADLANDS,
	BAMBOO_JUNGLE,
	SOUL_SAND_VALLEY,
	CRIMSON_FOREST,
	WARPED_FOREST,
	BASALT_DELTAS,
	DRIPSTONE_CAVES,
	LUSH_CAVES,
	DEEP_DARK,
	SULFUR_CAVES,
	MEADOW,
	GROVE,
	SNOWY_SLOPES,
	FROZEN_PEAKS,
	JAGGED_PEAKS,
	STONY_PEAKS,
	CHERRY_GROVE,
	DAPPLED_FOREST,
	CUSTOM
	// </generated-values>
	;

	public String getName() {
		return this.name();
	}

	public static Optional<ZBiome> match(String input) {
		Optional<RosaBiome> biome = RosaBiome.parse(input);
		return biome.isPresent()
				? ZCompatibility.byName(ZBiome.class, biome.get().getName())
				: Optional.empty();
	}

	public static Optional<ZBiome> parse(String input) {
		return match(input);
	}

	public static ZBiome matchOrThrow(String input) {
		return ZCompatibility.require(ZBiome.class, input, match(input));
	}

	public static ZBiome from(Biome biome) {
		Objects.requireNonNull(biome, "biome");
		return matchOrThrow(RosaBiome.from(biome).getName());
	}

	public Optional<Biome> resolve() {
		return this.delegate().resolve();
	}

	public Optional<Biome> getBiome() {
		return this.resolve();
	}

	public boolean isSupported() {
		return this.delegate().isSupported();
	}

	private RosaBiome delegate() {
		return RosaBiome.of(this.name());
	}
}

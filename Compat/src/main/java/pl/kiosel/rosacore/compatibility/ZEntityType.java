package pl.kiosel.rosacore.compatibility;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.Objects;
import java.util.Optional;

/**
 * Complete entity type enum for Minecraft 1.8.8 through 26.2.
 */
public enum ZEntityType {
	// <generated-values>
	ITEM,
	EXPERIENCE_ORB,
	AREA_EFFECT_CLOUD,
	ELDER_GUARDIAN,
	WITHER_SKELETON,
	STRAY,
	EGG,
	LEASH_KNOT,
	PAINTING,
	ARROW,
	SNOWBALL,
	FIREBALL,
	SMALL_FIREBALL,
	ENDER_PEARL,
	EYE_OF_ENDER,
	SPLASH_POTION,
	LINGERING_POTION,
	EXPERIENCE_BOTTLE,
	ITEM_FRAME,
	WITHER_SKULL,
	TNT,
	FALLING_BLOCK,
	FIREWORK_ROCKET,
	HUSK,
	SPECTRAL_ARROW,
	SHULKER_BULLET,
	DRAGON_FIREBALL,
	ZOMBIE_VILLAGER,
	SKELETON_HORSE,
	ZOMBIE_HORSE,
	ARMOR_STAND,
	DONKEY,
	MULE,
	EVOKER_FANGS,
	EVOKER,
	VEX,
	VINDICATOR,
	ILLUSIONER,
	COMMAND_BLOCK_MINECART,
	MINECART,
	CHEST_MINECART,
	FURNACE_MINECART,
	TNT_MINECART,
	HOPPER_MINECART,
	SPAWNER_MINECART,
	CREEPER,
	SKELETON,
	SPIDER,
	GIANT,
	ZOMBIE,
	SLIME,
	GHAST,
	ZOMBIFIED_PIGLIN,
	ENDERMAN,
	CAVE_SPIDER,
	SILVERFISH,
	BLAZE,
	MAGMA_CUBE,
	ENDER_DRAGON,
	WITHER,
	BAT,
	WITCH,
	ENDERMITE,
	GUARDIAN,
	SHULKER,
	PIG,
	SHEEP,
	COW,
	CHICKEN,
	SQUID,
	WOLF,
	MOOSHROOM,
	SNOW_GOLEM,
	OCELOT,
	IRON_GOLEM,
	HORSE,
	RABBIT,
	POLAR_BEAR,
	LLAMA,
	LLAMA_SPIT,
	PARROT,
	VILLAGER,
	END_CRYSTAL,
	TURTLE,
	PHANTOM,
	TRIDENT,
	COD,
	SALMON,
	PUFFERFISH,
	TROPICAL_FISH,
	DROWNED,
	DOLPHIN,
	CAT,
	PANDA,
	PILLAGER,
	RAVAGER,
	TRADER_LLAMA,
	WANDERING_TRADER,
	FOX,
	BEE,
	HOGLIN,
	PIGLIN,
	STRIDER,
	ZOGLIN,
	PIGLIN_BRUTE,
	AXOLOTL,
	GLOW_ITEM_FRAME,
	GLOW_SQUID,
	GOAT,
	MARKER,
	ALLAY,
	FROG,
	TADPOLE,
	WARDEN,
	CAMEL,
	BLOCK_DISPLAY,
	INTERACTION,
	ITEM_DISPLAY,
	SNIFFER,
	TEXT_DISPLAY,
	BREEZE,
	WIND_CHARGE,
	BREEZE_WIND_CHARGE,
	ARMADILLO,
	BOGGED,
	OMINOUS_ITEM_SPAWNER,
	ACACIA_BOAT,
	ACACIA_CHEST_BOAT,
	BAMBOO_RAFT,
	BAMBOO_CHEST_RAFT,
	BIRCH_BOAT,
	BIRCH_CHEST_BOAT,
	CHERRY_BOAT,
	CHERRY_CHEST_BOAT,
	DARK_OAK_BOAT,
	DARK_OAK_CHEST_BOAT,
	JUNGLE_BOAT,
	JUNGLE_CHEST_BOAT,
	MANGROVE_BOAT,
	MANGROVE_CHEST_BOAT,
	OAK_BOAT,
	OAK_CHEST_BOAT,
	PALE_OAK_BOAT,
	PALE_OAK_CHEST_BOAT,
	SPRUCE_BOAT,
	SPRUCE_CHEST_BOAT,
	CREAKING,
	HAPPY_GHAST,
	COPPER_GOLEM,
	MANNEQUIN,
	CAMEL_HUSK,
	NAUTILUS,
	PARCHED,
	ZOMBIE_NAUTILUS,
	SULFUR_CUBE,
	FISHING_BOBBER,
	LIGHTNING_BOLT,
	CUSHION,
	PLAYER,
	UNKNOWN
	// </generated-values>
	;

	public String getName() {
		return this.name();
	}

	public static Optional<ZEntityType> match(String input) {
		Optional<RosaEntityType> type = RosaEntityType.parse(input);
		return type.isPresent()
				? ZCompatibility.byName(ZEntityType.class, type.get().getName())
				: Optional.empty();
	}

	public static Optional<ZEntityType> parse(String input) {
		return match(input);
	}

	public static ZEntityType matchOrThrow(String input) {
		return ZCompatibility.require(ZEntityType.class, input, match(input));
	}

	public static ZEntityType from(EntityType type) {
		Objects.requireNonNull(type, "type");
		return matchOrThrow(RosaEntityType.from(type).getName());
	}

	public static ZEntityType from(Entity entity) {
		Objects.requireNonNull(entity, "entity");
		return from(entity.getType());
	}

	public Optional<EntityType> resolve() {
		return this.delegate().resolve();
	}

	public Optional<EntityType> getEntityType() {
		return this.resolve();
	}

	public boolean isSupported() {
		return this.delegate().isSupported();
	}

	private RosaEntityType delegate() {
		return RosaEntityType.of(this.name());
	}
}

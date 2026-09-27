package pl.kiosel.rosacore.compatibility;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public enum ZParticle {
	// <generated-values>
	POOF,
	EXPLOSION,
	EXPLOSION_EMITTER,
	FIREWORK,
	BUBBLE,
	SULFUR_BUBBLES,
	NOXIOUS_GAS,
	NOXIOUS_GAS_CLOUD,
	GEYSER,
	GEYSER_BASE,
	GEYSER_POOF,
	GEYSER_PLUME,
	SPLASH,
	FISHING,
	UNDERWATER,
	CRIT,
	ENCHANTED_HIT,
	SMOKE,
	LARGE_SMOKE,
	EFFECT,
	INSTANT_EFFECT,
	ENTITY_EFFECT,
	WITCH,
	DRIPPING_WATER,
	DRIPPING_LAVA,
	ANGRY_VILLAGER,
	HAPPY_VILLAGER,
	MYCELIUM,
	NOTE,
	PORTAL,
	ENCHANT,
	FLAME,
	LAVA,
	CLOUD,
	DUST,
	ITEM_SNOWBALL,
	ITEM_SLIME,
	HEART,
	ITEM,
	BLOCK,
	RAIN,
	ELDER_GUARDIAN,
	DRAGON_BREATH,
	END_ROD,
	DAMAGE_INDICATOR,
	SWEEP_ATTACK,
	FALLING_DUST,
	TOTEM_OF_UNDYING,
	SPIT,
	SQUID_INK,
	BUBBLE_POP,
	CURRENT_DOWN,
	BUBBLE_COLUMN_UP,
	NAUTILUS,
	DOLPHIN,
	SNEEZE,
	CAMPFIRE_COSY_SMOKE,
	CAMPFIRE_SIGNAL_SMOKE,
	COMPOSTER,
	FLASH,
	FALLING_LAVA,
	LANDING_LAVA,
	FALLING_WATER,
	DRIPPING_HONEY,
	FALLING_HONEY,
	LANDING_HONEY,
	FALLING_NECTAR,
	SOUL_FIRE_FLAME,
	ASH,
	CRIMSON_SPORE,
	WARPED_SPORE,
	SOUL,
	DRIPPING_OBSIDIAN_TEAR,
	FALLING_OBSIDIAN_TEAR,
	LANDING_OBSIDIAN_TEAR,
	REVERSE_PORTAL,
	WHITE_ASH,
	DUST_COLOR_TRANSITION,
	VIBRATION,
	FALLING_SPORE_BLOSSOM,
	SPORE_BLOSSOM_AIR,
	SMALL_FLAME,
	SNOWFLAKE,
	DRIPPING_DRIPSTONE_LAVA,
	FALLING_DRIPSTONE_LAVA,
	DRIPPING_DRIPSTONE_WATER,
	FALLING_DRIPSTONE_WATER,
	GLOW_SQUID_INK,
	GLOW,
	WAX_ON,
	WAX_OFF,
	ELECTRIC_SPARK,
	SCRAPE,
	SONIC_BOOM,
	SCULK_SOUL,
	SCULK_CHARGE,
	SCULK_CHARGE_POP,
	SHRIEK,
	CHERRY_LEAVES,
	PALE_OAK_LEAVES,
	TINTED_LEAVES,
	EGG_CRACK,
	DUST_PLUME,
	WHITE_SMOKE,
	GUST,
	SMALL_GUST,
	GUST_EMITTER_LARGE,
	GUST_EMITTER_SMALL,
	TRIAL_SPAWNER_DETECTION,
	TRIAL_SPAWNER_DETECTION_OMINOUS,
	VAULT_CONNECTION,
	INFESTED,
	ITEM_COBWEB,
	DUST_PILLAR,
	BLOCK_CRUMBLE,
	TRAIL,
	OMINOUS_SPAWNING,
	RAID_OMEN,
	TRIAL_OMEN,
	BLOCK_MARKER,
	FIREFLY,
	SULFUR_CUBE_GOO,
	COPPER_FIRE_FLAME,
	PAUSE_MOB_GROWTH,
	RESET_MOB_GROWTH,

	//26.3
	SHELF_MUSHROOM_PARTICLE,
	// </generated-values>
	;

	public String getName() {
		return this.name();
	}

	public static Optional<ZParticle> match(String input) {
		Optional<RosaParticle> particle = RosaParticle.parse(input);
		return particle.isPresent()
				? ZCompatibility.byName(ZParticle.class, particle.get().getName())
				: Optional.empty();
	}

	public static Optional<ZParticle> parse(String input) {
		return match(input);
	}

	public static ZParticle matchOrThrow(String input) {
		return ZCompatibility.require(ZParticle.class, input, match(input));
	}

	public boolean isSupported() {
		return this.delegate().isSupported();
	}

	public boolean spawn(Location location, int count) {
		return this.delegate().spawn(location, count);
	}

	public boolean spawn(Location location, int count, double offsetX, double offsetY,
						 double offsetZ, double extra) {
		return this.delegate().spawn(location, count, offsetX, offsetY, offsetZ, extra);
	}

	public boolean spawn(Player player, Location location, int count) {
		return this.delegate().spawn(player, location, count);
	}

	public boolean spawn(Player player, Location location, int count, double offsetX,
						 double offsetY, double offsetZ, double extra) {
		return this.delegate().spawn(player, location, count, offsetX, offsetY, offsetZ, extra);
	}

	private RosaParticle delegate() {
		return RosaParticle.of(this.name());
	}
}

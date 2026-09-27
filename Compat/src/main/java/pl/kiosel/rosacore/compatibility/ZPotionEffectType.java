package pl.kiosel.rosacore.compatibility;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;
import java.util.Optional;

public enum ZPotionEffectType {
	// <generated-values>
	SPEED,
	SLOWNESS,
	HASTE,
	MINING_FATIGUE,
	STRENGTH,
	INSTANT_HEALTH,
	INSTANT_DAMAGE,
	JUMP_BOOST,
	NAUSEA,
	REGENERATION,
	RESISTANCE,
	FIRE_RESISTANCE,
	WATER_BREATHING,
	INVISIBILITY,
	BLINDNESS,
	NIGHT_VISION,
	HUNGER,
	WEAKNESS,
	POISON,
	WITHER,
	HEALTH_BOOST,
	ABSORPTION,
	SATURATION,
	GLOWING,
	LEVITATION,
	LUCK,
	UNLUCK,
	SLOW_FALLING,
	CONDUIT_POWER,
	DOLPHINS_GRACE,
	BAD_OMEN,
	HERO_OF_THE_VILLAGE,
	DARKNESS,
	TRIAL_OMEN,
	RAID_OMEN,
	WIND_CHARGED,
	WEAVING,
	OOZING,
	INFESTED,
	BREATH_OF_THE_NAUTILUS
	// </generated-values>
	;

	public String getName() {
		return this.name();
	}

	public static Optional<ZPotionEffectType> match(String input) {
		Optional<RosaPotionEffectType> type = RosaPotionEffectType.parse(input);
		return type.isPresent()
				? ZCompatibility.byName(ZPotionEffectType.class, type.get().getName())
				: Optional.empty();
	}

	public static Optional<ZPotionEffectType> parse(String input) {
		return match(input);
	}

	public static ZPotionEffectType matchOrThrow(String input) {
		return ZCompatibility.require(ZPotionEffectType.class, input, match(input));
	}

	public static ZPotionEffectType from(PotionEffectType type) {
		Objects.requireNonNull(type, "type");
		return matchOrThrow(RosaPotionEffectType.from(type).getName());
	}

	public Optional<PotionEffectType> resolve() {
		return this.delegate().resolve();
	}

	public Optional<PotionEffectType> getPotionEffectType() {
		return this.resolve();
	}

	public boolean isSupported() {
		return this.delegate().isSupported();
	}

	public Optional<PotionEffect> create(int durationTicks, int amplifier) {
		return this.delegate().create(durationTicks, amplifier);
	}

	public boolean apply(LivingEntity entity, int durationTicks, int amplifier, boolean force) {
		return this.delegate().apply(entity, durationTicks, amplifier, force);
	}

	private RosaPotionEffectType delegate() {
		return RosaPotionEffectType.of(this.name());
	}
}

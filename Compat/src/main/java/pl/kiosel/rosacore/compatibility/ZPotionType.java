package pl.kiosel.rosacore.compatibility;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.util.*;

/**
 * Base potion types used from Minecraft 1.8.8 through 26.2.
 */
public enum ZPotionType {
	WATER,
	MUNDANE,
	THICK,
	AWKWARD,
	NIGHT_VISION,
	LONG_NIGHT_VISION,
	INVISIBILITY,
	LONG_INVISIBILITY,
	LEAPING("JUMP"),
	LONG_LEAPING,
	STRONG_LEAPING,
	FIRE_RESISTANCE,
	LONG_FIRE_RESISTANCE,
	SWIFTNESS("SPEED"),
	LONG_SWIFTNESS,
	STRONG_SWIFTNESS,
	SLOWNESS,
	LONG_SLOWNESS,
	STRONG_SLOWNESS,
	WATER_BREATHING,
	LONG_WATER_BREATHING,
	HEALING("INSTANT_HEAL"),
	STRONG_HEALING,
	HARMING("INSTANT_DAMAGE"),
	STRONG_HARMING,
	POISON,
	LONG_POISON,
	STRONG_POISON,
	REGENERATION("REGEN"),
	LONG_REGENERATION,
	STRONG_REGENERATION,
	STRENGTH,
	LONG_STRENGTH,
	STRONG_STRENGTH,
	WEAKNESS,
	LONG_WEAKNESS,
	LUCK,
	TURTLE_MASTER,
	LONG_TURTLE_MASTER,
	STRONG_TURTLE_MASTER,
	SLOW_FALLING,
	LONG_SLOW_FALLING,
	WIND_CHARGED,
	WEAVING,
	OOZING,
	INFESTED;

	private final String[] aliases;

	ZPotionType(String... aliases) {
		this.aliases = aliases;
	}

	public String getName() {
		return this.name();
	}

	public static Optional<ZPotionType> match(String input) {
		if (input == null) {
			return Optional.empty();
		}
		String normalized = normalize(input);
		for (ZPotionType type : values()) {
			if (type.name().equals(normalized)) {
				return Optional.of(type);
			}
			for (String alias : type.aliases) {
				if (alias.equals(normalized)) {
					return Optional.of(type);
				}
			}
		}
		return Optional.empty();
	}

	public static Optional<ZPotionType> parse(String input) {
		return match(input);
	}

	public static ZPotionType matchOrThrow(String input) {
		return match(input).orElseThrow(() -> new IllegalArgumentException("Unknown potion type: " + input));
	}

	public static ZPotionType from(PotionType type) {
		Objects.requireNonNull(type, "type");
		return matchOrThrow(type.name());
	}

	public Optional<PotionType> resolve() {
		for (String name : resolutionNames()) {
			try {
				return Optional.of(PotionType.valueOf(name));
			} catch (IllegalArgumentException ignored) {
			}
		}
		return Optional.empty();
	}

	public Optional<PotionType> getPotionType() {
		return resolve();
	}

	public boolean isSupported() {
		return resolve().isPresent();
	}

	public static List<ZPotionType> supported() {
		List<ZPotionType> result = new ArrayList<>();
		for (ZPotionType type : values()) {
			if (type.isSupported()) {
				result.add(type);
			}
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * Creates the fallback effect used when a server has no base-potion API.
	 */
	public Optional<PotionEffect> createEffect(int durationTicks, int amplifier) {
		Optional<ZPotionEffectType> effectType = effectType();
		return effectType.isPresent()
				? effectType.get().create(durationTicks, amplifier)
				: Optional.empty();
	}

	private Optional<ZPotionEffectType> effectType() {
		String base = baseName();
		if ("SWIFTNESS".equals(base)) return Optional.of(ZPotionEffectType.SPEED);
		if ("LEAPING".equals(base)) return Optional.of(ZPotionEffectType.JUMP_BOOST);
		if ("HEALING".equals(base)) return Optional.of(ZPotionEffectType.INSTANT_HEALTH);
		if ("HARMING".equals(base)) return Optional.of(ZPotionEffectType.INSTANT_DAMAGE);
		if ("REGENERATION".equals(base)) return Optional.of(ZPotionEffectType.REGENERATION);
		return ZPotionEffectType.match(base);
	}

	private List<String> resolutionNames() {
		List<String> names = new ArrayList<>();
		names.add(name());
		Collections.addAll(names, aliases);
		if (isLong() || isStrong()) {
			ZPotionType base = valueOf(baseName());
			names.add(base.name());
			Collections.addAll(names, base.aliases);
		}
		return names;
	}

	public boolean isLong() {
		return name().startsWith("LONG_");
	}

	public boolean isStrong() {
		return name().startsWith("STRONG_");
	}

	private String baseName() {
		if (name().startsWith("LONG_")) return name().substring(5);
		if (name().startsWith("STRONG_")) return name().substring(7);
		return name();
	}

	private static String normalize(String value) {
		String normalized = value.trim().toUpperCase(Locale.ROOT)
				.replace('-', '_').replace(' ', '_');
		int separator = normalized.indexOf(':');
		return separator >= 0 ? normalized.substring(separator + 1) : normalized;
	}
}

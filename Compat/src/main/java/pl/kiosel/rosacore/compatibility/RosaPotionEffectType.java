package pl.kiosel.rosacore.compatibility;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

public final class RosaPotionEffectType {

	private static final NameAliases ALIASES = new NameAliases()
			.add("SLOWNESS", "SLOW")
			.add("HASTE", "FAST_DIGGING")
			.add("MINING_FATIGUE", "SLOW_DIGGING")
			.add("STRENGTH", "INCREASE_DAMAGE")
			.add("INSTANT_HEALTH", "HEAL")
			.add("INSTANT_DAMAGE", "HARM")
			.add("JUMP_BOOST", "JUMP")
			.add("NAUSEA", "CONFUSION")
			.add("RESISTANCE", "DAMAGE_RESISTANCE");

	private final String name;

	private RosaPotionEffectType(String name) {
		this.name = name;
	}

	public static RosaPotionEffectType of(String name) {
		return new RosaPotionEffectType(ALIASES.canonical(name));
	}

	public static Optional<RosaPotionEffectType> parse(String name) {
		try {
			return Optional.of(of(name));
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}

	public static RosaPotionEffectType from(PotionEffectType type) {
		Objects.requireNonNull(type, "type");
		String fieldName = staticFieldName(type);
		return of(fieldName == null ? type.getName() : fieldName);
	}

	public String getName() {
		return this.name;
	}

	public Optional<PotionEffectType> resolve() {
		PotionEffectType direct = find(this.name);
		if (direct != null) {
			return Optional.of(direct);
		}
		String legacy = ALIASES.legacy(this.name);
		return Optional.ofNullable(legacy == null ? null : find(legacy));
	}

	public boolean isSupported() {
		return this.resolve().isPresent();
	}

	public Optional<PotionEffect> create(int durationTicks, int amplifier) {
		if (durationTicks < 0 || amplifier < 0) {
			throw new IllegalArgumentException("Potion duration and amplifier cannot be negative");
		}
		Optional<PotionEffectType> type = this.resolve();
		return type.map(value -> new PotionEffect(value, durationTicks, amplifier));
	}

	public boolean apply(LivingEntity entity, int durationTicks, int amplifier, boolean force) {
		Objects.requireNonNull(entity, "entity");
		Optional<PotionEffect> effect = this.create(durationTicks, amplifier);
		return effect.isPresent() && entity.addPotionEffect(effect.get(), force);
	}

	private static PotionEffectType find(String name) {
		PotionEffectType registered = PotionEffectType.getByName(name);
		if (registered != null) {
			return registered;
		}
		try {
			Field field = PotionEffectType.class.getField(name);
			Object value = field.get(null);
			return value instanceof PotionEffectType ? (PotionEffectType) value : null;
		} catch (NoSuchFieldException | IllegalAccessException exception) {
			return null;
		}
	}

	private static String staticFieldName(PotionEffectType type) {
		for (Field field : PotionEffectType.class.getFields()) {
			if (!PotionEffectType.class.isAssignableFrom(field.getType())) {
				continue;
			}
			try {
				if (field.get(null) == type) {
					return field.getName();
				}
			} catch (IllegalAccessException ignored) {
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return this.name;
	}
}

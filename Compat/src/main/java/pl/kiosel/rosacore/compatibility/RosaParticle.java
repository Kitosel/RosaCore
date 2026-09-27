package pl.kiosel.rosacore.compatibility;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class RosaParticle {

	private static final Class<?> PARTICLE_CLASS = findClass("org.bukkit.Particle");
	private static final Method WORLD_SPAWN_METHOD = findSpawnMethod(World.class);
	private static final Method PLAYER_SPAWN_METHOD = findSpawnMethod(Player.class);
	private static final NameAliases PARTICLE_ALIASES = new NameAliases()
			.add("POOF", "EXPLOSION_NORMAL")
			.add("EXPLOSION", "EXPLOSION_LARGE")
			.add("EXPLOSION_EMITTER", "EXPLOSION_HUGE")
			.add("FIREWORK", "FIREWORKS_SPARK")
			.add("BUBBLE", "WATER_BUBBLE")
			.add("SPLASH", "WATER_SPLASH")
			.add("FISHING", "WATER_WAKE")
			.add("UNDERWATER", "SUSPENDED")
			.add("ENCHANTED_HIT", "CRIT_MAGIC")
			.add("SMOKE", "SMOKE_NORMAL")
			.add("LARGE_SMOKE", "SMOKE_LARGE")
			.add("EFFECT", "SPELL")
			.add("INSTANT_EFFECT", "SPELL_INSTANT")
			.add("ENTITY_EFFECT", "SPELL_MOB")
			.add("WITCH", "SPELL_WITCH")
			.add("DRIPPING_WATER", "DRIP_WATER")
			.add("DRIPPING_LAVA", "DRIP_LAVA")
			.add("ANGRY_VILLAGER", "VILLAGER_ANGRY")
			.add("HAPPY_VILLAGER", "VILLAGER_HAPPY")
			.add("MYCELIUM", "TOWN_AURA")
			.add("ENCHANT", "ENCHANTMENT_TABLE")
			.add("DUST", "REDSTONE")
			.add("ITEM_SNOWBALL", "SNOWBALL")
			.add("ITEM_SLIME", "SLIME")
			.add("ITEM", "ITEM_CRACK")
			.add("BLOCK", "BLOCK_CRACK")
			.add("RAIN", "WATER_DROP")
			.add("ELDER_GUARDIAN", "MOB_APPEARANCE")
			.add("TOTEM_OF_UNDYING", "TOTEM");
	private static final NameAliases EFFECT_ALIASES = new NameAliases()
			.add("POOF", "SMALL_SMOKE")
			.add("EXPLOSION", "EXPLOSION")
			.add("EXPLOSION_EMITTER", "EXPLOSION_HUGE")
			.add("FIREWORK", "FIREWORKS_SPARK")
			.add("CRIT", "CRIT")
			.add("ENCHANTED_HIT", "MAGIC_CRIT")
			.add("SMOKE", "PARTICLE_SMOKE")
			.add("LARGE_SMOKE", "LARGE_SMOKE")
			.add("EFFECT", "SPELL")
			.add("INSTANT_EFFECT", "INSTANT_SPELL")
			.add("WITCH", "WITCH_MAGIC")
			.add("DRIPPING_WATER", "WATERDRIP")
			.add("DRIPPING_LAVA", "LAVADRIP")
			.add("ANGRY_VILLAGER", "VILLAGER_THUNDERCLOUD")
			.add("HAPPY_VILLAGER", "HAPPY_VILLAGER")
			.add("ENCHANT", "FLYING_GLYPH")
			.add("FLAME", "FLAME")
			.add("LAVA", "LAVA_POP")
			.add("CLOUD", "CLOUD")
			.add("ITEM_SNOWBALL", "SNOWBALL_BREAK")
			.add("ITEM_SLIME", "SLIME")
			.add("HEART", "HEART")
			.add("PORTAL", "PORTAL")
			.add("NOTE", "NOTE");

	private final String name;

	private RosaParticle(String name) {
		this.name = name;
	}

	public static RosaParticle of(String name) {
		return new RosaParticle(EFFECT_ALIASES.canonical(PARTICLE_ALIASES.canonical(name)));
	}

	public static Optional<RosaParticle> parse(String name) {
		try {
			return Optional.of(of(name));
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}

	public String getName() {
		return this.name;
	}

	public boolean isSupported() {
		return this.resolveModern() != null || this.resolveLegacy() != null;
	}

	public boolean spawn(Location location, int count) {
		return this.spawn(location, count, 0D, 0D, 0D, 0D);
	}

	public boolean spawn(Location location, int count, double offsetX, double offsetY,
						 double offsetZ, double extra) {
		validate(location, count, offsetX, offsetY, offsetZ, extra);
		World world = location.getWorld();
		if (world == null) {
			return false;
		}

		Object particle = this.resolveModern();
		if (particle != null && invokeModern(WORLD_SPAWN_METHOD, world, particle, location, count,
				offsetX, offsetY, offsetZ, extra)) {
			return true;
		}

		Effect effect = this.resolveLegacy();
		if (effect == null) {
			return false;
		}
		int repetitions = Math.max(1, count);
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int index = 0; index < repetitions; index++) {
			Location point = location.clone().add(
					randomOffset(random, offsetX),
					randomOffset(random, offsetY),
					randomOffset(random, offsetZ));
			world.playEffect(point, effect, 0);
		}
		return true;
	}

	/**
	 * Sends the particle only to one player.
	 */
	public boolean spawn(Player player, Location location, int count) {
		return this.spawn(player, location, count, 0D, 0D, 0D, 0D);
	}

	/**
	 * Sends the particle only to one player.
	 */
	public boolean spawn(Player player, Location location, int count, double offsetX,
						 double offsetY, double offsetZ, double extra) {
		Objects.requireNonNull(player, "player");
		validate(location, count, offsetX, offsetY, offsetZ, extra);
		World world = location.getWorld();
		if (world == null || !world.equals(player.getWorld())) {
			return false;
		}

		Object particle = this.resolveModern();
		if (particle != null && invokeModern(PLAYER_SPAWN_METHOD, player, particle, location, count,
				offsetX, offsetY, offsetZ, extra)) {
			return true;
		}

		Effect effect = this.resolveLegacy();
		if (effect == null) {
			return false;
		}
		int repetitions = Math.max(1, count);
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int index = 0; index < repetitions; index++) {
			Location point = location.clone().add(
					randomOffset(random, offsetX),
					randomOffset(random, offsetY),
					randomOffset(random, offsetZ));
			player.playEffect(point, effect, 0);
		}
		return true;
	}

	private Object resolveModern() {
		if (PARTICLE_CLASS == null) {
			return null;
		}
		Object direct = staticValue(PARTICLE_CLASS, this.name);
		if (direct != null) {
			return direct;
		}
		String legacy = PARTICLE_ALIASES.legacy(this.name);
		return legacy == null ? null : staticValue(PARTICLE_CLASS, legacy);
	}

	private Effect resolveLegacy() {
		String effectName = EFFECT_ALIASES.legacy(this.name);
		if (effectName == null) {
			return null;
		}
		try {
			return Effect.valueOf(effectName);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private static boolean invokeModern(Method method, Object receiver, Object particle, Location location, int count,
										double offsetX, double offsetY, double offsetZ, double extra) {
		if (method == null) {
			return false;
		}
		try {
			method.invoke(receiver, particle, location, count, offsetX, offsetY, offsetZ, extra);
			return true;
		} catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ignored) {
			return false;
		}
	}

	private static Method findSpawnMethod(Class<?> receiverType) {
		if (PARTICLE_CLASS == null) {
			return null;
		}
		for (Method method : receiverType.getMethods()) {
			Class<?>[] parameters = method.getParameterTypes();
			if (method.getName().equals("spawnParticle") && parameters.length == 7
					&& parameters[0] == PARTICLE_CLASS && parameters[1] == Location.class
					&& parameters[2] == int.class && parameters[3] == double.class
					&& parameters[4] == double.class && parameters[5] == double.class
					&& parameters[6] == double.class) {
				return method;
			}
		}
		return null;
	}

	private static void validate(Location location, int count, double offsetX, double offsetY,
								 double offsetZ, double extra) {
		Objects.requireNonNull(location, "location");
		if (count < 0) {
			throw new IllegalArgumentException("Particle count cannot be negative");
		}
		requireNonNegativeFinite(offsetX, "Particle X offset");
		requireNonNegativeFinite(offsetY, "Particle Y offset");
		requireNonNegativeFinite(offsetZ, "Particle Z offset");
		requireNonNegativeFinite(extra, "Particle extra value");
	}

	private static void requireNonNegativeFinite(double value, String name) {
		if (value < 0D || Double.isNaN(value) || Double.isInfinite(value)) {
			throw new IllegalArgumentException(name + " must be a finite non-negative number");
		}
	}

	private static Object staticValue(Class<?> type, String name) {
		try {
			return type.getField(name).get(null);
		} catch (ReflectiveOperationException | LinkageError exception) {
			return null;
		}
	}

	private static Class<?> findClass(String name) {
		try {
			return Class.forName(name, false, RosaParticle.class.getClassLoader());
		} catch (ClassNotFoundException exception) {
			return null;
		}
	}

	private static double randomOffset(ThreadLocalRandom random, double offset) {
		return offset == 0D ? 0D : random.nextDouble(-offset, offset);
	}

	@Override
	public String toString() {
		return this.name;
	}
}

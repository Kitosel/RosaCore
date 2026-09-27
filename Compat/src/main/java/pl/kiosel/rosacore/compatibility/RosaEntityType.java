package pl.kiosel.rosacore.compatibility;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.Objects;
import java.util.Optional;

public final class RosaEntityType {

	private static final NameAliases ALIASES = new NameAliases()
			.add("ITEM", "DROPPED_ITEM")
			.add("LEASH_KNOT", "LEASH_HITCH")
			.add("EYE_OF_ENDER", "ENDER_SIGNAL")
			.add("EXPERIENCE_BOTTLE", "THROWN_EXP_BOTTLE")
			.add("TNT", "PRIMED_TNT")
			.add("FIREWORK_ROCKET", "FIREWORK")
			.add("COMMAND_BLOCK_MINECART", "MINECART_COMMAND")
			.add("OAK_BOAT", "BOAT")
			.add("CHEST_MINECART", "MINECART_CHEST")
			.add("FURNACE_MINECART", "MINECART_FURNACE")
			.add("TNT_MINECART", "MINECART_TNT")
			.add("HOPPER_MINECART", "MINECART_HOPPER")
			.add("SPAWNER_MINECART", "MINECART_MOB_SPAWNER")
			.add("ZOMBIFIED_PIGLIN", "PIG_ZOMBIE")
			.add("MOOSHROOM", "MUSHROOM_COW")
			.add("SNOW_GOLEM", "SNOWMAN")
			.add("END_CRYSTAL", "ENDER_CRYSTAL")
			.add("FISHING_BOBBER", "FISHING_HOOK")
			.add("LIGHTNING_BOLT", "LIGHTNING")
			.add("LIGHTNING_BOLT", "WEATHER")
			.add("ENDER_DRAGON", "COMPLEX_PART");

	private final String name;

	private RosaEntityType(String name) {
		this.name = name;
	}

	public static RosaEntityType of(String name) {
		return new RosaEntityType(ALIASES.canonical(name));
	}

	public static Optional<RosaEntityType> parse(String name) {
		try {
			return Optional.of(of(name));
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}

	public static RosaEntityType from(EntityType type) {
		return of(Objects.requireNonNull(type, "type").name());
	}

	public static RosaEntityType from(Entity entity) {
		return from(Objects.requireNonNull(entity, "entity").getType());
	}

	public String getName() {
		return this.name;
	}

	public Optional<EntityType> resolve() {
		EntityType direct = valueOf(this.name);
		if (direct != null) {
			return Optional.of(direct);
		}
		String legacy = ALIASES.legacy(this.name);
		return Optional.ofNullable(legacy == null ? null : valueOf(legacy));
	}

	public boolean isSupported() {
		return this.resolve().isPresent();
	}

	private static EntityType valueOf(String name) {
		return NameAliases.staticValue(EntityType.class, name);
	}

	@Override
	public String toString() {
		return this.name;
	}
}

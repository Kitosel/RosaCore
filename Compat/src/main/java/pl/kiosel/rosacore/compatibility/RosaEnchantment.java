package pl.kiosel.rosacore.compatibility;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

public final class RosaEnchantment {

	private static final NameAliases ALIASES = new NameAliases()
			.add("PROTECTION", "PROTECTION_ENVIRONMENTAL")
			.add("FIRE_PROTECTION", "PROTECTION_FIRE")
			.add("FEATHER_FALLING", "PROTECTION_FALL")
			.add("BLAST_PROTECTION", "PROTECTION_EXPLOSIONS")
			.add("PROJECTILE_PROTECTION", "PROTECTION_PROJECTILE")
			.add("RESPIRATION", "OXYGEN")
			.add("AQUA_AFFINITY", "WATER_WORKER")
			.add("SHARPNESS", "DAMAGE_ALL")
			.add("SMITE", "DAMAGE_UNDEAD")
			.add("BANE_OF_ARTHROPODS", "DAMAGE_ARTHROPODS")
			.add("LOOTING", "LOOT_BONUS_MOBS")
			.add("EFFICIENCY", "DIG_SPEED")
			.add("UNBREAKING", "DURABILITY")
			.add("FORTUNE", "LOOT_BONUS_BLOCKS")
			.add("POWER", "ARROW_DAMAGE")
			.add("PUNCH", "ARROW_KNOCKBACK")
			.add("FLAME", "ARROW_FIRE")
			.add("INFINITY", "ARROW_INFINITE")
			.add("LUCK_OF_THE_SEA", "LUCK");

	private final String name;

	private RosaEnchantment(String name) {
		this.name = name;
	}

	public static RosaEnchantment of(String name) {
		return new RosaEnchantment(ALIASES.canonical(name));
	}

	public static Optional<RosaEnchantment> parse(String name) {
		try {
			return Optional.of(of(name));
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}

	public static RosaEnchantment from(Enchantment enchantment) {
		Objects.requireNonNull(enchantment, "enchantment");
		String fieldName = staticFieldName(enchantment);
		return of(fieldName == null ? enchantment.getName() : fieldName);
	}

	public String getName() {
		return this.name;
	}

	public Optional<Enchantment> resolve() {
		Enchantment direct = find(this.name);
		if (direct != null) {
			return Optional.of(direct);
		}
		String legacy = ALIASES.legacy(this.name);
		return Optional.ofNullable(legacy == null ? null : find(legacy));
	}

	public boolean isSupported() {
		return this.resolve().isPresent();
	}

	public boolean apply(ItemStack item, int level, boolean unsafe) {
		Objects.requireNonNull(item, "item");
		Optional<Enchantment> enchantment = this.resolve();
		if (!enchantment.isPresent()) {
			return false;
		}
		if (unsafe) {
			item.addUnsafeEnchantment(enchantment.get(), level);
		} else {
			item.addEnchantment(enchantment.get(), level);
		}
		return true;
	}

	public boolean remove(ItemStack item) {
		Objects.requireNonNull(item, "item");
		Optional<Enchantment> enchantment = this.resolve();
		return enchantment.isPresent() && item.removeEnchantment(enchantment.get()) > 0;
	}

	private static Enchantment find(String name) {
		Enchantment registered = Enchantment.getByName(name);
		if (registered != null) {
			return registered;
		}
		try {
			Field field = Enchantment.class.getField(name);
			Object value = field.get(null);
			return value instanceof Enchantment ? (Enchantment) value : null;
		} catch (NoSuchFieldException | IllegalAccessException exception) {
			return null;
		}
	}

	private static String staticFieldName(Enchantment enchantment) {
		for (Field field : Enchantment.class.getFields()) {
			if (!Enchantment.class.isAssignableFrom(field.getType())) {
				continue;
			}
			try {
				if (field.get(null) == enchantment) {
					return field.getName();
				}
			} catch (IllegalAccessException ignored) {
				// Public Bukkit fields should be readable; continue if a fork restricts one.
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return this.name;
	}
}

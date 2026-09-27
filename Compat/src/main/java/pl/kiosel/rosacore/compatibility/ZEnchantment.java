package pl.kiosel.rosacore.compatibility;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Optional;

public enum ZEnchantment {
	// <generated-values>
	PROTECTION,
	FIRE_PROTECTION,
	FEATHER_FALLING,
	BLAST_PROTECTION,
	PROJECTILE_PROTECTION,
	RESPIRATION,
	AQUA_AFFINITY,
	THORNS,
	DEPTH_STRIDER,
	FROST_WALKER,
	BINDING_CURSE,
	SHARPNESS,
	SMITE,
	BANE_OF_ARTHROPODS,
	KNOCKBACK,
	FIRE_ASPECT,
	LOOTING,
	SWEEPING_EDGE,
	EFFICIENCY,
	SILK_TOUCH,
	UNBREAKING,
	FORTUNE,
	POWER,
	PUNCH,
	FLAME,
	INFINITY,
	LUCK_OF_THE_SEA,
	LURE,
	LOYALTY,
	IMPALING,
	RIPTIDE,
	CHANNELING,
	MULTISHOT,
	QUICK_CHARGE,
	PIERCING,
	DENSITY,
	BREACH,
	WIND_BURST,
	LUNGE,
	MENDING,
	VANISHING_CURSE,
	SOUL_SPEED,
	SWIFT_SNEAK
	// </generated-values>
	;

	public String getName() {
		return this.name();
	}

	public static Optional<ZEnchantment> match(String input) {
		Optional<RosaEnchantment> enchantment = RosaEnchantment.parse(input);
		return enchantment.isPresent()
				? ZCompatibility.byName(ZEnchantment.class, enchantment.get().getName())
				: Optional.empty();
	}

	public static Optional<ZEnchantment> parse(String input) {
		return match(input);
	}

	public static ZEnchantment matchOrThrow(String input) {
		return ZCompatibility.require(ZEnchantment.class, input, match(input));
	}

	public static ZEnchantment from(Enchantment enchantment) {
		Objects.requireNonNull(enchantment, "enchantment");
		return matchOrThrow(RosaEnchantment.from(enchantment).getName());
	}

	public Optional<Enchantment> resolve() {
		return this.delegate().resolve();
	}

	public Optional<Enchantment> getEnchantment() {
		return this.resolve();
	}

	public boolean isSupported() {
		return this.delegate().isSupported();
	}

	public boolean apply(ItemStack item, int level, boolean unsafe) {
		return this.delegate().apply(item, level, unsafe);
	}

	public boolean remove(ItemStack item) {
		return this.delegate().remove(item);
	}

	private RosaEnchantment delegate() {
		return RosaEnchantment.of(this.name());
	}
}

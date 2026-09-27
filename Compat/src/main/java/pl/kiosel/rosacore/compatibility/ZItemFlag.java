package pl.kiosel.rosacore.compatibility;

import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public enum ZItemFlag {
	// <generated-values>
	HIDE_ENCHANTS,
	HIDE_ATTRIBUTES,
	HIDE_UNBREAKABLE,
	HIDE_DESTROYS,
	HIDE_PLACED_ON,
	HIDE_ADDITIONAL_TOOLTIP,
	HIDE_DYE,
	HIDE_ARMOR_TRIM,
	HIDE_CUSTOM_DATA,
	HIDE_MAX_STACK_SIZE,
	HIDE_MAX_DAMAGE,
	HIDE_DAMAGE,
	HIDE_CUSTOM_NAME,
	HIDE_ITEM_NAME,
	HIDE_ITEM_MODEL,
	HIDE_LORE,
	HIDE_RARITY,
	HIDE_ENCHANTMENTS,
	HIDE_CAN_PLACE_ON,
	HIDE_CAN_BREAK,
	HIDE_ATTRIBUTE_MODIFIERS,
	HIDE_CUSTOM_MODEL_DATA,
	HIDE_TOOLTIP_DISPLAY,
	HIDE_REPAIR_COST,
	HIDE_CREATIVE_SLOT_LOCK,
	HIDE_ENCHANTMENT_GLINT_OVERRIDE,
	HIDE_INTANGIBLE_PROJECTILE,
	HIDE_FOOD,
	HIDE_CONSUMABLE,
	HIDE_USE_REMAINDER,
	HIDE_USE_COOLDOWN,
	HIDE_DAMAGE_RESISTANT,
	HIDE_TOOL,
	HIDE_WEAPON,
	HIDE_ENCHANTABLE,
	HIDE_EQUIPPABLE,
	HIDE_REPAIRABLE,
	HIDE_GLIDER,
	HIDE_TOOLTIP_STYLE,
	HIDE_DEATH_PROTECTION,
	HIDE_BLOCKS_ATTACKS,
	HIDE_STORED_ENCHANTMENTS,
	HIDE_DYED_COLOR,
	HIDE_MAP_COLOR,
	HIDE_MAP_ID,
	HIDE_MAP_DECORATIONS,
	HIDE_MAP_POST_PROCESSING,
	HIDE_CHARGED_PROJECTILES,
	HIDE_BUNDLE_CONTENTS,
	HIDE_POTION_CONTENTS,
	HIDE_POTION_DURATION_SCALE,
	HIDE_SUSPICIOUS_STEW_EFFECTS,
	HIDE_WRITABLE_BOOK_CONTENT,
	HIDE_WRITTEN_BOOK_CONTENT,
	HIDE_TRIM,
	HIDE_DEBUG_STICK_STATE,
	HIDE_ENTITY_DATA,
	HIDE_BUCKET_ENTITY_DATA,
	HIDE_BLOCK_ENTITY_DATA,
	HIDE_INSTRUMENT,
	HIDE_PROVIDES_TRIM_MATERIAL,
	HIDE_OMINOUS_BOTTLE_AMPLIFIER,
	HIDE_JUKEBOX_PLAYABLE,
	HIDE_PROVIDES_BANNER_PATTERNS,
	HIDE_RECIPES,
	HIDE_LODESTONE_TRACKER,
	HIDE_FIREWORK_EXPLOSION,
	HIDE_FIREWORKS,
	HIDE_PROFILE,
	HIDE_NOTE_BLOCK_SOUND,
	HIDE_BANNER_PATTERNS,
	HIDE_BASE_COLOR,
	HIDE_POT_DECORATIONS,
	HIDE_CONTAINER,
	HIDE_BLOCK_STATE,
	HIDE_BEES,
	HIDE_LOCK,
	HIDE_CONTAINER_LOOT,
	HIDE_BREAK_SOUND,
	HIDE_VILLAGER_VARIANT,
	HIDE_WOLF_VARIANT,
	HIDE_WOLF_SOUND_VARIANT,
	HIDE_WOLF_COLLAR,
	HIDE_FOX_VARIANT,
	HIDE_SALMON_SIZE,
	HIDE_PARROT_VARIANT,
	HIDE_TROPICAL_FISH_PATTERN,
	HIDE_TROPICAL_FISH_BASE_COLOR,
	HIDE_TROPICAL_FISH_PATTERN_COLOR,
	HIDE_MOOSHROOM_VARIANT,
	HIDE_RABBIT_VARIANT,
	HIDE_PIG_VARIANT,
	HIDE_COW_VARIANT,
	HIDE_CHICKEN_VARIANT,
	HIDE_FROG_VARIANT,
	HIDE_HORSE_VARIANT,
	HIDE_PAINTING_VARIANT,
	HIDE_LLAMA_VARIANT,
	HIDE_AXOLOTL_VARIANT,
	HIDE_CAT_VARIANT,
	HIDE_CAT_COLLAR,
	HIDE_SHEEP_COLOR,
	HIDE_SHULKER_COLOR
	// </generated-values>
	;

	public String getName() {
		return this.name();
	}

	public static Optional<ZItemFlag> match(String input) {
		Optional<RosaItemFlag> flag = RosaItemFlag.parse(input);
		return flag.isPresent()
				? ZCompatibility.byName(ZItemFlag.class, flag.get().getName())
				: Optional.empty();
	}

	public static Optional<ZItemFlag> parse(String input) {
		return match(input);
	}

	public static ZItemFlag matchOrThrow(String input) {
		return ZCompatibility.require(ZItemFlag.class, input, match(input));
	}

	public static ZItemFlag from(ItemFlag flag) {
		Objects.requireNonNull(flag, "flag");
		return matchOrThrow(RosaItemFlag.from(flag).getName());
	}

	public static List<ZItemFlag> supported() {
		List<ZItemFlag> supported = new ArrayList<>();
		for (ZItemFlag flag : values()) {
			if (flag.isSupported()) {
				supported.add(flag);
			}
		}
		return Collections.unmodifiableList(supported);
	}

	public Optional<ItemFlag> resolve() {
		return this.delegate().resolve();
	}

	public Optional<ItemFlag> getItemFlag() {
		return this.resolve();
	}

	public boolean isSupported() {
		return this.delegate().isSupported();
	}

	public boolean apply(ItemMeta meta) {
		return this.delegate().apply(meta);
	}

	/**
	 * Fluent-builder friendly alias for {@link #apply(ItemMeta)}.
	 */
	public boolean set(ItemMeta meta) {
		return this.apply(meta);
	}

	public boolean apply(ItemStack item) {
		return this.delegate().apply(item);
	}

	public boolean remove(ItemMeta meta) {
		return this.delegate().remove(meta);
	}

	/**
	 * Applies every flag that exists on the running server.
	 */
	public static void hideEverything(ItemMeta meta) {
		Objects.requireNonNull(meta, "meta");
		for (ZItemFlag flag : values()) {
			flag.apply(meta);
		}
	}

	private RosaItemFlag delegate() {
		return RosaItemFlag.of(this.name());
	}
}

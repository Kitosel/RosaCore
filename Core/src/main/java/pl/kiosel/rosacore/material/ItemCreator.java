package pl.kiosel.rosacore.material;

import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Banner;
import org.bukkit.block.BlockState;
import org.bukkit.block.banner.Pattern;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import pl.kiosel.rosacore.compatibility.*;
import pl.kiosel.rosacore.utils.ColorUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

public final class ItemCreator {

	@Getter
	private static String defaultLorePrefix = "&7";

	private final ItemStack source;
	private ZMaterial material;
	private Integer amount;
	private Integer damage;
	private String name;
	private boolean clearName;
	private final List<String> lore = new ArrayList<>();
	private boolean loreTouched;
	private boolean replaceLore;
	private String lorePrefix = defaultLorePrefix;

	private final Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
	private final Map<ZEnchantment, Integer> xEnchantments = new LinkedHashMap<>();
	private boolean clearEnchantments;
	private final Set<ZItemFlag> flags = new LinkedHashSet<>();
	private boolean clearFlags;
	private boolean hideAll;
	private boolean glow;
	private Boolean unbreakable;
	private Boolean glintOverride;
	private Integer customModelData;
	private boolean customModelDataTouched;
	private Integer repairCost;
	private Integer maxStackSize;
	private Integer maxDamage;

	private Color leatherColor;
	private Color potionColor;
	private Color mapColor;
	private Boolean mapScaling;
	private String mapLocationName;
	private Location lodestone;
	private Boolean lodestoneTracked;
	private DyeColor tropicalFishBodyColor;
	private DyeColor tropicalFishPatternColor;
	private String tropicalFishPattern;
	private String axolotlVariant;

	private String localizedName;
	private boolean localizedNameTouched;
	private Boolean hideTooltip;
	private String itemModel;
	private String tooltipStyle;
	private String armorTrimMaterial;
	private String armorTrimPattern;
	private boolean armorTrimTouched;

	private ZPotionType basePotion;
	private boolean extendedPotion;
	private boolean upgradedPotion;
	private final List<PotionEffect> potionEffects = new ArrayList<>();
	private boolean clearPotionEffects;

	private OfflinePlayer skullOwner;
	private String skullOwnerName;
	private SkullTextureType skullTextureType;
	private String skullTexture;

	private String bookTitle;
	private String bookAuthor;
	private final List<String> bookPages = new ArrayList<>();
	private boolean bookPagesTouched;
	private String bookGeneration;
	private final List<String> knowledgeBookRecipes = new ArrayList<>();
	private boolean knowledgeBookRecipesTouched;

	private final List<FireworkEffect> fireworkEffects = new ArrayList<>();
	private Integer fireworkPower;
	private boolean clearFireworkEffects;

	private DyeColor bannerBaseColor;
	private final List<Pattern> bannerPatterns = new ArrayList<>();
	private boolean bannerPatternsTouched;

	private final List<ItemStack> chargedProjectiles = new ArrayList<>();
	private final List<ItemStack> bundleItems = new ArrayList<>();
	private final List<PotionEffect> stewEffects = new ArrayList<>();

	private final List<AttributeSpec> attributes = new ArrayList<>();
	private boolean clearAttributes;
	private final List<NbtOperation> nbtOperations = new ArrayList<>();
	private final List<Consumer<ItemMeta>> metaEditors = new ArrayList<>();
	private final List<TypedMetaEditor<?>> typedMetaEditors = new ArrayList<>();
	private final List<ReflectiveCall> reflectiveMetaCalls = new ArrayList<>();
	private final List<Consumer<ItemStack>> itemEditors = new ArrayList<>();

	private ItemCreator(ItemStack source) {
		this.source = Objects.requireNonNull(source, "source").clone();
	}

	public static ItemCreator of(ZMaterial material) {
		Objects.requireNonNull(material, "material");
		ItemStack item = material.requireItem();
		if (item == null) {
			throw new IllegalArgumentException("Material " + material + " is not supported on this server version");
		}
		return new ItemCreator(item).material(material);
	}

	public static ItemCreator of(Material material) {
		Objects.requireNonNull(material, "material");
		return of(ZMaterial.from(material));
	}

	public static ItemCreator of(String material) {
		Objects.requireNonNull(material, "material");
		ZMaterial matched = ZMaterial.match(material)
				.orElseThrow(() -> new IllegalArgumentException("Unknown material: " + material));
		return of(matched);
	}

	public static ItemCreator of(ItemStack item) {
		return new ItemCreator(Objects.requireNonNull(item, "item"));
	}

	public static ItemCreator potion(ZMaterial material) {
		Objects.requireNonNull(material, "material");
		if (!material.name().endsWith("POTION")) {
			throw new IllegalArgumentException("Expected POTION, SPLASH_POTION or LINGERING_POTION, got " + material);
		}
		return of(material);
	}

	public static ItemCreator potion(ZPotionEffectType effect, int durationTicks, int amplifier) {
		return potion(ZMaterial.POTION).potionEffect(effect, durationTicks, amplifier);
	}

	public static ItemCreator spawnEgg(ZEntityType entityType) {
		Objects.requireNonNull(entityType, "entityType");
		String materialName = entityType.name() + "_SPAWN_EGG";
		ZMaterial egg = ZMaterial.match(materialName)
				.orElseThrow(() -> new IllegalArgumentException("No spawn egg material for " + entityType));
		return of(egg);
	}

	public static ItemCreator playerHead() {
		return of(ZMaterial.PLAYER_HEAD);
	}

	public static ItemCreator headFromTextureValue(String textureValue) {
		return playerHead().skullTexture(textureValue);
	}

	public static ItemCreator headFromTextureUrl(String textureUrl) {
		return playerHead().skullTextureUrl(textureUrl);
	}

	public static ItemCreator headFromTextureHash(String textureHash) {
		return playerHead().skullTextureHash(textureHash);
	}

	public static ItemCreator book(ZMaterial material) {
		Objects.requireNonNull(material, "material");
		if (material != ZMaterial.WRITABLE_BOOK && material != ZMaterial.WRITTEN_BOOK
				&& material != ZMaterial.ENCHANTED_BOOK && material != ZMaterial.KNOWLEDGE_BOOK) {
			throw new IllegalArgumentException("Expected a book material, got " + material);
		}
		return of(material);
	}

	public static void setDefaultLorePrefix(String defaultLorePrefix) {
		ItemCreator.defaultLorePrefix = defaultLorePrefix;
	}

	public ItemCreator material(ZMaterial material) {
		this.material = Objects.requireNonNull(material, "material");
		return this;
	}

	public ItemCreator material(Material material) {
		return material(ZMaterial.from(Objects.requireNonNull(material, "material")));
	}

	public ItemCreator material(String material) {
		return material(ZMaterial.match(Objects.requireNonNull(material, "material"))
				.orElseThrow(() -> new IllegalArgumentException("Unknown material: " + material)));
	}

	public ItemCreator amount(int amount) {
		if (amount < 1) {
			throw new IllegalArgumentException("Amount must be at least 1");
		}
		this.amount = amount;
		return this;
	}

	public ItemCreator damage(int damage) {
		if (damage < 0) {
			throw new IllegalArgumentException("Damage cannot be negative");
		}
		this.damage = damage;
		return this;
	}

	public ItemCreator name(String name) {
		this.name = Objects.requireNonNull(name, "name");
		this.clearName = false;
		return this;
	}

	public ItemCreator clearName() {
		this.name = null;
		this.clearName = true;
		return this;
	}

	public ItemCreator lore(String... lines) {
		return lore(Arrays.asList(lines));
	}

	public ItemCreator lore(Collection<String> lines) {
		if (lines == null || lines.isEmpty()) {
			return this;
		}
		this.loreTouched = true;
		for (String line : lines) {
			addLoreLines(line);
		}
		return this;
	}

	public ItemCreator setLore(String... lines) {
		return setLore(Arrays.asList(lines));
	}

	public ItemCreator setLore(Collection<String> lines) {
		this.lore.clear();
		this.replaceLore = true;
		this.loreTouched = true;
		return lore(lines);
	}

	public ItemCreator clearLore() {
		this.lore.clear();
		this.replaceLore = true;
		this.loreTouched = true;
		return this;
	}

	public ItemCreator lorePrefix(String lorePrefix) {
		this.lorePrefix = lorePrefix;
		return this;
	}

	public ItemCreator enchant(ZEnchantment enchantment) {
		return enchant(enchantment, 1);
	}

	public ItemCreator enchant(ZEnchantment enchantment, int level) {
		this.xEnchantments.put(Objects.requireNonNull(enchantment, "enchantment"), level);
		return this;
	}

	public ItemCreator enchant(Enchantment enchantment) {
		return enchant(enchantment, 1);
	}

	public ItemCreator enchant(Enchantment enchantment, int level) {
		this.enchantments.put(Objects.requireNonNull(enchantment, "enchantment"), level);
		return this;
	}

	public ItemCreator enchant(String enchantment, int level) {
		ZEnchantment matched = ZEnchantment.match(Objects.requireNonNull(enchantment, "enchantment"))
				.orElseThrow(() -> new IllegalArgumentException("Unknown enchantment: " + enchantment));
		return enchant(matched, level);
	}

	public ItemCreator clearEnchantments() {
		this.clearEnchantments = true;
		this.enchantments.clear();
		this.xEnchantments.clear();
		return this;
	}

	public ItemCreator flags(ZItemFlag... flags) {
		this.flags.addAll(Arrays.asList(flags));
		return this;
	}

	public ItemCreator flags(ItemFlag... flags) {
		for (ItemFlag flag : flags) {
			this.flags.add(ZItemFlag.from(Objects.requireNonNull(flag, "flag")));
		}
		return this;
	}

	public ItemCreator flag(String flag) {
		ZItemFlag matched = ZItemFlag.match(Objects.requireNonNull(flag, "flag"))
				.orElseThrow(() -> new IllegalArgumentException("Unknown item flag: " + flag));
		this.flags.add(matched);
		return this;
	}

	public ItemCreator clearFlags() {
		this.clearFlags = true;
		this.flags.clear();
		return this;
	}

	public ItemCreator hideAll() {
		this.hideAll = true;
		return this;
	}

	public ItemCreator hideAttributes() {
		return flags(ZItemFlag.HIDE_ATTRIBUTES);
	}

	public ItemCreator glow() {
		return glow(true);
	}

	public ItemCreator nbtBoolean(String key, boolean value) {
		return tag(key, value);
	}

	public ItemCreator nbtString(String key, String value) {
		return tag(key, value);
	}

	public ItemCreator nbtInteger(String key, Integer value) {
		return tag(key, Objects.requireNonNull(value, "value"));
	}

	public ItemCreator nbtUUID(String key, UUID value) {
		return tag(key, value);
	}

	public ItemCreator glow(boolean glow) {
		this.glow = glow;
		return this;
	}

	public ItemCreator unbreakable() {
		return unbreakable(true);
	}

	public ItemCreator unbreakable(boolean unbreakable) {
		this.unbreakable = unbreakable;
		return this;
	}

	public ItemCreator glintOverride(boolean glint) {
		this.glintOverride = glint;
		return this;
	}

	public ItemCreator customModelData(Integer customModelData) {
		this.customModelData = customModelData;
		this.customModelDataTouched = true;
		return this;
	}

	public ItemCreator repairCost(int repairCost) {
		if (repairCost < 0) {
			throw new IllegalArgumentException("Repair cost cannot be negative");
		}
		this.repairCost = repairCost;
		return this;
	}

	public ItemCreator maxStackSize(int maxStackSize) {
		if (maxStackSize < 1 || maxStackSize > 99) {
			throw new IllegalArgumentException("Max stack size must be between 1 and 99");
		}
		this.maxStackSize = maxStackSize;
		return this;
	}

	public ItemCreator maxDamage(int maxDamage) {
		if (maxDamage < 1) {
			throw new IllegalArgumentException("Max damage must be positive");
		}
		this.maxDamage = maxDamage;
		return this;
	}

	public ItemCreator color(Color color) {
		this.leatherColor = Objects.requireNonNull(color, "color");
		this.potionColor = color;
		this.mapColor = color;
		return this;
	}

	public ItemCreator leatherColor(Color color) {
		this.leatherColor = Objects.requireNonNull(color, "color");
		return this;
	}

	public ItemCreator potionColor(Color color) {
		this.potionColor = Objects.requireNonNull(color, "color");
		return this;
	}

	public ItemCreator mapColor(Color color) {
		this.mapColor = Objects.requireNonNull(color, "color");
		return this;
	}

	public ItemCreator mapScaling(boolean scaling) {
		this.mapScaling = scaling;
		return this;
	}

	public ItemCreator mapLocationName(String locationName) {
		this.mapLocationName = locationName;
		return this;
	}

	public ItemCreator lodestone(Location location, boolean tracked) {
		this.lodestone = Objects.requireNonNull(location, "location");
		this.lodestoneTracked = tracked;
		return this;
	}

	public ItemCreator tropicalFish(DyeColor bodyColor, DyeColor patternColor, String pattern) {
		this.tropicalFishBodyColor = Objects.requireNonNull(bodyColor, "bodyColor");
		this.tropicalFishPatternColor = Objects.requireNonNull(patternColor, "patternColor");
		this.tropicalFishPattern = Objects.requireNonNull(pattern, "pattern");
		return this;
	}

	public ItemCreator axolotlVariant(String variant) {
		this.axolotlVariant = Objects.requireNonNull(variant, "variant");
		return this;
	}

	public ItemCreator localizedName(String localizedName) {
		this.localizedName = localizedName;
		this.localizedNameTouched = true;
		return this;
	}

	public ItemCreator hideTooltip(boolean hideTooltip) {
		this.hideTooltip = hideTooltip;
		return this;
	}

	public ItemCreator itemModel(String namespacedKey) {
		this.itemModel = Objects.requireNonNull(namespacedKey, "namespacedKey");
		return this;
	}

	public ItemCreator tooltipStyle(String namespacedKey) {
		this.tooltipStyle = Objects.requireNonNull(namespacedKey, "namespacedKey");
		return this;
	}

	public ItemCreator armorTrim(String material, String pattern) {
		this.armorTrimMaterial = Objects.requireNonNull(material, "material");
		this.armorTrimPattern = Objects.requireNonNull(pattern, "pattern");
		this.armorTrimTouched = true;
		return this;
	}

	public ItemCreator clearArmorTrim() {
		this.armorTrimMaterial = null;
		this.armorTrimPattern = null;
		this.armorTrimTouched = true;
		return this;
	}

	public ItemCreator basePotion(ZPotionType potion) {
		return basePotion(potion, false, false);
	}

	public ItemCreator basePotion(ZPotionType potion, boolean extended, boolean upgraded) {
		if (extended && upgraded) {
			throw new IllegalArgumentException("A potion cannot be both extended and upgraded");
		}
		this.basePotion = Objects.requireNonNull(potion, "potion");
		this.extendedPotion = extended;
		this.upgradedPotion = upgraded;
		return this;
	}

	public ItemCreator potionEffect(ZPotionEffectType effectType, int durationTicks, int amplifier) {
		Objects.requireNonNull(effectType, "effectType");
		if (durationTicks < 1) {
			throw new IllegalArgumentException("Potion duration must be positive");
		}
		if (amplifier < 0) {
			throw new IllegalArgumentException("Potion amplifier cannot be negative");
		}
		PotionEffect effect = effectType.create(durationTicks, amplifier)
				.orElseThrow(() -> new IllegalArgumentException(
						"Potion effect " + effectType + " is unsupported on this server"));
		return potionEffect(effect);
	}

	public ItemCreator potionEffect(PotionEffect effect) {
		this.potionEffects.add(Objects.requireNonNull(effect, "effect"));
		return this;
	}

	public ItemCreator clearPotionEffects() {
		this.clearPotionEffects = true;
		this.potionEffects.clear();
		return this;
	}

	public ItemCreator skullOwner(OfflinePlayer owner) {
		this.skullOwner = Objects.requireNonNull(owner, "owner");
		this.skullOwnerName = null;
		return this;
	}

	public ItemCreator skullOwner(String ownerName) {
		this.skullOwnerName = Objects.requireNonNull(ownerName, "ownerName");
		this.skullOwner = null;
		return this;
	}

	public ItemCreator skullTexture(String textureValue) {
		this.skullTextureType = SkullTextureType.VALUE;
		this.skullTexture = Objects.requireNonNull(textureValue, "textureValue");
		return this;
	}

	public ItemCreator skullTextureUrl(String textureUrl) {
		this.skullTextureType = SkullTextureType.URL;
		this.skullTexture = Objects.requireNonNull(textureUrl, "textureUrl");
		return this;
	}

	public ItemCreator skullTextureHash(String textureHash) {
		this.skullTextureType = SkullTextureType.HASH;
		this.skullTexture = Objects.requireNonNull(textureHash, "textureHash");
		return this;
	}

	public ItemCreator bookTitle(String title) {
		this.bookTitle = title;
		return this;
	}

	public ItemCreator bookAuthor(String author) {
		this.bookAuthor = author;
		return this;
	}

	public ItemCreator bookPages(String... pages) {
		return bookPages(Arrays.asList(pages));
	}

	public ItemCreator bookPages(Collection<String> pages) {
		this.bookPagesTouched = true;
		this.bookPages.addAll(Objects.requireNonNull(pages, "pages"));
		return this;
	}

	public ItemCreator clearBookPages() {
		this.bookPagesTouched = true;
		this.bookPages.clear();
		return this;
	}

	public ItemCreator bookGeneration(String generation) {
		this.bookGeneration = generation;
		return this;
	}

	public ItemCreator knowledgeBookRecipe(String namespacedKey) {
		this.knowledgeBookRecipesTouched = true;
		this.knowledgeBookRecipes.add(Objects.requireNonNull(namespacedKey, "namespacedKey"));
		return this;
	}

	public ItemCreator knowledgeBookRecipes(Collection<String> namespacedKeys) {
		this.knowledgeBookRecipesTouched = true;
		this.knowledgeBookRecipes.addAll(Objects.requireNonNull(namespacedKeys, "namespacedKeys"));
		return this;
	}

	public ItemCreator clearKnowledgeBookRecipes() {
		this.knowledgeBookRecipesTouched = true;
		this.knowledgeBookRecipes.clear();
		return this;
	}

	public ItemCreator fireworkEffect(FireworkEffect effect) {
		this.fireworkEffects.add(Objects.requireNonNull(effect, "effect"));
		return this;
	}

	public ItemCreator fireworkEffects(Collection<FireworkEffect> effects) {
		this.fireworkEffects.addAll(Objects.requireNonNull(effects, "effects"));
		return this;
	}

	public ItemCreator clearFireworkEffects() {
		this.clearFireworkEffects = true;
		this.fireworkEffects.clear();
		return this;
	}

	public ItemCreator fireworkPower(int power) {
		if (power < 0 || power > 4) {
			throw new IllegalArgumentException("Firework power must be between 0 and 4");
		}
		this.fireworkPower = power;
		return this;
	}

	public ItemCreator bannerBaseColor(DyeColor color) {
		this.bannerBaseColor = Objects.requireNonNull(color, "color");
		return this;
	}

	public ItemCreator bannerPattern(Pattern pattern) {
		this.bannerPatternsTouched = true;
		this.bannerPatterns.add(Objects.requireNonNull(pattern, "pattern"));
		return this;
	}

	public ItemCreator bannerPatterns(Collection<Pattern> patterns) {
		this.bannerPatternsTouched = true;
		this.bannerPatterns.addAll(Objects.requireNonNull(patterns, "patterns"));
		return this;
	}

	public ItemCreator clearBannerPatterns() {
		this.bannerPatternsTouched = true;
		this.bannerPatterns.clear();
		return this;
	}

	public ItemCreator chargedProjectile(ItemStack projectile) {
		this.chargedProjectiles.add(Objects.requireNonNull(projectile, "projectile").clone());
		return this;
	}

	public ItemCreator bundleItem(ItemStack item) {
		this.bundleItems.add(Objects.requireNonNull(item, "item").clone());
		return this;
	}

	public ItemCreator suspiciousStewEffect(PotionEffect effect) {
		this.stewEffects.add(Objects.requireNonNull(effect, "effect"));
		return this;
	}

	public ItemCreator attribute(String attribute, double amount) {
		return attribute(attribute, "metacore-" + UUID.randomUUID(), amount,
				AttributeOperation.ADD_NUMBER, AttributeSlot.HAND);
	}

	public ItemCreator attribute(String attribute, String modifierName, double amount,
								 AttributeOperation operation, AttributeSlot slot) {
		this.attributes.add(new AttributeSpec(
				Objects.requireNonNull(attribute, "attribute"),
				Objects.requireNonNull(modifierName, "modifierName"),
				amount,
				Objects.requireNonNull(operation, "operation"),
				Objects.requireNonNull(slot, "slot")
		));
		return this;
	}

	public ItemCreator clearAttributes() {
		this.clearAttributes = true;
		this.attributes.clear();
		return this;
	}

	public ItemCreator tag(String key, String value) {
		this.nbtOperations.add(new NbtOperation(key, NbtType.STRING, value));
		return this;
	}

	public ItemCreator tag(String key, int value) {
		this.nbtOperations.add(new NbtOperation(key, NbtType.INTEGER, value));
		return this;
	}

	public ItemCreator tag(String key, double value) {
		this.nbtOperations.add(new NbtOperation(key, NbtType.DOUBLE, value));
		return this;
	}

	public ItemCreator tag(String key, boolean value) {
		this.nbtOperations.add(new NbtOperation(key, NbtType.BOOLEAN, value));
		return this;
	}

	public ItemCreator tag(String key, long value) {
		this.nbtOperations.add(new NbtOperation(key, NbtType.LONG, value));
		return this;
	}

	public ItemCreator tag(String key, UUID value) {
		this.nbtOperations.add(new NbtOperation(key, NbtType.UUID, Objects.requireNonNull(value, "value")));
		return this;
	}

	public ItemCreator removeTag(String key) {
		this.nbtOperations.add(new NbtOperation(key, NbtType.REMOVE, null));
		return this;
	}

	public ItemCreator editMeta(Consumer<ItemMeta> editor) {
		this.metaEditors.add(Objects.requireNonNull(editor, "editor"));
		return this;
	}

	public <T extends ItemMeta> ItemCreator editMeta(Class<T> type, Consumer<T> editor) {
		this.typedMetaEditors.add(new TypedMetaEditor<>(
				Objects.requireNonNull(type, "type"), Objects.requireNonNull(editor, "editor")));
		return this;
	}

	public ItemCreator invokeMeta(String method, Object... arguments) {
		this.reflectiveMetaCalls.add(new ReflectiveCall(
				Objects.requireNonNull(method, "method"), arguments == null ? new Object[0] : arguments.clone()));
		return this;
	}

	public ItemCreator editItem(Consumer<ItemStack> editor) {
		this.itemEditors.add(Objects.requireNonNull(editor, "editor"));
		return this;
	}

	public ItemStack makeMenuItem() {
		this.hideAll = true;
		return make();
	}

	public ItemStack make() {
		ItemStack item = source.clone();
		if (material != null) {
			ItemStack changed = material.setType(item);
			if (changed == null) {
				throw new IllegalArgumentException("Material " + material + " is unsupported on this server");
			}
			item = changed;
		}

		if (item.getType() == Material.AIR) {
			if (amount != null) {
				item.setAmount(amount);
			}
			return item;
		}

		if (skullTextureType != null) {
			ItemStack texturedHead = createTexturedHead();
			item = ZMaterial.PLAYER_HEAD.setType(item);
			item.setItemMeta(texturedHead.getItemMeta());
		}

		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			applyCommonMeta(meta);
			applySpecialMeta(meta);
			applyEditors(meta);
			item.setItemMeta(meta);
		}

		applyDamage(item);
		item = applyNbt(item);

		if (amount != null) {
			item.setAmount(amount);
		}
		for (Consumer<ItemStack> editor : itemEditors) {
			editor.accept(item);
		}
		return item;
	}

	public void give(Player player) {
		Objects.requireNonNull(player, "player");
		Map<Integer, ItemStack> leftovers = player.getInventory().addItem(make());
		for (ItemStack leftover : leftovers.values()) {
			player.getWorld().dropItemNaturally(player.getLocation(), leftover);
		}
	}

	public void drop(Location location) {
		Objects.requireNonNull(location, "location");
		if (location.getWorld() == null) {
			throw new IllegalArgumentException("Location has no world");
		}
		location.getWorld().dropItemNaturally(location, make());
	}

	public Map<String, Object> serialize() {
		return make().serialize();
	}

	public static ItemCreator deserialize(Map<String, Object> serialized) {
		ItemStack item = ItemStack.deserialize(Objects.requireNonNull(serialized, "serialized"));
		if (item == null) {
			throw new IllegalArgumentException("Could not deserialize item");
		}
		return of(item);
	}

	private void applyCommonMeta(ItemMeta meta) {
		if (clearName) {
			meta.setDisplayName(null);
		} else if (name != null) {
			meta.setDisplayName(format(name));
		}

		if (loreTouched) {
			List<String> finalLore = new ArrayList<>();
			if (!replaceLore && meta.getLore() != null) {
				finalLore.addAll(meta.getLore());
			}
			for (String line : lore) {
				finalLore.add(format((lorePrefix == null ? "" : lorePrefix) + line));
			}
			meta.setLore(finalLore);
		}

		if (clearEnchantments) {
			for (Enchantment enchantment : new ArrayList<>(meta.getEnchants().keySet())) {
				meta.removeEnchant(enchantment);
			}
			if (meta instanceof EnchantmentStorageMeta) {
				EnchantmentStorageMeta storage = (EnchantmentStorageMeta) meta;
				for (Enchantment enchantment : new ArrayList<>(storage.getStoredEnchants().keySet())) {
					storage.removeStoredEnchant(enchantment);
				}
			}
		}

		for (Map.Entry<ZEnchantment, Integer> entry : xEnchantments.entrySet()) {
			entry.getKey().getEnchantment().ifPresent(enchantment -> addEnchant(meta, enchantment, entry.getValue()));
		}
		for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
			addEnchant(meta, entry.getKey(), entry.getValue());
		}

		if (glow && enchantments.isEmpty() && xEnchantments.isEmpty()) {
			Optional<Enchantment> unbreakingEnchant = ZEnchantment.UNBREAKING.getEnchantment();
			if (unbreakingEnchant.isPresent()) {
				addEnchant(meta, unbreakingEnchant.get(), 1);
				flags.add(ZItemFlag.HIDE_ENCHANTS);
			}
		}

		if (clearFlags && !meta.getItemFlags().isEmpty()) {
			meta.removeItemFlags(meta.getItemFlags().toArray(new ItemFlag[0]));
		}
		if (hideAll) {
			ZItemFlag.hideEverything(meta);
		}
		for (ZItemFlag flag : flags) {
			if (flag != null && flag.isSupported()) {
				flag.set(meta);
			}
		}

		if (unbreakable != null) {
			invokeCompatible(meta, "setUnbreakable", unbreakable);
			if (unbreakable && ZItemFlag.HIDE_UNBREAKABLE.isSupported()) {
				ZItemFlag.HIDE_UNBREAKABLE.set(meta);
			}
		}
		if (glintOverride != null) {
			invokeCompatible(meta, "setEnchantmentGlintOverride", glintOverride);
		}
		if (customModelDataTouched) {
			invokeCompatible(meta, "setCustomModelData", customModelData);
		}
		if (localizedNameTouched) {
			invokeCompatible(meta, "setLocalizedName", localizedName == null ? null : format(localizedName));
		}
		if (hideTooltip != null) {
			invokeCompatible(meta, "setHideTooltip", hideTooltip);
		}
		if (itemModel != null) {
			Object key = createNamespacedKey(itemModel);
			if (key != null) {
				invokeCompatible(meta, "setItemModel", key);
			}
		}
		if (tooltipStyle != null) {
			Object key = createNamespacedKey(tooltipStyle);
			if (key != null) {
				invokeCompatible(meta, "setTooltipStyle", key);
			}
		}
		if (maxStackSize != null) {
			invokeCompatible(meta, "setMaxStackSize", maxStackSize);
		}
		if (maxDamage != null) {
			invokeCompatible(meta, "setMaxDamage", maxDamage);
		}
		if (repairCost != null && meta instanceof Repairable) {
			((Repairable) meta).setRepairCost(repairCost);
		}

		if (clearAttributes) {
			invokeCompatible(meta, "setAttributeModifiers", (Object) null);
		}
		if (!attributes.isEmpty() && classExists("org.bukkit.attribute.Attribute")) {
			AttributeSupport.apply(meta, attributes);
		}
		if (armorTrimTouched) {
			applyArmorTrim(meta);
		}
	}

	private void applySpecialMeta(ItemMeta meta) {
		if (meta instanceof LeatherArmorMeta && leatherColor != null) {
			((LeatherArmorMeta) meta).setColor(leatherColor);
		}

		if (meta instanceof PotionMeta) {
			PotionMeta potionMeta = (PotionMeta) meta;
			if (clearPotionEffects) {
				potionMeta.clearCustomEffects();
			}
			if (basePotion != null) {
				applyBasePotion(potionMeta);
			}
			for (PotionEffect effect : potionEffects) {
				potionMeta.addCustomEffect(effect, true);
			}
			if (potionColor != null) {
				invokeCompatible(potionMeta, "setColor", potionColor);
			}
		}

		if (meta instanceof SkullMeta) {
			applySkullOwner((SkullMeta) meta);
		}

		if (meta instanceof BookMeta) {
			BookMeta bookMeta = (BookMeta) meta;
			if (bookTitle != null) {
				bookMeta.setTitle(format(bookTitle));
			}
			if (bookAuthor != null) {
				bookMeta.setAuthor(format(bookAuthor));
			}
			if (bookPagesTouched) {
				List<String> pages = new ArrayList<>();
				for (String page : bookPages) {
					pages.add(format(page));
				}
				bookMeta.setPages(pages);
			}
			if (bookGeneration != null) {
				applyEnumMethod(bookMeta, "setGeneration", "org.bukkit.inventory.meta.BookMeta$Generation", bookGeneration);
			}
		}

		if (meta instanceof FireworkMeta) {
			FireworkMeta fireworkMeta = (FireworkMeta) meta;
			if (clearFireworkEffects) {
				fireworkMeta.clearEffects();
			}
			if (!fireworkEffects.isEmpty()) {
				fireworkMeta.addEffects(fireworkEffects);
			}
			if (fireworkPower != null) {
				fireworkMeta.setPower(fireworkPower);
			}
		} else if (meta instanceof FireworkEffectMeta && !fireworkEffects.isEmpty()) {
			((FireworkEffectMeta) meta).setEffect(fireworkEffects.get(0));
		}

		applyBanner(meta);

		if (meta instanceof MapMeta) {
			MapMeta mapMeta = (MapMeta) meta;
			if (mapScaling != null) {
				mapMeta.setScaling(mapScaling);
			}
			if (mapColor != null) {
				invokeCompatible(mapMeta, "setColor", mapColor);
			}
			if (mapLocationName != null) {
				invokeCompatible(mapMeta, "setLocationName", mapLocationName);
			}
		}

		if (lodestone != null) {
			invokeCompatible(meta, "setLodestone", lodestone);
		}
		if (lodestoneTracked != null) {
			invokeCompatible(meta, "setLodestoneTracked", lodestoneTracked);
		}
		if (tropicalFishBodyColor != null) {
			invokeCompatible(meta, "setBodyColor", tropicalFishBodyColor);
		}
		if (tropicalFishPatternColor != null) {
			invokeCompatible(meta, "setPatternColor", tropicalFishPatternColor);
		}
		if (tropicalFishPattern != null) {
			applyEnumMethod(meta, "setPattern", "org.bukkit.entity.TropicalFish$Pattern", tropicalFishPattern);
		}
		if (axolotlVariant != null) {
			applyEnumMethod(meta, "setVariant", "org.bukkit.entity.Axolotl$Variant", axolotlVariant);
		}

		if (knowledgeBookRecipesTouched) {
			List<Object> recipes = new ArrayList<>();
			for (String recipe : knowledgeBookRecipes) {
				Object key = createNamespacedKey(recipe);
				if (key != null) {
					recipes.add(key);
				}
			}
			invokeCompatible(meta, "setRecipes", recipes);
		}

		if (!chargedProjectiles.isEmpty()) {
			for (ItemStack projectile : chargedProjectiles) {
				invokeCompatible(meta, "addChargedProjectile", projectile.clone());
			}
		}
		if (!bundleItems.isEmpty()) {
			for (ItemStack bundleItem : bundleItems) {
				invokeCompatible(meta, "addItem", bundleItem.clone());
			}
		}
		if (!stewEffects.isEmpty()) {
			for (PotionEffect effect : stewEffects) {
				invokeCompatible(meta, "addCustomEffect", effect, true);
			}
		}
	}

	private void applyEditors(ItemMeta meta) {
		for (Consumer<ItemMeta> editor : metaEditors) {
			editor.accept(meta);
		}
		for (TypedMetaEditor<?> editor : typedMetaEditors) {
			editor.accept(meta);
		}
		for (ReflectiveCall call : reflectiveMetaCalls) {
			invokeCompatible(meta, call.method, call.arguments);
		}
	}

	private void applySkullOwner(SkullMeta meta) {
		if (skullOwner != null) {
			if (!invokeCompatible(meta, "setOwningPlayer", skullOwner)) {
				meta.setOwner(skullOwner.getName());
			}
		} else if (skullOwnerName != null) {
			meta.setOwner(skullOwnerName);
		}
	}

	private void applyBanner(ItemMeta meta) {
		if (meta instanceof BannerMeta) {
			BannerMeta bannerMeta = (BannerMeta) meta;
			if (bannerBaseColor != null) {
				invokeCompatible(bannerMeta, "setBaseColor", bannerBaseColor);
			}
			if (bannerPatternsTouched) {
				bannerMeta.setPatterns(new ArrayList<>(bannerPatterns));
			}
			return;
		}

		if (meta instanceof BlockStateMeta && (bannerBaseColor != null || bannerPatternsTouched)) {
			BlockStateMeta blockMeta = (BlockStateMeta) meta;
			BlockState state = blockMeta.getBlockState();
			if (state instanceof Banner) {
				Banner banner = (Banner) state;
				if (bannerBaseColor != null) {
					invokeCompatible(banner, "setBaseColor", bannerBaseColor);
				}
				if (bannerPatternsTouched) {
					banner.setPatterns(new ArrayList<>(bannerPatterns));
				}
				blockMeta.setBlockState(banner);
			}
		}
	}

	private void applyBasePotion(PotionMeta meta) {
		Optional<PotionType> resolved = basePotion.getPotionType();
		if (!resolved.isPresent()) {
			return;
		}
		PotionType type = resolved.get();

		boolean effectiveExtended = extendedPotion || basePotion.isLong();
		boolean effectiveUpgraded = upgradedPotion || basePotion.isStrong();
		PotionType modernType = getModernPotionType(type, effectiveExtended, effectiveUpgraded);
		if (invokeCompatible(meta, "setBasePotionType", modernType)) {
			return;
		}

		try {
			Class<?> potionDataClass = Class.forName("org.bukkit.potion.PotionData");
			Constructor<?> constructor = potionDataClass.getConstructor(PotionType.class, boolean.class, boolean.class);
			Object potionData = constructor.newInstance(type, effectiveExtended, effectiveUpgraded);
			invokeCompatible(meta, "setBasePotionData", potionData);
		} catch (ReflectiveOperationException | LinkageError ignored) {
			basePotion.createEffect(20 * 180, effectiveUpgraded ? 1 : 0)
					.ifPresent(effect -> meta.addCustomEffect(effect, true));
		}
	}

	private void applyArmorTrim(ItemMeta meta) {
		if (armorTrimMaterial == null || armorTrimPattern == null) {
			invokeCompatible(meta, "setTrim", (Object) null);
			return;
		}
		try {
			Class<?> materialClass = Class.forName("org.bukkit.inventory.meta.trim.TrimMaterial");
			Class<?> patternClass = Class.forName("org.bukkit.inventory.meta.trim.TrimPattern");
			Class<?> trimClass = Class.forName("org.bukkit.inventory.meta.trim.ArmorTrim");
			Object material = materialClass.getField(enumName(armorTrimMaterial)).get(null);
			Object pattern = patternClass.getField(enumName(armorTrimPattern)).get(null);
			Constructor<?> constructor = trimClass.getConstructor(materialClass, patternClass);
			invokeCompatible(meta, "setTrim", constructor.newInstance(material, pattern));
		} catch (ReflectiveOperationException | IllegalArgumentException | LinkageError ignored) {
		}
	}

	private void applyDamage(ItemStack item) {
		if (damage == null) {
			return;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta != null && invokeCompatible(meta, "setDamage", damage)) {
			item.setItemMeta(meta);
		} else {
			item.setDurability((short) Math.min(damage, Short.MAX_VALUE));
		}
	}

	private ItemStack applyNbt(ItemStack item) {
		if (nbtOperations.isEmpty() && unbreakable == null) {
			return item;
		}
		if (unbreakable != null && !hasBooleanMetaMethod(item, "isUnbreakable")) {
			if (unbreakable) {
				ItemTag.set(item, "Unbreakable", true);
			} else {
				ItemTag.remove(item, "Unbreakable");
			}
		}
		for (NbtOperation operation : nbtOperations) {
			switch (operation.type) {
				case STRING:
					ItemTag.set(item, operation.key, (String) operation.value);
					break;
				case INTEGER:
					ItemTag.set(item, operation.key, (Integer) operation.value);
					break;
				case LONG:
					ItemTag.set(item, operation.key, (Long) operation.value);
					break;
				case DOUBLE:
					ItemTag.set(item, operation.key, (Double) operation.value);
					break;
				case BOOLEAN:
					ItemTag.set(item, operation.key, (Boolean) operation.value);
					break;
				case UUID:
					ItemTag.set(item, operation.key, (UUID) operation.value);
					break;
				case REMOVE:
					ItemTag.remove(item, operation.key);
					break;
				default:
					throw new IllegalStateException("Unhandled NBT type " + operation.type);
			}
		}
		return item;
	}

	private ItemStack createTexturedHead() {
		switch (skullTextureType) {
			case VALUE:
				return SkullItemCreator.byTextureValue(skullTexture);
			case URL:
				return SkullItemCreator.byTextureUrl(skullTexture);
			case HASH:
				return SkullItemCreator.byTextureUrlHash(skullTexture);
			default:
				throw new IllegalStateException("Unhandled skull texture type " + skullTextureType);
		}
	}

	private static void addEnchant(ItemMeta meta, Enchantment enchantment, int level) {
		if (meta instanceof EnchantmentStorageMeta) {
			((EnchantmentStorageMeta) meta).addStoredEnchant(enchantment, level, true);
		} else {
			meta.addEnchant(enchantment, level, true);
		}
	}

	private PotionType getModernPotionType(PotionType baseType, boolean extended, boolean upgraded) {
		if (baseType.name().startsWith("LONG_") || baseType.name().startsWith("STRONG_")) {
			return baseType;
		}
		if (!extended && !upgraded) {
			return baseType;
		}
		String prefix = extended ? "LONG_" : "STRONG_";
		try {
			return PotionType.valueOf(prefix + baseType.name());
		} catch (IllegalArgumentException ignored) {
			return baseType;
		}
	}

	private void addLoreLines(String line) {
		if (line == null) {
			return;
		}
		Collections.addAll(lore, line.split("\\n", -1));
	}

	private static String format(String text) {
		return ColorUtils.color(text == null ? "" : text);
	}

	private static boolean hasBooleanMetaMethod(ItemStack item, String methodName) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return false;
		}
		try {
			Method method = meta.getClass().getMethod(methodName);
			Object value = method.invoke(meta);
			return value instanceof Boolean && (Boolean) value;
		} catch (ReflectiveOperationException | LinkageError ignored) {
			return false;
		}
	}

	private static boolean invokeCompatible(Object target, String methodName, Object... arguments) {
		if (target == null) {
			return false;
		}
		Object[] args = arguments == null ? new Object[0] : arguments;
		for (Method method : target.getClass().getMethods()) {
			if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
				continue;
			}
			if (!parametersMatch(method.getParameterTypes(), args)) {
				continue;
			}
			try {
				method.invoke(target, args);
				return true;
			} catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException |
					 LinkageError ignored) {
			}
		}
		return false;
	}

	private static boolean parametersMatch(Class<?>[] parameterTypes, Object[] arguments) {
		for (int i = 0; i < parameterTypes.length; i++) {
			Object argument = arguments[i];
			if (argument == null) {
				if (parameterTypes[i].isPrimitive()) {
					return false;
				}
				continue;
			}
			Class<?> expected = wrap(parameterTypes[i]);
			if (!expected.isAssignableFrom(argument.getClass())) {
				return false;
			}
		}
		return true;
	}

	private static Class<?> wrap(Class<?> type) {
		if (!type.isPrimitive()) {
			return type;
		}
		if (type == boolean.class) return Boolean.class;
		if (type == byte.class) return Byte.class;
		if (type == short.class) return Short.class;
		if (type == int.class) return Integer.class;
		if (type == long.class) return Long.class;
		if (type == float.class) return Float.class;
		if (type == double.class) return Double.class;
		if (type == char.class) return Character.class;
		return type;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void applyEnumMethod(Object target, String methodName, String enumClassName, String value) {
		try {
			Class<?> enumClass = Class.forName(enumClassName);
			Object enumValue = Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), enumName(value));
			invokeCompatible(target, methodName, enumValue);
		} catch (ClassNotFoundException | IllegalArgumentException | LinkageError ignored) {
		}
	}

	private static boolean classExists(String className) {
		try {
			Class.forName(className, false, ItemCreator.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException | LinkageError ignored) {
			return false;
		}
	}

	private static Object createNamespacedKey(String input) {
		try {
			Class<?> keyClass = Class.forName("org.bukkit.NamespacedKey");
			try {
				Method fromString = keyClass.getMethod("fromString", String.class);
				Object key = fromString.invoke(null, input);
				if (key != null) {
					return key;
				}
			} catch (NoSuchMethodException ignored) {
			}

			String[] split = input.toLowerCase().split(":", 2);
			String namespace = split.length == 2 ? split[0] : "minecraft";
			String key = split.length == 2 ? split[1] : split[0];
			Constructor<?> constructor = keyClass.getConstructor(String.class, String.class);
			return constructor.newInstance(namespace, key);
		} catch (ReflectiveOperationException | IllegalArgumentException | LinkageError ignored) {
			return null;
		}
	}

	private static String enumName(String value) {
		int separator = value.indexOf(':');
		String name = separator >= 0 ? value.substring(separator + 1) : value;
		return name.trim().toUpperCase().replace('-', '_').replace(' ', '_');
	}

	public enum AttributeOperation {
		ADD_NUMBER,
		ADD_SCALAR,
		MULTIPLY_SCALAR_1
	}

	public enum AttributeSlot {
		HAND,
		OFF_HAND,
		HEAD,
		CHEST,
		LEGS,
		FEET
	}

	private enum SkullTextureType {
		VALUE,
		URL,
		HASH
	}

	private enum NbtType {
		STRING,
		INTEGER,
		LONG,
		DOUBLE,
		BOOLEAN,
		UUID,
		REMOVE
	}

	private static final class NbtOperation {
		private final String key;
		private final NbtType type;
		private final Object value;

		private NbtOperation(String key, NbtType type, Object value) {
			this.key = Objects.requireNonNull(key, "key");
			this.type = Objects.requireNonNull(type, "type");
			this.value = value;
		}
	}

	private static final class ReflectiveCall {
		private final String method;
		private final Object[] arguments;

		private ReflectiveCall(String method, Object[] arguments) {
			this.method = method;
			this.arguments = arguments;
		}
	}

	private static final class AttributeSpec {
		private final String attribute;
		private final String modifierName;
		private final double amount;
		private final AttributeOperation operation;
		private final AttributeSlot slot;

		private AttributeSpec(String attribute, String modifierName, double amount,
							  AttributeOperation operation, AttributeSlot slot) {
			this.attribute = attribute;
			this.modifierName = modifierName;
			this.amount = amount;
			this.operation = operation;
			this.slot = slot;
		}
	}

	private static final class TypedMetaEditor<T extends ItemMeta> {
		private final Class<T> type;
		private final Consumer<T> editor;

		private TypedMetaEditor(Class<T> type, Consumer<T> editor) {
			this.type = type;
			this.editor = editor;
		}

		private void accept(ItemMeta meta) {
			if (type.isInstance(meta)) {
				editor.accept(type.cast(meta));
			}
		}
	}

	private static final class AttributeSupport {
		private static void apply(ItemMeta meta, List<AttributeSpec> specs) {
			for (AttributeSpec spec : specs) {
				try {
					Class<?> attributeClass = Class.forName("org.bukkit.attribute.Attribute");
					Object attribute = enumValue(attributeClass, spec.attribute);
					if (attribute == null) {
						continue;
					}
					Class<?> modifierClass = Class.forName("org.bukkit.attribute.AttributeModifier");
					Class<?> operationClass = Class.forName("org.bukkit.attribute.AttributeModifier$Operation");
					Object operation = enumValue(operationClass, spec.operation.name());
					Object modifier = createModifier(modifierClass, operationClass, operation, spec);
					if (modifier != null) {
						Method add = meta.getClass().getMethod("addAttributeModifier", attributeClass, modifierClass);
						add.invoke(meta, attribute, modifier);
					}
				} catch (ReflectiveOperationException | IllegalArgumentException | LinkageError ignored) {
				}
			}
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private static Object enumValue(Class<?> enumClass, String value) {
			Class<? extends Enum> type = enumClass.asSubclass(Enum.class);
			String name = enumName(value);
			String[] candidates = name.startsWith("GENERIC_")
					? new String[]{name, name.substring(8)}
					: new String[]{name, "GENERIC_" + name};
			for (String candidate : candidates) {
				try {
					return Enum.valueOf(type, candidate);
				} catch (IllegalArgumentException ignored) {
				}
			}
			return null;
		}

		private static Object createModifier(Class<?> modifierClass, Class<?> operationClass,
											 Object operation, AttributeSpec spec)
				throws ReflectiveOperationException {
			if (operation == null) {
				return null;
			}
			Class<?> slotClass = Class.forName("org.bukkit.inventory.EquipmentSlot");
			Object slot = enumValue(slotClass, spec.slot.name());
			try {
				Constructor<?> constructor = modifierClass.getConstructor(
						String.class, double.class, operationClass, slotClass);
				return constructor.newInstance(spec.modifierName, spec.amount, operation, slot);
			} catch (NoSuchMethodException ignored) {
			}
			try {
				Constructor<?> constructor = modifierClass.getConstructor(
						UUID.class, String.class, double.class, operationClass, slotClass);
				return constructor.newInstance(UUID.randomUUID(), spec.modifierName,
						spec.amount, operation, slot);
			} catch (NoSuchMethodException ignored) {
			}
			try {
				Constructor<?> constructor = modifierClass.getConstructor(
						String.class, double.class, operationClass);
				return constructor.newInstance(spec.modifierName, spec.amount, operation);
			} catch (NoSuchMethodException ignored) {
				Constructor<?> constructor = modifierClass.getConstructor(
						UUID.class, String.class, double.class, operationClass);
				return constructor.newInstance(UUID.randomUUID(), spec.modifierName,
						spec.amount, operation);
			}
		}
	}
}

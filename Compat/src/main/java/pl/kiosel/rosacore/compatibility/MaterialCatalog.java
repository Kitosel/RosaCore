package pl.kiosel.rosacore.compatibility;

import pl.kiosel.rosacore.version.MinecraftVersion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MaterialCatalog {

	private static final MinecraftVersion V1_8 = MinecraftVersion.of(1, 8, 0);
	private static final MinecraftVersion V1_9 = MinecraftVersion.of(1, 9, 0);
	private static final MinecraftVersion V1_10 = MinecraftVersion.of(1, 10, 0);
	private static final MinecraftVersion V1_11 = MinecraftVersion.of(1, 11, 0);
	private static final MinecraftVersion V1_12 = MinecraftVersion.of(1, 12, 0);
	private static final MinecraftVersion V1_12_2 = MinecraftVersion.of(1, 12, 2);
	private static final MinecraftVersion V1_13 = MinecraftVersion.of(1, 13, 0);

	private static final Map<String, Descriptor> DESCRIPTORS = new HashMap<>();
	private static final Map<LegacyKey, String> REVERSE = new HashMap<>();

	static {
		registerColors();
		registerWoodFamilies();
		registerStoneVariants();
		registerPlants();
		registerRenames();
		registerTargetSpecificMaterials();
		registerLegacy18Aliases();
	}

	private MaterialCatalog() {
	}

	static String canonicalName(String inputName, int data, boolean explicitlyLegacy) {
		String name = inputName.startsWith("LEGACY_")
				? inputName.substring("LEGACY_".length()) : inputName;
		if (!explicitlyLegacy && data < 0 && DESCRIPTORS.containsKey(name)) {
			return name;
		}
		if (data >= 0) {
			String exact = REVERSE.get(new LegacyKey(name, data));
			if (exact != null) {
				return exact;
			}
		} else {
			String defaultVariant = REVERSE.get(new LegacyKey(name, 0));
			if (defaultVariant != null) {
				return defaultVariant;
			}
		}
		String withoutData = REVERSE.get(new LegacyKey(name, -1));
		return withoutData == null ? name : withoutData;
	}

	static MaterialSpec resolve(String canonicalName, MaterialTarget target,
								MinecraftVersion version, MaterialNameLookup lookup) {
		Descriptor descriptor = DESCRIPTORS.get(canonicalName);
		boolean flattened = !version.isOlderThan(V1_13);

		if (flattened && lookup.exists(canonicalName)) {
			return new MaterialSpec(canonicalName, -1);
		}
		if (descriptor != null) {
			for (Candidate candidate : descriptor.candidates) {
				if (candidate.supports(target, version) && lookup.exists(candidate.name)) {
					return new MaterialSpec(candidate.name, candidate.data);
				}
			}
		}
		if (lookup.exists(canonicalName)) {
			return new MaterialSpec(canonicalName, -1);
		}
		return null;
	}

	private static void registerColors() {
		String[] colors = {
				"WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE",
				"YELLOW", "LIME", "PINK", "GRAY",
				"LIGHT_GRAY", "CYAN", "PURPLE", "BLUE",
				"BROWN", "GREEN", "RED", "BLACK"
		};
		for (int data = 0; data < colors.length; data++) {
			String color = colors[data];
			legacy(color + "_WOOL", "WOOL", data);
			legacy(color + "_STAINED_GLASS", "STAINED_GLASS", data);
			legacy(color + "_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", data);
			legacy(color + "_TERRACOTTA", "STAINED_CLAY", data);
			legacy(color + "_CARPET", "CARPET", data);
			legacySince(color + "_CONCRETE", "CONCRETE", data,
					MaterialTarget.ANY, V1_12);
			legacySince(color + "_CONCRETE_POWDER", "CONCRETE_POWDER", data,
					MaterialTarget.ANY, V1_12);

			int dyeData = 15 - data;
			legacy(color + "_DYE", "INK_SACK", dyeData);
			legacy(color + "_BANNER", "BANNER", dyeData, MaterialTarget.ITEM);

			MinecraftVersion bedMinimum = color.equals("RED") ? V1_8 : V1_12;
			legacySince(color + "_BED", "BED", data, MaterialTarget.ITEM, bedMinimum);
			legacySince(color + "_BED", "BED_BLOCK", data, MaterialTarget.BLOCK, bedMinimum);
		}
	}

	private static void registerWoodFamilies() {
		String[] woods = {"OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA", "DARK_OAK"};
		for (int data = 0; data < woods.length; data++) {
			String wood = woods[data];
			legacy(wood + "_PLANKS", "WOOD", data);
			legacy(wood + "_SAPLING", "SAPLING", data);
			legacy(wood + "_SLAB", "WOOD_STEP", data);
			if (data < 4) {
				legacy(wood + "_LOG", "LOG", data);
				legacy(wood + "_WOOD", "LOG", data + 12);
				legacy(wood + "_LEAVES", "LEAVES", data);
			} else {
				legacy(wood + "_LOG", "LOG_2", data - 4);
				legacy(wood + "_WOOD", "LOG_2", data - 4 + 12);
				legacy(wood + "_LEAVES", "LEAVES_2", data - 4);
			}
		}

		renamed("OAK_STAIRS", "WOOD_STAIRS");
		renamed("SPRUCE_STAIRS", "SPRUCE_WOOD_STAIRS");
		renamed("BIRCH_STAIRS", "BIRCH_WOOD_STAIRS");
		renamed("JUNGLE_STAIRS", "JUNGLE_WOOD_STAIRS");
		renamed("OAK_FENCE", "FENCE");
		renamed("OAK_FENCE_GATE", "FENCE_GATE");
		renamed("OAK_BUTTON", "WOOD_BUTTON");
		renamed("OAK_PRESSURE_PLATE", "WOOD_PLATE");
		renamed("OAK_TRAPDOOR", "TRAP_DOOR");
		renamedSince("SPRUCE_BOAT", "BOAT_SPRUCE", V1_9);
		renamedSince("BIRCH_BOAT", "BOAT_BIRCH", V1_9);
		renamedSince("JUNGLE_BOAT", "BOAT_JUNGLE", V1_9);
		renamedSince("ACACIA_BOAT", "BOAT_ACACIA", V1_9);
		renamedSince("DARK_OAK_BOAT", "BOAT_DARK_OAK", V1_9);
		renamed("OAK_BOAT", "BOAT");
	}

	private static void registerStoneVariants() {
		legacy("GRANITE", "STONE", 1);
		legacy("POLISHED_GRANITE", "STONE", 2);
		legacy("DIORITE", "STONE", 3);
		legacy("POLISHED_DIORITE", "STONE", 4);
		legacy("ANDESITE", "STONE", 5);
		legacy("POLISHED_ANDESITE", "STONE", 6);

		legacy("COARSE_DIRT", "DIRT", 1);
		legacy("PODZOL", "DIRT", 2);
		legacy("RED_SAND", "SAND", 1);
		legacy("CHISELED_SANDSTONE", "SANDSTONE", 1);
		legacy("SMOOTH_SANDSTONE", "SANDSTONE", 2);

		legacy("STONE_BRICKS", "SMOOTH_BRICK", 0);
		legacy("MOSSY_STONE_BRICKS", "SMOOTH_BRICK", 1);
		legacy("CRACKED_STONE_BRICKS", "SMOOTH_BRICK", 2);
		legacy("CHISELED_STONE_BRICKS", "SMOOTH_BRICK", 3);

		legacy("INFESTED_STONE", "MONSTER_EGGS", 0);
		legacy("INFESTED_COBBLESTONE", "MONSTER_EGGS", 1);
		legacy("INFESTED_STONE_BRICKS", "MONSTER_EGGS", 2);
		legacy("INFESTED_MOSSY_STONE_BRICKS", "MONSTER_EGGS", 3);
		legacy("INFESTED_CRACKED_STONE_BRICKS", "MONSTER_EGGS", 4);
		legacy("INFESTED_CHISELED_STONE_BRICKS", "MONSTER_EGGS", 5);

		legacy("PRISMARINE", "PRISMARINE", 0);
		legacy("PRISMARINE_BRICKS", "PRISMARINE", 1);
		legacy("DARK_PRISMARINE", "PRISMARINE", 2);

		legacy("CHISELED_QUARTZ_BLOCK", "QUARTZ_BLOCK", 1);
		legacy("QUARTZ_PILLAR", "QUARTZ_BLOCK", 2);
		legacy("MOSSY_COBBLESTONE_WALL", "COBBLE_WALL", 1);
		legacy("COBBLESTONE_WALL", "COBBLE_WALL", 0);
	}

	private static void registerPlants() {
		renamed("GRASS_BLOCK", "GRASS");
		legacy("SHORT_GRASS", "LONG_GRASS", 1);
		legacy("FERN", "LONG_GRASS", 2);
		renamed("DANDELION", "YELLOW_FLOWER");

		String[] flowers = {
				"POPPY", "BLUE_ORCHID", "ALLIUM", "AZURE_BLUET",
				"RED_TULIP", "ORANGE_TULIP", "WHITE_TULIP", "PINK_TULIP", "OXEYE_DAISY"
		};
		for (int data = 0; data < flowers.length; data++) {
			legacy(flowers[data], "RED_ROSE", data);
		}

		String[] tallPlants = {
				"SUNFLOWER", "LILAC", "TALL_GRASS", "LARGE_FERN", "ROSE_BUSH", "PEONY"
		};
		for (int data = 0; data < tallPlants.length; data++) {
			legacy(tallPlants[data], "DOUBLE_PLANT", data);
		}
		renamed("LILY_PAD", "WATER_LILY");
		renamed("MYCELIUM", "MYCEL");
	}

	private static void registerRenames() {
		String[][] pairs = {
				{"COBWEB", "WEB"},
				{"SPAWNER", "MOB_SPAWNER"},
				{"CRAFTING_TABLE", "WORKBENCH"},
				{"FARMLAND", "SOIL"},
				{"IRON_BARS", "IRON_FENCE"},
				{"GLASS_PANE", "THIN_GLASS"},
				{"TERRACOTTA", "HARD_CLAY"},
				{"ENCHANTING_TABLE", "ENCHANTMENT_TABLE"},
				{"END_STONE", "ENDER_STONE"},
				{"NETHER_QUARTZ_ORE", "QUARTZ_ORE"},
				{"NETHER_BRICK_FENCE", "NETHER_FENCE"},
				{"COMMAND_BLOCK", "COMMAND"},
				{"GUNPOWDER", "SULPHUR"},
				{"CLOCK", "WATCH"},
				{"SNOWBALL", "SNOW_BALL"},
				{"WHEAT_SEEDS", "SEEDS"},
				{"PORKCHOP", "PORK"},
				{"COOKED_PORKCHOP", "GRILLED_PORK"},
				{"BEEF", "RAW_BEEF"},
				{"CHICKEN", "RAW_CHICKEN"},
				{"ENDER_EYE", "EYE_OF_ENDER"},
				{"GLISTERING_MELON_SLICE", "SPECKLED_MELON"},
				{"EXPERIENCE_BOTTLE", "EXP_BOTTLE"},
				{"FIRE_CHARGE", "FIREBALL"},
				{"WRITABLE_BOOK", "BOOK_AND_QUILL"},
				{"FIREWORK_ROCKET", "FIREWORK"},
				{"FIREWORK_STAR", "FIREWORK_CHARGE"},
				{"TNT_MINECART", "EXPLOSIVE_MINECART"},
				{"CHEST_MINECART", "STORAGE_MINECART"},
				{"FURNACE_MINECART", "POWERED_MINECART"},
				{"IRON_HORSE_ARMOR", "IRON_BARDING"},
				{"GOLDEN_HORSE_ARMOR", "GOLD_BARDING"},
				{"DIAMOND_HORSE_ARMOR", "DIAMOND_BARDING"},
				{"LEAD", "LEASH"},
				{"WOODEN_SWORD", "WOOD_SWORD"},
				{"WOODEN_SHOVEL", "WOOD_SPADE"},
				{"WOODEN_PICKAXE", "WOOD_PICKAXE"},
				{"WOODEN_AXE", "WOOD_AXE"},
				{"WOODEN_HOE", "WOOD_HOE"},
				{"STONE_SHOVEL", "STONE_SPADE"},
				{"IRON_SHOVEL", "IRON_SPADE"},
				{"GOLDEN_SWORD", "GOLD_SWORD"},
				{"GOLDEN_SHOVEL", "GOLD_SPADE"},
				{"GOLDEN_PICKAXE", "GOLD_PICKAXE"},
				{"GOLDEN_AXE", "GOLD_AXE"},
				{"GOLDEN_HOE", "GOLD_HOE"},
				{"DIAMOND_SHOVEL", "DIAMOND_SPADE"},
				{"GOLDEN_HELMET", "GOLD_HELMET"},
				{"GOLDEN_CHESTPLATE", "GOLD_CHESTPLATE"},
				{"GOLDEN_LEGGINGS", "GOLD_LEGGINGS"},
				{"GOLDEN_BOOTS", "GOLD_BOOTS"}
		};
		for (String[] pair : pairs) {
			renamed(pair[0], pair[1]);
		}

		ambiguous("MAP", "EMPTY_MAP", MaterialTarget.ITEM);
		ambiguous("FILLED_MAP", "MAP", MaterialTarget.ITEM);
		ambiguous("BRICK", "CLAY_BRICK", MaterialTarget.ITEM);
		ambiguous("BRICKS", "BRICK", MaterialTarget.ANY);
		ambiguous("MELON", "MELON_BLOCK", MaterialTarget.ANY);
		ambiguous("MELON_SLICE", "MELON", MaterialTarget.ITEM);
		ambiguous("NETHER_BRICK", "NETHER_BRICK_ITEM", MaterialTarget.ITEM);
		ambiguous("NETHER_BRICKS", "NETHER_BRICK", MaterialTarget.ANY);
		ambiguous("CARROT", "CARROT_ITEM", MaterialTarget.ITEM);
		ambiguous("CARROTS", "CARROT", MaterialTarget.BLOCK);
		ambiguous("POTATO", "POTATO_ITEM", MaterialTarget.ITEM);
		ambiguous("POTATOES", "POTATO", MaterialTarget.BLOCK);

		renamedSince("TOTEM_OF_UNDYING", "TOTEM", V1_11);
	}

	private static void registerTargetSpecificMaterials() {
		legacy("PLAYER_HEAD", "SKULL_ITEM", 3, MaterialTarget.ITEM);
		legacy("PLAYER_HEAD", "SKULL", 3, MaterialTarget.BLOCK);
		legacy("SKELETON_SKULL", "SKULL_ITEM", 0, MaterialTarget.ITEM);
		legacy("WITHER_SKELETON_SKULL", "SKULL_ITEM", 1, MaterialTarget.ITEM);
		legacy("ZOMBIE_HEAD", "SKULL_ITEM", 2, MaterialTarget.ITEM);
		legacy("CREEPER_HEAD", "SKULL_ITEM", 4, MaterialTarget.ITEM);

		renamed("CAULDRON", "CAULDRON_ITEM", MaterialTarget.ITEM);
		renamed("FLOWER_POT", "FLOWER_POT_ITEM", MaterialTarget.ITEM);
		renamed("REPEATER", "DIODE", MaterialTarget.ITEM);
		renamed("COMPARATOR", "REDSTONE_COMPARATOR", MaterialTarget.ITEM);
		renamed("NETHER_WART", "NETHER_STALK", MaterialTarget.ITEM);
		renamed("NETHER_WART", "NETHER_WARTS", MaterialTarget.BLOCK);

		door("OAK", "WOOD_DOOR", "WOODEN_DOOR");
		door("SPRUCE", "SPRUCE_DOOR_ITEM", "SPRUCE_DOOR");
		door("BIRCH", "BIRCH_DOOR_ITEM", "BIRCH_DOOR");
		door("JUNGLE", "JUNGLE_DOOR_ITEM", "JUNGLE_DOOR");
		door("ACACIA", "ACACIA_DOOR_ITEM", "ACACIA_DOOR");
		door("DARK_OAK", "DARK_OAK_DOOR_ITEM", "DARK_OAK_DOOR");

		renamed("OAK_SIGN", "SIGN", MaterialTarget.ITEM);
		renamed("OAK_SIGN", "SIGN_POST", MaterialTarget.BLOCK);
		renamed("OAK_WALL_SIGN", "WALL_SIGN", MaterialTarget.BLOCK);
	}

	private static void registerLegacy18Aliases() {
		// Old block states that now share one material.
		reverseAlias("WATER", "STATIONARY_WATER");
		reverseAlias("LAVA", "STATIONARY_LAVA");
		renamed("STICKY_PISTON", "PISTON_STICKY_BASE");
		reverseAlias("SHORT_GRASS", "LONG_GRASS");
		renamed("PISTON", "PISTON_BASE");
		renamed("PISTON_HEAD", "PISTON_EXTENSION");
		renamed("MOVING_PISTON", "PISTON_MOVING_PIECE");
		legacy("SMOOTH_STONE", "DOUBLE_STEP", 0);
		legacy("STONE_SLAB", "STEP", 0);
		renamed("WHEAT", "CROPS", MaterialTarget.BLOCK);
		reverseAlias("FURNACE", "BURNING_FURNACE");
		renamed("RAIL", "RAILS");
		renamed("STONE_PRESSURE_PLATE", "STONE_PLATE");
		renamed("IRON_DOOR", "IRON_DOOR_BLOCK", MaterialTarget.BLOCK);
		reverseAlias("REDSTONE_ORE", "GLOWING_REDSTONE_ORE");
		renamed("REDSTONE_TORCH", "REDSTONE_TORCH_ON");
		renamed("REDSTONE_TORCH", "REDSTONE_TORCH_OFF");
		renamed("SUGAR_CANE", "SUGAR_CANE_BLOCK", MaterialTarget.BLOCK);
		renamed("NETHER_PORTAL", "PORTAL");
		renamed("CAKE", "CAKE_BLOCK", MaterialTarget.BLOCK);
		renamed("REPEATER", "DIODE_BLOCK_OFF", MaterialTarget.BLOCK);
		renamed("REPEATER", "DIODE_BLOCK_ON", MaterialTarget.BLOCK);
		renamed("BROWN_MUSHROOM_BLOCK", "HUGE_MUSHROOM_1");
		renamed("RED_MUSHROOM_BLOCK", "HUGE_MUSHROOM_2");
		renamed("STONE_BRICK_STAIRS", "SMOOTH_STAIRS");
		renamed("END_PORTAL", "ENDER_PORTAL");
		renamed("END_PORTAL_FRAME", "ENDER_PORTAL_FRAME");
		renamed("REDSTONE_LAMP", "REDSTONE_LAMP_OFF");
		renamed("REDSTONE_LAMP", "REDSTONE_LAMP_ON");
		legacy("OAK_SLAB", "WOOD_DOUBLE_STEP", 0);
		legacy("SKELETON_SKULL", "SKULL", 0, MaterialTarget.BLOCK);
		renamed("LIGHT_WEIGHTED_PRESSURE_PLATE", "GOLD_PLATE");
		renamed("HEAVY_WEIGHTED_PRESSURE_PLATE", "IRON_PLATE");
		renamed("COMPARATOR", "REDSTONE_COMPARATOR_OFF", MaterialTarget.BLOCK);
		renamed("COMPARATOR", "REDSTONE_COMPARATOR_ON", MaterialTarget.BLOCK);
		renamed("WHITE_BANNER", "STANDING_BANNER", MaterialTarget.BLOCK);
		renamed("WHITE_WALL_BANNER", "WALL_BANNER", MaterialTarget.BLOCK);
		reverseAlias("DAYLIGHT_DETECTOR", "DAYLIGHT_DETECTOR_INVERTED");
		legacy("RED_SANDSTONE_SLAB", "STONE_SLAB2", 0);
		reverseAlias("RED_SANDSTONE_SLAB", "DOUBLE_STONE_SLAB2");

		// Old item names and the pre-flattening music disc constants.
		renamed("MUSHROOM_STEW", "MUSHROOM_SOUP", MaterialTarget.ITEM);
		legacy("COD", "RAW_FISH", 0, MaterialTarget.ITEM);
		legacy("COOKED_COD", "COOKED_FISH", 0, MaterialTarget.ITEM);
		renamed("BREWING_STAND", "BREWING_STAND_ITEM", MaterialTarget.ITEM);
		legacy("CREEPER_SPAWN_EGG", "MONSTER_EGG", 50, MaterialTarget.ITEM);
		legacy("SKELETON_SPAWN_EGG", "MONSTER_EGG", 51, MaterialTarget.ITEM);
		legacy("SPIDER_SPAWN_EGG", "MONSTER_EGG", 52, MaterialTarget.ITEM);
		legacy("ZOMBIE_SPAWN_EGG", "MONSTER_EGG", 54, MaterialTarget.ITEM);
		legacy("SLIME_SPAWN_EGG", "MONSTER_EGG", 55, MaterialTarget.ITEM);
		legacy("GHAST_SPAWN_EGG", "MONSTER_EGG", 56, MaterialTarget.ITEM);
		legacy("ZOMBIFIED_PIGLIN_SPAWN_EGG", "MONSTER_EGG", 57, MaterialTarget.ITEM);
		legacy("ENDERMAN_SPAWN_EGG", "MONSTER_EGG", 58, MaterialTarget.ITEM);
		legacy("CAVE_SPIDER_SPAWN_EGG", "MONSTER_EGG", 59, MaterialTarget.ITEM);
		legacy("SILVERFISH_SPAWN_EGG", "MONSTER_EGG", 60, MaterialTarget.ITEM);
		legacy("BLAZE_SPAWN_EGG", "MONSTER_EGG", 61, MaterialTarget.ITEM);
		legacy("MAGMA_CUBE_SPAWN_EGG", "MONSTER_EGG", 62, MaterialTarget.ITEM);
		legacy("BAT_SPAWN_EGG", "MONSTER_EGG", 65, MaterialTarget.ITEM);
		legacy("WITCH_SPAWN_EGG", "MONSTER_EGG", 66, MaterialTarget.ITEM);
		legacy("ENDERMITE_SPAWN_EGG", "MONSTER_EGG", 67, MaterialTarget.ITEM);
		legacy("GUARDIAN_SPAWN_EGG", "MONSTER_EGG", 68, MaterialTarget.ITEM);
		legacy("PIG_SPAWN_EGG", "MONSTER_EGG", 90, MaterialTarget.ITEM);
		legacy("SHEEP_SPAWN_EGG", "MONSTER_EGG", 91, MaterialTarget.ITEM);
		legacy("COW_SPAWN_EGG", "MONSTER_EGG", 92, MaterialTarget.ITEM);
		legacy("CHICKEN_SPAWN_EGG", "MONSTER_EGG", 93, MaterialTarget.ITEM);
		legacy("SQUID_SPAWN_EGG", "MONSTER_EGG", 94, MaterialTarget.ITEM);
		legacy("WOLF_SPAWN_EGG", "MONSTER_EGG", 95, MaterialTarget.ITEM);
		legacy("MOOSHROOM_SPAWN_EGG", "MONSTER_EGG", 96, MaterialTarget.ITEM);
		legacy("OCELOT_SPAWN_EGG", "MONSTER_EGG", 98, MaterialTarget.ITEM);
		legacy("HORSE_SPAWN_EGG", "MONSTER_EGG", 100, MaterialTarget.ITEM);
		legacy("RABBIT_SPAWN_EGG", "MONSTER_EGG", 101, MaterialTarget.ITEM);
		legacy("VILLAGER_SPAWN_EGG", "MONSTER_EGG", 120, MaterialTarget.ITEM);
		legacySince("ELDER_GUARDIAN_SPAWN_EGG", "MONSTER_EGG", 4, MaterialTarget.ITEM, V1_9);
		legacySince("WITHER_SKELETON_SPAWN_EGG", "MONSTER_EGG", 5, MaterialTarget.ITEM, V1_11);
		legacySince("STRAY_SPAWN_EGG", "MONSTER_EGG", 6, MaterialTarget.ITEM, V1_11);
		legacySince("HUSK_SPAWN_EGG", "MONSTER_EGG", 23, MaterialTarget.ITEM, V1_11);
		legacySince("ZOMBIE_VILLAGER_SPAWN_EGG", "MONSTER_EGG", 27, MaterialTarget.ITEM, V1_11);
		legacySince("SKELETON_HORSE_SPAWN_EGG", "MONSTER_EGG", 28, MaterialTarget.ITEM, V1_11);
		legacySince("ZOMBIE_HORSE_SPAWN_EGG", "MONSTER_EGG", 29, MaterialTarget.ITEM, V1_11);
		legacySince("DONKEY_SPAWN_EGG", "MONSTER_EGG", 31, MaterialTarget.ITEM, V1_11);
		legacySince("MULE_SPAWN_EGG", "MONSTER_EGG", 32, MaterialTarget.ITEM, V1_11);
		legacySince("EVOKER_SPAWN_EGG", "MONSTER_EGG", 34, MaterialTarget.ITEM, V1_11);
		legacySince("VEX_SPAWN_EGG", "MONSTER_EGG", 35, MaterialTarget.ITEM, V1_11);
		legacySince("VINDICATOR_SPAWN_EGG", "MONSTER_EGG", 36, MaterialTarget.ITEM, V1_11);
		legacySince("SHULKER_SPAWN_EGG", "MONSTER_EGG", 69, MaterialTarget.ITEM, V1_9);
		legacySince("POLAR_BEAR_SPAWN_EGG", "MONSTER_EGG", 102, MaterialTarget.ITEM, V1_10);
		legacySince("LLAMA_SPAWN_EGG", "MONSTER_EGG", 103, MaterialTarget.ITEM, V1_11);
		legacySince("PARROT_SPAWN_EGG", "MONSTER_EGG", 105, MaterialTarget.ITEM, V1_12);
		reverseAlias("PIG_SPAWN_EGG", "MONSTER_EGG");
		renamed("CARROT_ON_A_STICK", "CARROT_STICK", MaterialTarget.ITEM);
		renamed("COMMAND_BLOCK_MINECART", "COMMAND_MINECART", MaterialTarget.ITEM);
		renamed("MUSIC_DISC_13", "GOLD_RECORD", MaterialTarget.ITEM);
		renamed("MUSIC_DISC_CAT", "GREEN_RECORD", MaterialTarget.ITEM);
		renamed("MUSIC_DISC_BLOCKS", "RECORD_3", MaterialTarget.ITEM);
		renamed("MUSIC_DISC_CHIRP", "RECORD_4", MaterialTarget.ITEM);
		renamed("MUSIC_DISC_FAR", "RECORD_5", MaterialTarget.ITEM);
		renamed("MUSIC_DISC_MALL", "RECORD_6", MaterialTarget.ITEM);
		renamed("MUSIC_DISC_MELLOHI", "RECORD_7", MaterialTarget.ITEM);
		renamed("MUSIC_DISC_STAL", "RECORD_8", MaterialTarget.ITEM);
		renamed("MUSIC_DISC_STRAD", "RECORD_9", MaterialTarget.ITEM);
		renamed("MUSIC_DISC_WARD", "RECORD_10", MaterialTarget.ITEM);
		renamed("MUSIC_DISC_11", "RECORD_11", MaterialTarget.ITEM);
		renamed("MUSIC_DISC_WAIT", "RECORD_12", MaterialTarget.ITEM);
	}

	private static void door(String wood, String item, String block) {
		renamed(wood + "_DOOR", item, MaterialTarget.ITEM);
		renamed(wood + "_DOOR", block, MaterialTarget.BLOCK);
	}

	private static void renamed(String canonical, String legacy) {
		renamed(canonical, legacy, MaterialTarget.ANY);
	}

	private static void renamed(String canonical, String legacy, MaterialTarget target) {
		legacy(canonical, legacy, -1, target);
	}

	private static void renamedSince(String canonical, String legacy, MinecraftVersion minimum) {
		legacySince(canonical, legacy, -1, MaterialTarget.ANY, minimum);
	}

	private static void ambiguous(String canonical, String legacy, MaterialTarget target) {
		legacy(canonical, legacy, -1, target);
	}

	private static void reverseAlias(String canonical, String legacy) {
		REVERSE.put(new LegacyKey(legacy, -1), canonical);
	}

	private static void legacy(String canonical, String legacy, int data) {
		legacy(canonical, legacy, data, MaterialTarget.ANY);
	}

	private static void legacy(String canonical, String legacy, int data, MaterialTarget target) {
		add(canonical, new Candidate(legacy, data, target, V1_8, V1_12_2));
	}

	private static void legacySince(String canonical, String legacy, int data,
									MaterialTarget target, MinecraftVersion minimum) {
		add(canonical, new Candidate(legacy, data, target, minimum, V1_12_2));
	}

	private static void add(String canonical, Candidate candidate) {
		Descriptor descriptor = DESCRIPTORS.get(canonical);
		if (descriptor == null) {
			descriptor = new Descriptor();
			DESCRIPTORS.put(canonical, descriptor);
		}
		descriptor.candidates.add(candidate);
		LegacyKey key = new LegacyKey(candidate.name, candidate.data);
		String previous = REVERSE.put(key, canonical);
		if (previous != null && !previous.equals(canonical)) {
			throw new IllegalStateException("Conflicting legacy material mapping for "
					+ candidate.name + ":" + candidate.data + " (" + previous + ", " + canonical + ")");
		}
	}

	interface MaterialNameLookup {
		boolean exists(String name);
	}

	static final class MaterialSpec {

		final String name;
		final int data;

		MaterialSpec(String name, int data) {
			this.name = name;
			this.data = data;
		}
	}

	private static final class Descriptor {
		private final List<Candidate> candidates = new ArrayList<>();
	}

	private static final class Candidate {

		private final String name;
		private final int data;
		private final MaterialTarget target;
		private final MinecraftVersion minimum;
		private final MinecraftVersion maximum;

		private Candidate(String name, int data, MaterialTarget target,
						  MinecraftVersion minimum, MinecraftVersion maximum) {
			this.name = name;
			this.data = data;
			this.target = target;
			this.minimum = minimum;
			this.maximum = maximum;
		}

		private boolean supports(MaterialTarget requested, MinecraftVersion version) {
			boolean targetMatches = requested == MaterialTarget.ANY
					|| this.target == MaterialTarget.ANY || this.target == requested;
			return targetMatches && !version.isOlderThan(this.minimum)
					&& version.compareTo(this.maximum) <= 0;
		}
	}

	private static final class LegacyKey {

		private final String name;
		private final int data;

		private LegacyKey(String name, int data) {
			this.name = name;
			this.data = data;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof LegacyKey)) {
				return false;
			}
			LegacyKey other = (LegacyKey) object;
			return this.data == other.data && this.name.equals(other.name);
		}

		@Override
		public int hashCode() {
			return 31 * this.name.hashCode() + this.data;
		}
	}
}

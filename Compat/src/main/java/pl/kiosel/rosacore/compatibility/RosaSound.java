package pl.kiosel.rosacore.compatibility;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;

public final class RosaSound {

	private static final NameAliases ALIASES = new NameAliases()
			.add("AMBIENT_CAVE", "AMBIENCE_CAVE")
			.add("WEATHER_RAIN", "AMBIENCE_RAIN")
			.add("ENTITY_LIGHTNING_BOLT_THUNDER", "AMBIENCE_THUNDER")
			.add("BLOCK_ANVIL_BREAK", "ANVIL_BREAK")
			.add("BLOCK_ANVIL_LAND", "ANVIL_LAND")
			.add("BLOCK_ANVIL_USE", "ANVIL_USE")
			.add("ENTITY_ARROW_HIT", "ARROW_HIT")
			.add("ENTITY_PLAYER_BURP", "BURP")
			.add("BLOCK_CHEST_CLOSE", "CHEST_CLOSE")
			.add("BLOCK_CHEST_OPEN", "CHEST_OPEN")
			.add("UI_BUTTON_CLICK", "CLICK")
			.add("BLOCK_WOODEN_DOOR_CLOSE", "DOOR_CLOSE")
			.add("BLOCK_WOODEN_DOOR_OPEN", "DOOR_OPEN")
			.add("ENTITY_GENERIC_DRINK", "DRINK")
			.add("ENTITY_GENERIC_EAT", "EAT")
			.add("ENTITY_GENERIC_EXPLODE", "EXPLODE")
			.add("ENTITY_GENERIC_BIG_FALL", "FALL_BIG")
			.add("ENTITY_GENERIC_SMALL_FALL", "FALL_SMALL")
			.add("BLOCK_FIRE_AMBIENT", "FIRE")
			.add("ITEM_FLINTANDSTEEL_USE", "FIRE_IGNITE")
			.add("BLOCK_FIRE_EXTINGUISH", "FIZZ")
			.add("ENTITY_TNT_PRIMED", "FUSE")
			.add("BLOCK_GLASS_BREAK", "GLASS")
			.add("ENTITY_GENERIC_HURT", "HURT_FLESH")
			.add("ENTITY_ITEM_BREAK", "ITEM_BREAK")
			.add("ENTITY_ITEM_PICKUP", "ITEM_PICKUP")
			.add("BLOCK_LAVA_AMBIENT", "LAVA")
			.add("BLOCK_LAVA_POP", "LAVA_POP")
			.add("ENTITY_PLAYER_LEVELUP", "LEVEL_UP")
			.add("ENTITY_MINECART_RIDING", "MINECART_BASE")
			.add("ENTITY_MINECART_INSIDE", "MINECART_INSIDE")
			.add("ENTITY_EXPERIENCE_ORB_PICKUP", "ORB_PICKUP")
			.add("BLOCK_PISTON_EXTEND", "PISTON_EXTEND")
			.add("BLOCK_PISTON_CONTRACT", "PISTON_RETRACT")
			.add("BLOCK_PORTAL_AMBIENT", "PORTAL")
			.add("BLOCK_PORTAL_TRAVEL", "PORTAL_TRAVEL")
			.add("BLOCK_PORTAL_TRIGGER", "PORTAL_TRIGGER")
			.add("ENTITY_ARROW_SHOOT", "SHOOT_ARROW")
			.add("ENTITY_GENERIC_SPLASH", "SPLASH")
			.add("ENTITY_PLAYER_SPLASH_HIGH_SPEED", "SPLASH2")
			.add("ENTITY_GENERIC_SWIM", "SWIM")
			.add("BLOCK_WATER_AMBIENT", "WATER")
			.add("BLOCK_GRASS_STEP", "STEP_GRASS")
			.add("BLOCK_GRAVEL_STEP", "STEP_GRAVEL")
			.add("BLOCK_LADDER_STEP", "STEP_LADDER")
			.add("BLOCK_SAND_STEP", "STEP_SAND")
			.add("BLOCK_SNOW_STEP", "STEP_SNOW")
			.add("BLOCK_STONE_STEP", "STEP_STONE")
			.add("BLOCK_WOOD_STEP", "STEP_WOOD")
			.add("BLOCK_WOOL_STEP", "STEP_WOOL")
			.add("BLOCK_WOODEN_BUTTON_CLICK_ON", "WOOD_CLICK")
			.add("BLOCK_NOTE_BLOCK_BASS", "NOTE_BASS")
			.add("BLOCK_NOTE_BLOCK_HARP", "NOTE_PIANO")
			.add("BLOCK_NOTE_BLOCK_BASEDRUM", "NOTE_BASS_DRUM")
			.add("BLOCK_NOTE_BLOCK_HAT", "NOTE_STICKS")
			.add("BLOCK_NOTE_BLOCK_GUITAR", "NOTE_BASS_GUITAR")
			.add("BLOCK_NOTE_BLOCK_SNARE", "NOTE_SNARE_DRUM")
			.add("BLOCK_NOTE_BLOCK_PLING", "NOTE_PLING")
			.add("ENTITY_BAT_DEATH", "BAT_DEATH")
			.add("ENTITY_BAT_HURT", "BAT_HURT")
			.add("ENTITY_BAT_AMBIENT", "BAT_IDLE")
			.add("ENTITY_BAT_LOOP", "BAT_LOOP")
			.add("ENTITY_BAT_TAKEOFF", "BAT_TAKEOFF")
			.add("ENTITY_BLAZE_AMBIENT", "BLAZE_BREATH")
			.add("ENTITY_BLAZE_DEATH", "BLAZE_DEATH")
			.add("ENTITY_BLAZE_HURT", "BLAZE_HIT")
			.add("ENTITY_CAT_HISS", "CAT_HISS")
			.add("ENTITY_CAT_HURT", "CAT_HIT")
			.add("ENTITY_CAT_AMBIENT", "CAT_MEOW")
			.add("ENTITY_CAT_PURR", "CAT_PURR")
			.add("ENTITY_CAT_PURREOW", "CAT_PURREOW")
			.add("ENTITY_CHICKEN_AMBIENT", "CHICKEN_IDLE")
			.add("ENTITY_CHICKEN_HURT", "CHICKEN_HURT")
			.add("ENTITY_CHICKEN_EGG", "CHICKEN_EGG_POP")
			.add("ENTITY_CHICKEN_STEP", "CHICKEN_WALK")
			.add("ENTITY_COW_AMBIENT", "COW_IDLE")
			.add("ENTITY_COW_HURT", "COW_HURT")
			.add("ENTITY_COW_STEP", "COW_WALK")
			.add("ENTITY_CREEPER_PRIMED", "CREEPER_HISS")
			.add("ENTITY_CREEPER_DEATH", "CREEPER_DEATH")
			.add("ENTITY_ENDER_DRAGON_DEATH", "ENDERDRAGON_DEATH")
			.add("ENTITY_ENDER_DRAGON_GROWL", "ENDERDRAGON_GROWL")
			.add("ENTITY_ENDER_DRAGON_HURT", "ENDERDRAGON_HIT")
			.add("ENTITY_ENDER_DRAGON_FLAP", "ENDERDRAGON_WINGS")
			.add("ENTITY_ENDERMAN_DEATH", "ENDERMAN_DEATH")
			.add("ENTITY_ENDERMAN_HURT", "ENDERMAN_HIT")
			.add("ENTITY_ENDERMAN_AMBIENT", "ENDERMAN_IDLE")
			.add("ENTITY_ENDERMAN_TELEPORT", "ENDERMAN_TELEPORT")
			.add("ENTITY_ENDERMAN_SCREAM", "ENDERMAN_SCREAM")
			.add("ENTITY_ENDERMAN_STARE", "ENDERMAN_STARE")
			.add("ENTITY_GHAST_SCREAM", "GHAST_SCREAM")
			.add("ENTITY_GHAST_SCREAM", "GHAST_SCREAM2")
			.add("ENTITY_GHAST_WARN", "GHAST_CHARGE")
			.add("ENTITY_GHAST_DEATH", "GHAST_DEATH")
			.add("ENTITY_GHAST_SHOOT", "GHAST_FIREBALL")
			.add("ENTITY_GHAST_AMBIENT", "GHAST_MOAN")
			.add("ENTITY_IRON_GOLEM_DEATH", "IRONGOLEM_DEATH")
			.add("ENTITY_IRON_GOLEM_HURT", "IRONGOLEM_HIT")
			.add("ENTITY_IRON_GOLEM_ATTACK", "IRONGOLEM_THROW")
			.add("ENTITY_IRON_GOLEM_STEP", "IRONGOLEM_WALK")
			.add("ENTITY_MAGMA_CUBE_SQUISH", "MAGMACUBE_WALK")
			.add("ENTITY_MAGMA_CUBE_SQUISH_SMALL", "MAGMACUBE_WALK2")
			.add("ENTITY_MAGMA_CUBE_JUMP", "MAGMACUBE_JUMP")
			.add("ENTITY_PIG_AMBIENT", "PIG_IDLE")
			.add("ENTITY_PIG_DEATH", "PIG_DEATH")
			.add("ENTITY_PIG_STEP", "PIG_WALK")
			.add("ENTITY_SHEEP_AMBIENT", "SHEEP_IDLE")
			.add("ENTITY_SHEEP_SHEAR", "SHEEP_SHEAR")
			.add("ENTITY_SHEEP_STEP", "SHEEP_WALK")
			.add("ENTITY_SKELETON_AMBIENT", "SKELETON_IDLE")
			.add("ENTITY_SKELETON_DEATH", "SKELETON_DEATH")
			.add("ENTITY_SKELETON_HURT", "SKELETON_HURT")
			.add("ENTITY_SKELETON_STEP", "SKELETON_WALK")
			.add("ENTITY_SPIDER_AMBIENT", "SPIDER_IDLE")
			.add("ENTITY_SPIDER_DEATH", "SPIDER_DEATH")
			.add("ENTITY_SPIDER_STEP", "SPIDER_WALK")
			.add("ENTITY_SILVERFISH_HURT", "SILVERFISH_HIT")
			.add("ENTITY_SILVERFISH_DEATH", "SILVERFISH_KILL")
			.add("ENTITY_SILVERFISH_AMBIENT", "SILVERFISH_IDLE")
			.add("ENTITY_SILVERFISH_STEP", "SILVERFISH_WALK")
			.add("ENTITY_SLIME_ATTACK", "SLIME_ATTACK")
			.add("ENTITY_SLIME_SQUISH", "SLIME_WALK")
			.add("ENTITY_SLIME_SQUISH_SMALL", "SLIME_WALK2")
			.add("ENTITY_WITHER_DEATH", "WITHER_DEATH")
			.add("ENTITY_WITHER_HURT", "WITHER_HURT")
			.add("ENTITY_WITHER_AMBIENT", "WITHER_IDLE")
			.add("ENTITY_WITHER_SHOOT", "WITHER_SHOOT")
			.add("ENTITY_WITHER_SPAWN", "WITHER_SPAWN")
			.add("ENTITY_VILLAGER_DEATH", "VILLAGER_DEATH")
			.add("ENTITY_VILLAGER_TRADE", "VILLAGER_HAGGLE")
			.add("ENTITY_VILLAGER_HURT", "VILLAGER_HIT")
			.add("ENTITY_VILLAGER_AMBIENT", "VILLAGER_IDLE")
			.add("ENTITY_VILLAGER_NO", "VILLAGER_NO")
			.add("ENTITY_VILLAGER_YES", "VILLAGER_YES")
			.add("ENTITY_WOLF_AMBIENT", "WOLF_BARK")
			.add("ENTITY_WOLF_DEATH", "WOLF_DEATH")
			.add("ENTITY_WOLF_GROWL", "WOLF_GROWL")
			.add("ENTITY_WOLF_AMBIENT", "WOLF_HOWL")
			.add("ENTITY_WOLF_HURT", "WOLF_HURT")
			.add("ENTITY_WOLF_PANT", "WOLF_PANT")
			.add("ENTITY_WOLF_SHAKE", "WOLF_SHAKE")
			.add("ENTITY_WOLF_STEP", "WOLF_WALK")
			.add("ENTITY_WOLF_WHINE", "WOLF_WHINE")
			.add("ENTITY_ZOMBIE_ATTACK_IRON_DOOR", "ZOMBIE_METAL")
			.add("ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR", "ZOMBIE_WOOD")
			.add("ENTITY_ZOMBIE_BREAK_WOODEN_DOOR", "ZOMBIE_WOODBREAK")
			.add("ENTITY_ZOMBIE_AMBIENT", "ZOMBIE_IDLE")
			.add("ENTITY_ZOMBIE_DEATH", "ZOMBIE_DEATH")
			.add("ENTITY_ZOMBIE_HURT", "ZOMBIE_HURT")
			.add("ENTITY_ZOMBIE_INFECT", "ZOMBIE_INFECT")
			.add("ENTITY_ZOMBIE_VILLAGER_CURE", "ZOMBIE_UNFECT")
			.add("ENTITY_ZOMBIE_VILLAGER_CURE", "ZOMBIE_REMEDY")
			.add("ENTITY_ZOMBIE_STEP", "ZOMBIE_WALK")
			.add("ENTITY_ZOMBIFIED_PIGLIN_AMBIENT", "ZOMBIE_PIG_IDLE")
			.add("ENTITY_ZOMBIFIED_PIGLIN_ANGRY", "ZOMBIE_PIG_ANGRY")
			.add("ENTITY_ZOMBIFIED_PIGLIN_DEATH", "ZOMBIE_PIG_DEATH")
			.add("ENTITY_ZOMBIFIED_PIGLIN_HURT", "ZOMBIE_PIG_HURT")
			.add("BLOCK_WOOL_BREAK", "DIG_WOOL")
			.add("BLOCK_GRASS_BREAK", "DIG_GRASS")
			.add("BLOCK_GRAVEL_BREAK", "DIG_GRAVEL")
			.add("BLOCK_SAND_BREAK", "DIG_SAND")
			.add("BLOCK_SNOW_BREAK", "DIG_SNOW")
			.add("BLOCK_STONE_BREAK", "DIG_STONE")
			.add("BLOCK_WOOD_BREAK", "DIG_WOOD")
			.add("ENTITY_FIREWORK_ROCKET_BLAST", "FIREWORK_BLAST")
			.add("ENTITY_FIREWORK_ROCKET_BLAST_FAR", "FIREWORK_BLAST2")
			.add("ENTITY_FIREWORK_ROCKET_LARGE_BLAST", "FIREWORK_LARGE_BLAST")
			.add("ENTITY_FIREWORK_ROCKET_LARGE_BLAST_FAR", "FIREWORK_LARGE_BLAST2")
			.add("ENTITY_FIREWORK_ROCKET_TWINKLE", "FIREWORK_TWINKLE")
			.add("ENTITY_FIREWORK_ROCKET_TWINKLE_FAR", "FIREWORK_TWINKLE2")
			.add("ENTITY_FIREWORK_ROCKET_LAUNCH", "FIREWORK_LAUNCH")
			.add("ENTITY_ARROW_HIT_PLAYER", "SUCCESSFUL_HIT")
			.add("ENTITY_HORSE_ANGRY", "HORSE_ANGRY")
			.add("ENTITY_HORSE_ARMOR", "HORSE_ARMOR")
			.add("ENTITY_HORSE_BREATHE", "HORSE_BREATHE")
			.add("ENTITY_HORSE_DEATH", "HORSE_DEATH")
			.add("ENTITY_HORSE_GALLOP", "HORSE_GALLOP")
			.add("ENTITY_HORSE_HURT", "HORSE_HIT")
			.add("ENTITY_HORSE_AMBIENT", "HORSE_IDLE")
			.add("ENTITY_HORSE_JUMP", "HORSE_JUMP")
			.add("ENTITY_HORSE_LAND", "HORSE_LAND")
			.add("ENTITY_HORSE_SADDLE", "HORSE_SADDLE")
			.add("ENTITY_HORSE_STEP", "HORSE_SOFT")
			.add("ENTITY_HORSE_STEP_WOOD", "HORSE_WOOD")
			.add("ENTITY_DONKEY_ANGRY", "DONKEY_ANGRY")
			.add("ENTITY_DONKEY_DEATH", "DONKEY_DEATH")
			.add("ENTITY_DONKEY_HURT", "DONKEY_HIT")
			.add("ENTITY_DONKEY_AMBIENT", "DONKEY_IDLE")
			.add("ENTITY_SKELETON_HORSE_DEATH", "HORSE_SKELETON_DEATH")
			.add("ENTITY_SKELETON_HORSE_HURT", "HORSE_SKELETON_HIT")
			.add("ENTITY_SKELETON_HORSE_AMBIENT", "HORSE_SKELETON_IDLE")
			.add("ENTITY_ZOMBIE_HORSE_DEATH", "HORSE_ZOMBIE_DEATH")
			.add("ENTITY_ZOMBIE_HORSE_HURT", "HORSE_ZOMBIE_HIT")
			.add("ENTITY_ZOMBIE_HORSE_AMBIENT", "HORSE_ZOMBIE_IDLE");

	private final String name;

	private RosaSound(String name) {
		this.name = name;
	}

	public static RosaSound of(String name) {
		return new RosaSound(ALIASES.canonical(name));
	}

	public static Optional<RosaSound> parse(String name) {
		try {
			return Optional.of(of(name));
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}

	public static RosaSound from(Sound sound) {
		Objects.requireNonNull(sound, "sound");
		return of(NameAliases.runtimeName(Sound.class, sound));
	}

	public String getName() {
		return this.name;
	}

	public Optional<Sound> resolve() {
		Sound direct = valueOf(this.name);
		if (direct != null) {
			return Optional.of(direct);
		}
		String legacy = ALIASES.legacy(this.name);
		return Optional.ofNullable(legacy == null ? null : valueOf(legacy));
	}

	public boolean isSupported() {
		return this.resolve().isPresent();
	}

	public boolean play(Player player, float volume, float pitch) {
		Objects.requireNonNull(player, "player");
		return this.play(player, player.getLocation(), volume, pitch);
	}

	public boolean play(Player player, Location location, float volume, float pitch) {
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(location, "location");
		Optional<Sound> sound = this.resolve();
		if (!sound.isPresent()) {
			return false;
		}
		player.playSound(location, sound.get(), volume, pitch);
		return true;
	}

	public boolean play(Location location, float volume, float pitch) {
		Objects.requireNonNull(location, "location");
		World world = location.getWorld();
		Optional<Sound> sound = this.resolve();
		if (world == null || !sound.isPresent()) {
			return false;
		}
		world.playSound(location, sound.get(), volume, pitch);
		return true;
	}

	private static Sound valueOf(String name) {
		return NameAliases.staticValue(Sound.class, name);
	}

	public static final class SoundHolder {
		private final ZSound sound;
		private final float volume;
		private final float pitch;

		public SoundHolder(ZSound sound, float volume, float pitch) {
			this.sound = Objects.requireNonNull(sound, "sound");
			this.volume = normalize(volume);
			this.pitch = normalize(pitch);
		}

		public ZSound getSound() {
			return this.sound;
		}

		public float getVolume() {
			return this.volume;
		}

		public float getPitch() {
			return this.pitch;
		}

		public void play(Player player) {
			this.sound.play(player.getLocation(), this.volume, this.pitch);
		}

		public void play(Location location) {
			this.sound.play(location, this.volume, this.pitch);
		}

		private float normalize(float value) {
			if (Float.isNaN(value)) {
				throw new IllegalArgumentException("Sound volume and pitch cannot be NaN");
			}
			return Math.max(0.0f, Math.min(2.0f, value));
		}
	}

	@Override
	public String toString() {
		return this.name;
	}
}

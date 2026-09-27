package pl.kiosel.rosacore.hologram;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import pl.kiosel.rosacore.RosaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

final class HologramEntityFactory {

	private static final double ARMOR_STAND_TEXT_OFFSET = -1.25D;
	private static final int LEGACY_TEXT_LIMIT = 256;
	private static final String METADATA_KEY = "rosacore_hologram";

	private final RosaPlugin plugin;
	private final TextDisplayBridge textDisplays;

	HologramEntityFactory(RosaPlugin plugin) {
		this.plugin = plugin;
		ClassLoader serverLoader = plugin.getServer().getClass().getClassLoader();
		this.textDisplays = new TextDisplayBridge(serverLoader);
	}

	boolean supportsTextDisplays() {
		return this.textDisplays.isSupported();
	}

	Entity spawn(UUID hologramId, Location textLocation, String text,
				 boolean shadowed, boolean seeThrough, boolean defaultBackground) {
		Entity entity;
		if (this.textDisplays.isSupported()) {
			entity = this.textDisplays.spawn(textLocation, text, shadowed, seeThrough, defaultBackground);
		} else {
			entity = spawnArmorStand(textLocation, text);
		}
		try {
			configureCommon(entity, hologramId);
			return entity;
		} catch (RuntimeException | Error exception) {
			entity.remove();
			throw exception;
		}
	}

	void update(Entity entity, Location textLocation, String text,
				boolean shadowed, boolean seeThrough, boolean defaultBackground) {
		if (this.textDisplays.isTextDisplay(entity)) {
			if (!entity.getLocation().equals(textLocation)) entity.teleport(textLocation);
			this.textDisplays.apply(entity, text, shadowed, seeThrough, defaultBackground);
			return;
		}
		if (!(entity instanceof ArmorStand)) {
			throw new IllegalArgumentException("Unsupported hologram line entity: " + entity.getType());
		}
		Location entityLocation = armorStandLocation(textLocation);
		if (!entity.getLocation().equals(entityLocation)) entity.teleport(entityLocation);
		ArmorStand armorStand = (ArmorStand) entity;
		armorStand.setCustomName(HologramText.truncate(text, LEGACY_TEXT_LIMIT));
		armorStand.setCustomNameVisible(true);
	}

	boolean isUsable(Entity entity) {
		return entity != null && entity.isValid() && !entity.isDead();
	}

	void remove(Entity entity) {
		if (entity != null && !entity.isDead()) entity.remove();
	}

	private ArmorStand spawnArmorStand(Location textLocation, String text) {
		ArmorStand armorStand = textLocation.getWorld().spawn(armorStandLocation(textLocation), ArmorStand.class);
		try {
			armorStand.setVisible(false);
			armorStand.setGravity(false);
			armorStand.setMarker(true);
			armorStand.setSmall(true);
			armorStand.setBasePlate(false);
			armorStand.setArms(false);
			armorStand.setCanPickupItems(false);
			armorStand.setRemoveWhenFarAway(false);
			armorStand.setCustomName(HologramText.truncate(text, LEGACY_TEXT_LIMIT));
			armorStand.setCustomNameVisible(true);
			return armorStand;
		} catch (RuntimeException | Error exception) {
			armorStand.remove();
			throw exception;
		}
	}

	private void configureCommon(Entity entity, UUID hologramId) {
		entity.setMetadata(METADATA_KEY, new FixedMetadataValue(this.plugin, hologramId.toString()));
		invokeOptional(entity, "setPersistent", new Class<?>[]{boolean.class}, false);
		invokeOptional(entity, "setInvulnerable", new Class<?>[]{boolean.class}, true);
		invokeOptional(entity, "setSilent", new Class<?>[]{boolean.class}, true);
		invokeOptional(entity, "setCollidable", new Class<?>[]{boolean.class}, false);
		invokeOptional(entity, "addScoreboardTag", new Class<?>[]{String.class},
				"rosa_hologram_" + hologramId.toString());
	}

	private static Location armorStandLocation(Location textLocation) {
		return textLocation.clone().add(0.0D, ARMOR_STAND_TEXT_OFFSET, 0.0D);
	}

	private static void invokeOptional(Object target, String name, Class<?>[] types, Object... arguments) {
		try {
			Method method = target.getClass().getMethod(name, types);
			method.invoke(target, arguments);
		} catch (NoSuchMethodException ignored) {
		} catch (IllegalAccessException exception) {
			throw new IllegalStateException("Cannot access entity method " + name, exception);
		} catch (InvocationTargetException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof RuntimeException) throw (RuntimeException) cause;
			if (cause instanceof Error) throw (Error) cause;
			throw new IllegalStateException("Entity method failed: " + name, cause);
		}
	}
}

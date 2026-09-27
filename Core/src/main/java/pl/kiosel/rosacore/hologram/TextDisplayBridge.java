package pl.kiosel.rosacore.hologram;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class TextDisplayBridge {

	private static final String TEXT_DISPLAY_CLASS = "org.bukkit.entity.TextDisplay";
	private static final String BILLBOARD_CLASS = "org.bukkit.entity.Display$Billboard";

	private final Class<?> textDisplayClass;
	private final EntityType textDisplayType;
	private final Object centerBillboard;
	private final Method setText;
	private final Method setBillboard;
	private final Method setShadowed;
	private final Method setSeeThrough;
	private final Method setDefaultBackground;
	private final Method setLineWidth;

	TextDisplayBridge(ClassLoader classLoader) {
		Class<?> foundTextDisplay = null;
		EntityType foundType = null;
		Object foundCenter = null;
		Method foundSetText = null;
		Method foundSetBillboard = null;
		Method foundSetShadowed = null;
		Method foundSetSeeThrough = null;
		Method foundSetDefaultBackground = null;
		Method foundSetLineWidth = null;
		try {
			foundTextDisplay = Class.forName(TEXT_DISPLAY_CLASS, false, classLoader);
			Class<?> billboardClass = Class.forName(BILLBOARD_CLASS, false, classLoader);
			foundType = EntityType.valueOf("TEXT_DISPLAY");
			foundCenter = enumValue(billboardClass, "CENTER");
			foundSetText = foundTextDisplay.getMethod("setText", String.class);
			foundSetBillboard = foundTextDisplay.getMethod("setBillboard", billboardClass);
			foundSetShadowed = foundTextDisplay.getMethod("setShadowed", boolean.class);
			foundSetSeeThrough = foundTextDisplay.getMethod("setSeeThrough", boolean.class);
			foundSetDefaultBackground = foundTextDisplay.getMethod("setDefaultBackground", boolean.class);
			foundSetLineWidth = foundTextDisplay.getMethod("setLineWidth", int.class);
		} catch (ReflectiveOperationException | IllegalArgumentException | LinkageError ignored) {
			foundTextDisplay = null;
			foundType = null;
			foundCenter = null;
			foundSetText = null;
			foundSetBillboard = null;
			foundSetShadowed = null;
			foundSetSeeThrough = null;
			foundSetDefaultBackground = null;
			foundSetLineWidth = null;
		}
		this.textDisplayClass = foundTextDisplay;
		this.textDisplayType = foundType;
		this.centerBillboard = foundCenter;
		this.setText = foundSetText;
		this.setBillboard = foundSetBillboard;
		this.setShadowed = foundSetShadowed;
		this.setSeeThrough = foundSetSeeThrough;
		this.setDefaultBackground = foundSetDefaultBackground;
		this.setLineWidth = foundSetLineWidth;
	}

	boolean isSupported() {
		return this.textDisplayClass != null;
	}

	boolean isTextDisplay(Entity entity) {
		return isSupported() && this.textDisplayClass.isInstance(entity);
	}

	Entity spawn(Location location, String text, boolean shadowed,
				 boolean seeThrough, boolean defaultBackground) {
		if (!isSupported()) throw new IllegalStateException("Text displays are not supported");
		Entity entity = location.getWorld().spawnEntity(location, this.textDisplayType);
		if (!isTextDisplay(entity)) {
			entity.remove();
			throw new IllegalStateException("Server spawned an incompatible TEXT_DISPLAY entity");
		}
		try {
			apply(entity, text, shadowed, seeThrough, defaultBackground);
			return entity;
		} catch (RuntimeException | Error exception) {
			entity.remove();
			throw exception;
		}
	}

	void apply(Entity entity, String text, boolean shadowed,
			   boolean seeThrough, boolean defaultBackground) {
		if (!isTextDisplay(entity)) throw new IllegalArgumentException("Entity is not a text display");
		invoke(this.setText, entity, text);
		invoke(this.setBillboard, entity, this.centerBillboard);
		invoke(this.setShadowed, entity, shadowed);
		invoke(this.setSeeThrough, entity, seeThrough);
		invoke(this.setDefaultBackground, entity, defaultBackground);
		invoke(this.setLineWidth, entity, 2048);
	}

	private static void invoke(Method method, Object target, Object... arguments) {
		try {
			method.invoke(target, arguments);
		} catch (IllegalAccessException exception) {
			throw new IllegalStateException("Cannot access TextDisplay method " + method.getName(), exception);
		} catch (InvocationTargetException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof RuntimeException) throw (RuntimeException) cause;
			if (cause instanceof Error) throw (Error) cause;
			throw new IllegalStateException("TextDisplay method failed: " + method.getName(), cause);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Object enumValue(Class<?> enumClass, String name) {
		return Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), name);
	}
}

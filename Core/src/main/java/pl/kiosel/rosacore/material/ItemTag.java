package pl.kiosel.rosacore.material;

import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.NBTType;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteItemNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableItemNBT;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ItemTag {

	private ItemTag() {
	}

	public static ItemStack set(ItemStack item, String key, String value) {
		Objects.requireNonNull(value, "value");
		return modify(item, key, nbt -> nbt.setString(key, value));
	}

	public static ItemStack set(ItemStack item, String key, int value) {
		return modify(item, key, nbt -> nbt.setInteger(key, value));
	}

	public static ItemStack set(ItemStack item, String key, long value) {
		return modify(item, key, nbt -> nbt.setLong(key, value));
	}

	public static ItemStack set(ItemStack item, String key, double value) {
		return modify(item, key, nbt -> nbt.setDouble(key, value));
	}

	public static ItemStack set(ItemStack item, String key, boolean value) {
		return modify(item, key, nbt -> nbt.setBoolean(key, value));
	}

	public static ItemStack set(ItemStack item, String key, UUID value) {
		Objects.requireNonNull(value, "value");
		return modify(item, key, nbt -> nbt.setUUID(key, value));
	}

	public static String getString(ItemStack item, String key) {
		return read(item, key, nbt -> hasType(nbt, key, NBTType.NBTTagString) ? nbt.getString(key) : null);
	}

	public static String getString(ItemStack item, String key, String defaultValue) {
		String value = getString(item, key);
		return value == null ? defaultValue : value;
	}

	public static Integer getInt(ItemStack item, String key) {
		return read(item, key, nbt -> hasType(nbt, key, NBTType.NBTTagInt) ? nbt.getInteger(key) : null);
	}

	public static int getInt(ItemStack item, String key, int defaultValue) {
		Integer value = getInt(item, key);
		return value == null ? defaultValue : value;
	}

	public static Long getLong(ItemStack item, String key) {
		return read(item, key, nbt -> hasType(nbt, key, NBTType.NBTTagLong) ? nbt.getLong(key) : null);
	}

	public static long getLong(ItemStack item, String key, long defaultValue) {
		Long value = getLong(item, key);
		return value == null ? defaultValue : value;
	}

	public static Double getDouble(ItemStack item, String key) {
		return read(item, key, nbt -> hasType(nbt, key, NBTType.NBTTagDouble) ? nbt.getDouble(key) : null);
	}

	public static double getDouble(ItemStack item, String key, double defaultValue) {
		Double value = getDouble(item, key);
		return value == null ? defaultValue : value;
	}

	public static Boolean getBoolean(ItemStack item, String key) {
		return read(item, key, nbt -> hasType(nbt, key, NBTType.NBTTagByte) ? nbt.getBoolean(key) : null);
	}

	public static boolean getBoolean(ItemStack item, String key, boolean defaultValue) {
		Boolean value = getBoolean(item, key);
		return value == null ? defaultValue : value;
	}

	public static UUID getUUID(ItemStack item, String key) {
		return read(item, key, nbt -> nbt.hasTag(key) ? nbt.getUUID(key) : null);
	}

	public static UUID getUUID(ItemStack item, String key, UUID defaultValue) {
		UUID value = getUUID(item, key);
		return value == null ? defaultValue : value;
	}

	public static boolean has(ItemStack item, String key) {
		return read(item, key, nbt -> nbt.hasTag(key));
	}

	public static Set<String> keys(ItemStack item) {
		requireItem(item);
		Set<String> keys = NBT.get(item,
				(Function<ReadableItemNBT, Set<String>>) nbt -> new LinkedHashSet<>(nbt.getKeys()));
		return Collections.unmodifiableSet(keys);
	}

	public static ItemStack remove(ItemStack item, String key) {
		return modify(item, key, nbt -> nbt.removeKey(key));
	}

	private static ItemStack modify(ItemStack item, String key, Consumer<ReadWriteItemNBT> action) {
		requireItem(item);
		requireKey(key);
		NBT.modify(item, action);
		return item;
	}

	private static <T> T read(ItemStack item, String key, Function<ReadableItemNBT, T> reader) {
		requireItem(item);
		requireKey(key);
		return NBT.get(item, reader);
	}

	private static boolean hasType(ReadableItemNBT nbt, String key, NBTType type) {
		return nbt.hasTag(key) && nbt.getType(key) == type;
	}

	private static void requireItem(ItemStack item) {
		Objects.requireNonNull(item, "item");
		if (item.getType() == Material.AIR) {
			throw new IllegalArgumentException("AIR cannot contain persistent item tags");
		}
	}

	private static void requireKey(String key) {
		Objects.requireNonNull(key, "key");
		if (key.trim().isEmpty()) {
			throw new IllegalArgumentException("Tag key cannot be empty");
		}
	}
}

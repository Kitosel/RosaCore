package pl.kiosel.rosacore.material;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.config.ConfigView;
import pl.kiosel.rosacore.config.MutableConfig;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public final class ItemSerializer {

	private static final String ITEM_PREFIX = "rosa-item-v1:";
	private static final String ARRAY_PREFIX = "rosa-items-v1:";
	private static final String ITEM_PATH = "item";
	private static final String ITEMS_PATH = "items";
	private static final int MAX_ENCODED_LENGTH = 16 * 1024 * 1024;
	private static final int MAX_ARRAY_SIZE = 10_000;

	private ItemSerializer() {
	}

	public static String serialize(ItemStack item) {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set(ITEM_PATH, Objects.requireNonNull(item, "item").clone());
		return encode(ITEM_PREFIX, yaml.saveToString());
	}

	public static ItemStack deserialize(String serialized) {
		YamlConfiguration yaml = load(decode(ITEM_PREFIX, serialized));
		ItemStack item = yaml.getItemStack(ITEM_PATH);
		if (item == null) {
			throw new IllegalArgumentException("Serialized value does not contain an item");
		}
		return item.clone();
	}

	public static Optional<ItemStack> tryDeserialize(String serialized) {
		if (serialized == null || serialized.trim().isEmpty()) {
			return Optional.empty();
		}
		try {
			return Optional.of(deserialize(serialized));
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}

	public static String serializeArray(ItemStack[] items) {
		Objects.requireNonNull(items, "items");
		if (items.length > MAX_ARRAY_SIZE) {
			throw new IllegalArgumentException("Too many items to serialize: " + items.length);
		}
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("size", items.length);
		for (int index = 0; index < items.length; index++) {
			ItemStack item = items[index];
			if (item != null) yaml.set(ITEMS_PATH + "." + index, item.clone());
		}
		return encode(ARRAY_PREFIX, yaml.saveToString());
	}

	public static String serializeItems(Collection<ItemStack> items) {
		Objects.requireNonNull(items, "items");
		return serializeArray(items.toArray(new ItemStack[0]));
	}

	public static ItemStack[] deserializeArray(String serialized) {
		YamlConfiguration yaml = load(decode(ARRAY_PREFIX, serialized));
		int size = yaml.getInt("size", -1);
		if (size < 0 || size > MAX_ARRAY_SIZE) {
			throw new IllegalArgumentException("Invalid serialized item array size: " + size);
		}
		ItemStack[] items = new ItemStack[size];
		for (int index = 0; index < size; index++) {
			ItemStack item = yaml.getItemStack(ITEMS_PATH + "." + index);
			items[index] = item == null ? null : item.clone();
		}
		return items;
	}

	public static void write(MutableConfig config, String path, ItemStack item) {
		Objects.requireNonNull(config, "config");
		Objects.requireNonNull(path, "path");
		if (item == null) {
			config.remove(path);
		} else {
			config.set(path, serialize(item));
		}
	}

	public static Optional<ItemStack> read(ConfigView config, String path) {
		Objects.requireNonNull(config, "config");
		Objects.requireNonNull(path, "path");
		String serialized = config.getString(path);
		return serialized == null || serialized.trim().isEmpty()
				? Optional.empty()
				: Optional.of(deserialize(serialized));
	}

	public static void writeArray(MutableConfig config, String path, ItemStack[] items) {
		Objects.requireNonNull(config, "config").set(
				Objects.requireNonNull(path, "path"),
				serializeArray(items)
		);
	}

	public static Optional<ItemStack[]> readArray(ConfigView config, String path) {
		Objects.requireNonNull(config, "config");
		Objects.requireNonNull(path, "path");
		String serialized = config.getString(path);
		return serialized == null || serialized.trim().isEmpty()
				? Optional.empty()
				: Optional.of(deserializeArray(serialized));
	}

	private static String encode(String prefix, String yaml) {
		String payload = Base64.getEncoder().encodeToString(yaml.getBytes(StandardCharsets.UTF_8));
		if (payload.length() > MAX_ENCODED_LENGTH) {
			throw new IllegalArgumentException("Serialized item data is too large");
		}
		return prefix + payload;
	}

	private static String decode(String prefix, String serialized) {
		Objects.requireNonNull(serialized, "serialized");
		if (!serialized.startsWith(prefix)) {
			throw new IllegalArgumentException("Unsupported item serialization format");
		}
		String payload = serialized.substring(prefix.length());
		if (payload.isEmpty() || payload.length() > MAX_ENCODED_LENGTH) {
			throw new IllegalArgumentException("Invalid serialized item data length");
		}
		try {
			return new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Invalid Base64 item data", exception);
		}
	}

	private static YamlConfiguration load(String content) {
		YamlConfiguration yaml = new YamlConfiguration();
		try {
			yaml.loadFromString(content);
			return yaml;
		} catch (InvalidConfigurationException exception) {
			throw new IllegalArgumentException("Invalid serialized item YAML", exception);
		}
	}
}

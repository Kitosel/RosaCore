package pl.kiosel.rosacore.material;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import pl.kiosel.rosacore.config.ConfigLoadResult;
import pl.kiosel.rosacore.config.ConfigSaveResult;
import pl.kiosel.rosacore.config.RosaConfig;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Getter
public final class CraftingRecipeStore {

	private static final String ROOT = "recipes";

	private final RosaConfig config;

	public CraftingRecipeStore(RosaConfig config) {
		this.config = Objects.requireNonNull(config, "config");
		if (!config.isLoaded()) {
			ConfigLoadResult result = config.load();
			if (!result.isSuccess()) {
				throw loadFailure(config, result);
			}
		}
	}

	public static CraftingRecipeStore open(Plugin plugin, String relativePath) {
		return new CraftingRecipeStore(new RosaConfig(
				Objects.requireNonNull(plugin, "plugin"), relativePath));
	}

	public ConfigLoadResult reload() {
		return this.config.reload();
	}

	public boolean contains(String key) {
		return this.config.contains(path(key));
	}

	public Optional<CraftingRecipeData> find(String key) {
		String normalized = CraftingRecipeData.normalizeKey(key);
		String path = path(normalized);
		if (!this.config.contains(path)) {
			return Optional.empty();
		}

		String storedKey = this.config.getString(path + ".key");
		String typeName = this.config.getString(path + ".type");
		String serializedIngredients = this.config.getString(path + ".ingredients");
		String serializedResult = this.config.getString(path + ".result");
		if (!normalized.equals(storedKey) || typeName == null
				|| serializedIngredients == null || serializedResult == null) {
			throw new IllegalStateException("Incomplete stored crafting recipe: " + normalized);
		}

		CraftingRecipeData.ShapeType type;
		try {
			type = CraftingRecipeData.ShapeType.valueOf(typeName);
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("Unknown crafting recipe type for " + normalized, exception);
		}
		ItemStack[] ingredients = ItemSerializer.deserializeArray(serializedIngredients);
		ItemStack result = ItemSerializer.deserialize(serializedResult);
		return Optional.of(new CraftingRecipeData(normalized, type,
				this.config.getBoolean(path + ".exact-ingredients", true), ingredients, result));
	}

	public Map<String, CraftingRecipeData> getAll() {
		ConfigurationSection section = this.config.getConfigurationSection(ROOT);
		if (section == null) return Collections.emptyMap();
		Map<String, CraftingRecipeData> recipes = new LinkedHashMap<>();
		for (String encodedId : section.getKeys(false)) {
			String key = this.config.getString(ROOT + "." + encodedId + ".key");
			if (key == null) {
				throw new IllegalStateException("Stored crafting recipe has no key: " + encodedId);
			}
			CraftingRecipeData data = this.find(key).orElseThrow(
					() -> new IllegalStateException("Could not load stored crafting recipe: " + key));
			recipes.put(data.getKey(), data);
		}
		return Collections.unmodifiableMap(recipes);
	}

	public ConfigSaveResult save(CraftingRecipeData data) {
		Objects.requireNonNull(data, "data");
		String ingredients = ItemSerializer.serializeArray(data.getIngredients());
		String result = ItemSerializer.serialize(data.getResult());
		String path = path(data.getKey());

		this.config.remove(path);
		this.config.set(path + ".key", data.getKey());
		this.config.set(path + ".type", data.getType().name());
		this.config.set(path + ".exact-ingredients", data.hasExactIngredients());
		this.config.set(path + ".ingredients", ingredients);
		this.config.set(path + ".result", result);
		return this.config.save();
	}

	public ConfigSaveResult delete(String key) {
		String path = path(key);
		if (!this.config.contains(path)) return ConfigSaveResult.success();
		this.config.remove(path);
		return this.config.save();
	}

	private static String path(String key) {
		String normalized = CraftingRecipeData.normalizeKey(key);
		String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(
				normalized.getBytes(StandardCharsets.UTF_8));
		return ROOT + ".r_" + encoded;
	}

	private static IllegalStateException loadFailure(RosaConfig config, ConfigLoadResult result) {
		String problem = result.getProblems().isEmpty()
				? "unknown problem"
				: result.getProblems().get(0).toString();
		return new IllegalStateException("Could not load crafting recipes "
				+ config.getPath() + ": " + problem, result.getCause());
	}
}

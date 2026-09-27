package pl.kiosel.rosacore.loot;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.material.MaterialData;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.compatibility.ZMaterial;
import pl.kiosel.rosacore.material.ResolvedMaterial;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Builder for shaped and shapeless Bukkit recipes from 1.8.8 through 26.2.
 * Modern servers receive a namespaced recipe and exact item choices. On old
 * servers an exact choice gracefully falls back to material plus legacy data.
 */
public final class RecipeBuilder {

	private final RosaPlugin plugin;
	@Getter
	private final String keyName;
	private final Map<Character, Ingredient> ingredients = new LinkedHashMap<>();
	private String[] shape;
	private ItemStack result;
	@Getter
	private boolean shapeless;
	private Recipe registeredRecipe;
	private Object namespacedKey;

	public RecipeBuilder(RosaPlugin plugin, String name) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.keyName = normalizeKey(name);
	}

	public RecipeBuilder shapeless() {
		this.shapeless = true;
		return this;
	}

	public RecipeBuilder shaped() {
		this.shapeless = false;
		return this;
	}

	public RecipeBuilder shape(String... shape) {
		Objects.requireNonNull(shape, "shape");
		if (shape.length < 1 || shape.length > 3) {
			throw new IllegalArgumentException("A recipe shape must contain between 1 and 3 rows");
		}
		String[] copy = shape.clone();
		for (String row : copy) {
			if (row == null || row.length() < 1 || row.length() > 3) {
				throw new IllegalArgumentException("Every recipe row must contain between 1 and 3 characters");
			}
		}
		this.shape = copy;
		this.shapeless = false;
		return this;
	}

	public RecipeBuilder setIngredient(char key, ZMaterial material) {
		this.ingredients.put(key, Ingredient.material(Objects.requireNonNull(material, "material")));
		return this;
	}

	public RecipeBuilder setIngredient(char key, Material material) {
		return setIngredient(key, ZMaterial.from(Objects.requireNonNull(material, "material")));
	}

	public RecipeBuilder setIngredient(char key, ItemStack item) {
		this.ingredients.put(key, Ingredient.exact(Objects.requireNonNull(item, "item")));
		return this;
	}

	public RecipeBuilder removeIngredient(char key) {
		this.ingredients.remove(key);
		return this;
	}

	public RecipeBuilder setResult(ItemStack result) {
		this.result = Objects.requireNonNull(result, "result").clone();
		return this;
	}

	public RecipeBuilder setResult(ZMaterial material) {
		return setResult(Objects.requireNonNull(material, "material").requireItem());
	}

	/**
	 * Builds and registers the recipe on the running server.
	 */
	public Recipe build() {
		validate();
		Recipe recipe = shapeless ? createShapeless() : createShaped();
		if (!plugin.getServer().addRecipe(recipe)) {
			throw new IllegalStateException("Could not register recipe " + keyName + " (the key may already exist)");
		}
		this.registeredRecipe = recipe;
		return recipe;
	}

	/**
	 * Alias that makes the side effect of {@link #build()} explicit.
	 */
	public Recipe register() {
		return build();
	}

	public boolean unregister() {
		Server server = plugin.getServer();
		Object key = getKey();
		if (key != null) {
			try {
				Method removeRecipe = server.getClass().getMethod("removeRecipe", key.getClass());
				Object removed = removeRecipe.invoke(server, key);
				if (!(removed instanceof Boolean) || (Boolean) removed) {
					registeredRecipe = null;
					return true;
				}
			} catch (ReflectiveOperationException | LinkageError ignored) {
				// 1.8-1.12: remove the registered instance through the iterator.
			}
		}

		if (registeredRecipe == null) {
			return false;
		}
		Iterator<Recipe> iterator = server.recipeIterator();
		while (iterator.hasNext()) {
			if (iterator.next() == registeredRecipe) {
				iterator.remove();
				registeredRecipe = null;
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns Bukkit's NamespacedKey on modern servers, otherwise null.
	 */
	public Object getKey() {
		if (namespacedKey == null) {
			namespacedKey = createNamespacedKey();
		}
		return namespacedKey;
	}

	public List<String> getShape() {
		return shape == null
				? Collections.emptyList()
				: Collections.unmodifiableList(Arrays.asList(shape.clone()));
	}

	public Map<Character, ItemStack> getIngredients() {
		return getIngredientItems();
	}

	public Map<Character, ZMaterial> getMaterialIngredients() {
		Map<Character, ZMaterial> values = new LinkedHashMap<>();
		for (Map.Entry<Character, Ingredient> entry : ingredients.entrySet()) {
			ZMaterial material = entry.getValue().material;
			if (material != null) {
				values.put(entry.getKey(), material);
			}
		}
		return Collections.unmodifiableMap(values);
	}

	public Map<Character, ItemStack> getIngredientItems() {
		Map<Character, ItemStack> values = new LinkedHashMap<>();
		for (Map.Entry<Character, Ingredient> entry : ingredients.entrySet()) {
			values.put(entry.getKey(), entry.getValue().asItem());
		}
		return Collections.unmodifiableMap(values);
	}

	public ItemStack getResult() {
		return result == null ? null : result.clone();
	}

	private Recipe createShaped() {
		ShapedRecipe recipe = newModernRecipe(ShapedRecipe.class);
		if (recipe == null) {
			recipe = new ShapedRecipe(result.clone());
		}
		recipe.shape(shape.clone());
		for (Map.Entry<Character, Ingredient> entry : ingredients.entrySet()) {
			if (!setModernChoice(recipe, "setIngredient", entry.getKey(), entry.getValue())) {
				setLegacyIngredient(recipe, entry.getKey(), entry.getValue());
			}
		}
		return recipe;
	}

	private Recipe createShapeless() {
		ShapelessRecipe recipe = newModernRecipe(ShapelessRecipe.class);
		if (recipe == null) {
			recipe = new ShapelessRecipe(result.clone());
		}
		for (Ingredient ingredient : ingredients.values()) {
			if (!setModernChoice(recipe, "addIngredient", null, ingredient)) {
				addLegacyIngredient(recipe, ingredient);
			}
		}
		return recipe;
	}

	private <T extends Recipe> T newModernRecipe(Class<T> recipeClass) {
		Object key = getKey();
		if (key == null) {
			return null;
		}
		try {
			Constructor<T> constructor = recipeClass.getConstructor(key.getClass(), ItemStack.class);
			return constructor.newInstance(key, result.clone());
		} catch (ReflectiveOperationException | LinkageError ignored) {
			return null;
		}
	}

	private boolean setModernChoice(Object recipe, String methodName, Character symbol, Ingredient ingredient) {
		Object choice = ingredient.createModernChoice();
		if (choice == null) {
			return false;
		}
		try {
			Class<?> choiceClass = Class.forName("org.bukkit.inventory.RecipeChoice");
			Method method = symbol == null
					? recipe.getClass().getMethod(methodName, choiceClass)
					: recipe.getClass().getMethod(methodName, char.class, choiceClass);
			if (symbol == null) {
				method.invoke(recipe, choice);
			} else {
				method.invoke(recipe, symbol.charValue(), choice);
			}
			return true;
		} catch (ReflectiveOperationException | IllegalArgumentException | LinkageError ignored) {
			return false;
		}
	}

	@SuppressWarnings("deprecation")
	private static void setLegacyIngredient(ShapedRecipe recipe, char key, Ingredient ingredient) {
		ResolvedMaterial material = ingredient.resolve();
		if (material.hasLegacyData()) {
			recipe.setIngredient(key, new MaterialData(material.getMaterial(),
					(byte) material.getLegacyData().getAsInt()));
		} else {
			recipe.setIngredient(key, material.getMaterial());
		}
	}

	@SuppressWarnings("deprecation")
	private static void addLegacyIngredient(ShapelessRecipe recipe, Ingredient ingredient) {
		ResolvedMaterial material = ingredient.resolve();
		if (material.hasLegacyData()) {
			recipe.addIngredient(new MaterialData(material.getMaterial(),
					(byte) material.getLegacyData().getAsInt()));
		} else {
			recipe.addIngredient(material.getMaterial());
		}
	}

	private Object createNamespacedKey() {
		try {
			Class<?> keyClass = Class.forName("org.bukkit.NamespacedKey");
			Class<?> pluginClass = Class.forName("org.bukkit.plugin.Plugin");
			Constructor<?> constructor = keyClass.getConstructor(pluginClass, String.class);
			return constructor.newInstance(plugin, keyName);
		} catch (ReflectiveOperationException | LinkageError ignored) {
			return null;
		}
	}

	private void validate() {
		if (result == null) {
			throw new IllegalStateException("No result set for recipe " + keyName);
		}
		if (ingredients.isEmpty()) {
			throw new IllegalStateException("No ingredients set for recipe " + keyName);
		}
		if (!shapeless && shape == null) {
			throw new IllegalStateException("No shape defined for shaped recipe " + keyName);
		}
		for (Map.Entry<Character, Ingredient> entry : ingredients.entrySet()) {
			try {
				entry.getValue().resolve();
			} catch (IllegalStateException exception) {
				throw new IllegalStateException("Ingredient '" + entry.getKey() + "' in recipe "
						+ keyName + " is unavailable on this server", exception);
			}
		}
	}

	private static String normalizeKey(String name) {
		Objects.requireNonNull(name, "name");
		String key = name.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
		if (key.isEmpty() || !key.matches("[a-z0-9._/-]+")) {
			throw new IllegalArgumentException("Invalid recipe key: " + name);
		}
		return key;
	}

	private static final class Ingredient {
		private final ZMaterial material;
		private final ItemStack item;

		private Ingredient(ZMaterial material, ItemStack item) {
			this.material = material;
			this.item = item;
		}

		private static Ingredient material(ZMaterial material) {
			return new Ingredient(material, null);
		}

		private static Ingredient exact(ItemStack item) {
			return new Ingredient(null, item.clone());
		}

		private ResolvedMaterial resolve() {
			if (material != null) {
				return material.resolveForItem().orElseThrow(
						() -> new IllegalStateException("Unsupported material " + material));
			}
			return ZMaterial.from(item).resolveForItem().orElseThrow(
					() -> new IllegalStateException("Unsupported item material " + item.getType()));
		}

		private ItemStack asItem() {
			return item != null ? item.clone() : material.requireItem();
		}

		private Object createModernChoice() {
			try {
				if (item != null) {
					Class<?> type = Class.forName("org.bukkit.inventory.RecipeChoice$ExactChoice");
					Constructor<?> constructor = type.getConstructor(ItemStack.class);
					return constructor.newInstance(item.clone());
				}
				Class<?> type = Class.forName("org.bukkit.inventory.RecipeChoice$MaterialChoice");
				Constructor<?> constructor = type.getConstructor(Material.class);
				return constructor.newInstance(resolve().getMaterial());
			} catch (ReflectiveOperationException | IllegalArgumentException | LinkageError ignored) {
				return null;
			}
		}
	}
}

package pl.kiosel.rosacore.material;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.compatibility.ZMaterial;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public final class CraftingRecipeData {

	public enum ShapeType {
		SHAPED,
		SHAPELESS
	}

	private static final int GRID_SIZE = 9;

	@Getter
	private final String key;
	@Getter
	private final ShapeType type;
	private final boolean exactIngredients;
	private final ItemStack[] ingredients;
	private final ItemStack result;

	public CraftingRecipeData(String key, ShapeType type, boolean exactIngredients,
							  ItemStack[] ingredients, ItemStack result) {
		this.key = normalizeKey(key);
		this.type = Objects.requireNonNull(type, "type");
		this.exactIngredients = exactIngredients;
		if (ingredients == null || ingredients.length != GRID_SIZE) {
			throw new IllegalArgumentException("Crafting ingredient grid must contain exactly 9 slots");
		}
		this.ingredients = cloneGrid(ingredients);
		this.result = requireItem(result, "result");
		if (!hasIngredient(this.ingredients)) {
			throw new IllegalArgumentException("Crafting recipe must contain at least one ingredient");
		}
	}

	public boolean isShapeless() {
		return this.type == ShapeType.SHAPELESS;
	}

	public boolean hasExactIngredients() {
		return this.exactIngredients;
	}

	public ItemStack[] getIngredients() {
		return cloneGrid(this.ingredients);
	}

	public ItemStack getIngredient(int index) {
		checkIndex(index);
		return cloneOrNull(this.ingredients[index]);
	}

	public ItemStack getIngredient(int row, int column) {
		if (row < 1 || row > 3 || column < 1 || column > 3) {
			throw new IndexOutOfBoundsException("Crafting row and column must be between 1 and 3");
		}
		return getIngredient((row - 1) * 3 + column - 1);
	}

	public ItemStack getResult() {
		return this.result.clone();
	}

	public RecipeBuilder toRecipeBuilder(RosaPlugin plugin) {
		RecipeBuilder builder = new RecipeBuilder(Objects.requireNonNull(plugin, "plugin"), this.key)
				.setResult(this.result);
		if (this.type == ShapeType.SHAPELESS) {
			builder.shapeless();
			char symbol = 'A';
			for (ItemStack ingredient : this.ingredients) {
				if (isEmpty(ingredient)) continue;
				setIngredient(builder, symbol++, ingredient);
			}
			return builder;
		}

		int minimumRow = 3;
		int maximumRow = -1;
		int minimumColumn = 3;
		int maximumColumn = -1;
		for (int index = 0; index < this.ingredients.length; index++) {
			if (isEmpty(this.ingredients[index])) continue;
			int row = index / 3;
			int column = index % 3;
			minimumRow = Math.min(minimumRow, row);
			maximumRow = Math.max(maximumRow, row);
			minimumColumn = Math.min(minimumColumn, column);
			maximumColumn = Math.max(maximumColumn, column);
		}

		String[] shape = new String[maximumRow - minimumRow + 1];
		char symbol = 'A';
		for (int row = minimumRow; row <= maximumRow; row++) {
			StringBuilder shapeRow = new StringBuilder();
			for (int column = minimumColumn; column <= maximumColumn; column++) {
				ItemStack ingredient = this.ingredients[row * 3 + column];
				if (isEmpty(ingredient)) {
					shapeRow.append(' ');
				} else {
					shapeRow.append(symbol);
					setIngredient(builder, symbol++, ingredient);
				}
			}
			shape[row - minimumRow] = shapeRow.toString();
		}
		return builder.shaped().shape(shape);
	}

	public Recipe register(RosaPlugin plugin) {
		return this.toRecipeBuilder(plugin).register();
	}

	private void setIngredient(RecipeBuilder builder, char symbol, ItemStack ingredient) {
		if (this.exactIngredients) {
			builder.setIngredient(symbol, ingredient);
		} else {
			builder.setIngredient(symbol, ZMaterial.from(ingredient));
		}
	}

	public static String normalizeKey(String key) {
		Objects.requireNonNull(key, "key");
		String normalized = key.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
		if (normalized.isEmpty() || !normalized.matches("[a-z0-9._/-]+")) {
			throw new IllegalArgumentException("Invalid recipe key: " + key);
		}
		return normalized;
	}

	private static ItemStack[] cloneGrid(ItemStack[] ingredients) {
		ItemStack[] copy = new ItemStack[ingredients.length];
		for (int index = 0; index < ingredients.length; index++) {
			ItemStack ingredient = ingredients[index];
			copy[index] = isEmpty(ingredient) ? null : ingredient.clone();
		}
		return copy;
	}

	private static ItemStack requireItem(ItemStack item, String name) {
		if (isEmpty(item)) {
			throw new IllegalArgumentException(name + " cannot be empty");
		}
		return item.clone();
	}

	private static boolean hasIngredient(ItemStack[] ingredients) {
		for (ItemStack ingredient : ingredients) {
			if (!isEmpty(ingredient)) return true;
		}
		return false;
	}

	private static boolean isEmpty(ItemStack item) {
		return item == null || item.getType() == Material.AIR;
	}

	private static ItemStack cloneOrNull(ItemStack item) {
		return item == null ? null : item.clone();
	}

	private static void checkIndex(int index) {
		if (index < 0 || index >= GRID_SIZE) {
			throw new IndexOutOfBoundsException("Crafting ingredient index must be between 0 and 8");
		}
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof CraftingRecipeData)) return false;
		CraftingRecipeData other = (CraftingRecipeData) object;
		return this.exactIngredients == other.exactIngredients
				&& this.key.equals(other.key)
				&& this.type == other.type
				&& Arrays.equals(this.ingredients, other.ingredients)
				&& this.result.equals(other.result);
	}

	@Override
	public int hashCode() {
		int resultHash = Objects.hash(this.key, this.type, this.exactIngredients, this.result);
		return 31 * resultHash + Arrays.hashCode(this.ingredients);
	}
}

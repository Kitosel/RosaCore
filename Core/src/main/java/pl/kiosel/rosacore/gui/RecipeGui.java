package pl.kiosel.rosacore.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.compatibility.ZMaterial;
import pl.kiosel.rosacore.material.CraftingRecipeData;
import pl.kiosel.rosacore.material.ItemCreator;
import pl.kiosel.rosacore.material.RecipeBuilder;

import java.util.*;

public class RecipeGui extends Gui {

	private static final int GRID_SIZE = 9;
	private static final int[] DEFAULT_INGREDIENT_SLOTS = {
			10, 11, 12,
			19, 20, 21,
			28, 29, 30
	};

	private final ItemStack[] ingredients = new ItemStack[GRID_SIZE];
	private final Set<Integer> managedSlots = new LinkedHashSet<>();

	private ItemStack result;
	private ItemStack backgroundItem;
	private ItemStack arrowItem;
	private ItemStack backItem;
	private int[] ingredientSlots = DEFAULT_INGREDIENT_SLOTS.clone();
	private int arrowSlot = 23;
	private int resultSlot = 25;
	private int backSlot = 44;
	private boolean fillEmpty = true;
	private boolean showArrow = true;
	private boolean showBackButton = true;

	public RecipeGui(RecipeBuilder recipe) {
		this(recipe, null);
	}

	public RecipeGui(RecipeBuilder recipe, Gui parent) {
		super(5, parent);
		defaultItems();
		recipe(recipe);
	}

	public RecipeGui(CraftingRecipeData recipe) {
		this(recipe, null);
	}

	public RecipeGui(CraftingRecipeData recipe, Gui parent) {
		super(5, parent);
		defaultItems();
		recipe(recipe);
	}

	public RecipeGui title(String title) {
		setTitle(title);
		return this;
	}

	public RecipeGui recipe(RecipeBuilder recipe) {
		Objects.requireNonNull(recipe, "recipe");
		clearRecipeData();
		this.result = requireItem(recipe.getResult(), "recipe result");

		Map<Character, ItemStack> ingredientItems = recipe.getIngredientItems();
		if (recipe.isShapeless()) {
			copyShapeless(ingredientItems.values());
		} else {
			List<String> shape = recipe.getShape();
			if (shape.isEmpty()) throw new IllegalArgumentException("Shaped recipe has no shape");
			for (int row = 0; row < Math.min(3, shape.size()); row++) {
				String shapeRow = shape.get(row);
				for (int column = 0; column < Math.min(3, shapeRow.length()); column++) {
					char symbol = shapeRow.charAt(column);
					if (symbol != ' ') this.ingredients[row * 3 + column] = cloneOrNull(ingredientItems.get(symbol));
				}
			}
		}
		validateHasIngredient();
		return this;
	}

	public RecipeGui recipe(CraftingRecipeData recipe) {
		Objects.requireNonNull(recipe, "recipe");
		clearRecipeData();
		this.result = requireItem(recipe.getResult(), "recipe result");
		ItemStack[] grid = recipe.getIngredients();
		if (recipe.isShapeless()) {
			copyShapeless(Arrays.asList(grid));
		} else {
			for (int index = 0; index < GRID_SIZE; index++) {
				this.ingredients[index] = cloneOrNull(grid[index]);
			}
		}
		validateHasIngredient();
		return this;
	}

	public RecipeGui ingredientSlots(int... slots) {
		Objects.requireNonNull(slots, "slots");
		if (slots.length != GRID_SIZE) {
			throw new IllegalArgumentException("Recipe ingredient layout must contain exactly 9 slots");
		}
		Set<Integer> unique = new HashSet<>();
		for (int slot : slots) {
			validateSlot(slot);
			if (!unique.add(slot)) throw new IllegalArgumentException("Recipe ingredient slots must be unique");
		}
		this.ingredientSlots = slots.clone();
		return this;
	}

	public RecipeGui arrowSlot(int slot) {
		validateSlot(slot);
		this.arrowSlot = slot;
		return this;
	}

	public RecipeGui resultSlot(int slot) {
		validateSlot(slot);
		this.resultSlot = slot;
		return this;
	}

	public RecipeGui backSlot(int slot) {
		validateSlot(slot);
		this.backSlot = slot;
		return this;
	}

	public RecipeGui backgroundItem(ItemStack item) {
		this.backgroundItem = cloneOrNull(item);
		return this;
	}

	public RecipeGui arrowItem(ItemStack item) {
		this.arrowItem = cloneOrNull(item);
		return this;
	}

	public RecipeGui backItem(ItemStack item) {
		this.backItem = cloneOrNull(item);
		return this;
	}

	public RecipeGui fillSlots(boolean fill) {
		this.fillEmpty = fill;
		return this;
	}

	public RecipeGui showArrow(boolean show) {
		this.showArrow = show;
		return this;
	}

	public RecipeGui showBackButton(boolean show) {
		this.showBackButton = show;
		return this;
	}

	public RecipeGui defaultItems() {
		title("Crafting recipe");
		backgroundItem(ItemCreator.of(ZMaterial.WHITE_STAINED_GLASS_PANE).name(" ").makeMenuItem());
		arrowItem(ItemCreator.of(ZMaterial.ARROW).name("&e→").makeMenuItem());
		backItem(ItemCreator.of(ZMaterial.ARROW).name("&9Back").makeMenuItem());
		return this;
	}

	public RecipeGui setItems() {
		validateLayout();
		clearManagedSlots();
		setDefaultItem(this.fillEmpty ? this.backgroundItem : null);

		for (int index = 0; index < GRID_SIZE; index++) {
			int slot = this.ingredientSlots[index];
			this.managedSlots.add(slot);
			if (!isEmpty(this.ingredients[index])) setItem(slot, this.ingredients[index]);
		}
		if (this.showArrow) {
			setItem(this.arrowSlot, this.arrowItem);
			this.managedSlots.add(this.arrowSlot);
		}
		setItem(this.resultSlot, this.result);
		this.managedSlots.add(this.resultSlot);

		Gui parent = getParent();
		if (this.showBackButton && parent != null) {
			setButton(this.backSlot, this.backItem,
					event -> event.getManager().openGUI(event.getPlayer(), parent));
			this.managedSlots.add(this.backSlot);
		}
		update();
		return this;
	}

	public ItemStack getIngredient(int index) {
		if (index < 0 || index >= GRID_SIZE) throw new IndexOutOfBoundsException("index must be between 0 and 8");
		return cloneOrNull(this.ingredients[index]);
	}

	public ItemStack getResultItem() {
		return cloneOrNull(this.result);
	}

	private void validateLayout() {
		Objects.requireNonNull(this.result, "recipe result");
		if (this.fillEmpty) Objects.requireNonNull(this.backgroundItem, "backgroundItem");
		if (this.showArrow) Objects.requireNonNull(this.arrowItem, "arrowItem");
		if (this.showBackButton && getParent() != null) Objects.requireNonNull(this.backItem, "backItem");

		Set<Integer> occupied = new HashSet<>();
		for (int slot : this.ingredientSlots) occupied.add(slot);
		if (this.showArrow && !occupied.add(this.arrowSlot))
			throw new IllegalStateException("Recipe arrow slot overlaps another recipe item");
		if (!occupied.add(this.resultSlot))
			throw new IllegalStateException("Recipe result slot overlaps another recipe item");
		if (this.showBackButton && getParent() != null && !occupied.add(this.backSlot))
			throw new IllegalStateException("Recipe back slot overlaps another recipe item");
	}

	private void clearManagedSlots() {
		for (Integer slot : this.managedSlots) {
			this.cellItems.remove(slot);
			this.actions.remove(slot);
			this.clickSounds.remove(slot);
		}
		this.managedSlots.clear();
	}

	private void clearRecipeData() {
		Arrays.fill(this.ingredients, null);
		this.result = null;
	}

	private void copyShapeless(Collection<ItemStack> source) {
		int index = 0;
		for (ItemStack ingredient : source) {
			if (isEmpty(ingredient)) continue;
			if (index >= GRID_SIZE) throw new IllegalArgumentException("Shapeless recipe has more than 9 ingredients");
			this.ingredients[index++] = ingredient.clone();
		}
	}

	private void validateHasIngredient() {
		for (ItemStack ingredient : this.ingredients) {
			if (!isEmpty(ingredient)) return;
		}
		throw new IllegalArgumentException("Recipe must contain at least one ingredient");
	}

	private void validateSlot(int slot) {
		if (slot < 0 || slot >= getSize()) {
			throw new IllegalArgumentException("slot must be between 0 and " + (getSize() - 1));
		}
	}

	private static ItemStack requireItem(ItemStack item, String name) {
		if (isEmpty(item)) throw new IllegalArgumentException(name + " cannot be empty");
		return item.clone();
	}

	private static boolean isEmpty(ItemStack item) {
		return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
	}

	private static ItemStack cloneOrNull(ItemStack item) {
		return item == null ? null : item.clone();
	}
}

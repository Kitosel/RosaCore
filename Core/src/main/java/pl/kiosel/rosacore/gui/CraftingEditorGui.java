package pl.kiosel.rosacore.gui;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.compatibility.ZMaterial;
import pl.kiosel.rosacore.config.ConfigSaveResult;
import pl.kiosel.rosacore.material.CraftingRecipeData;
import pl.kiosel.rosacore.material.CraftingRecipeStore;
import pl.kiosel.rosacore.material.ItemCreator;
import pl.kiosel.rosacore.utils.ColorUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CraftingEditorGui extends Gui {

	public static final int[] INGREDIENT_SLOTS = {1, 2, 3, 10, 11, 12, 19, 20, 21};
	public static final int ARROW_SLOT = 14;
	public static final int RESULT_SLOT = 16;
	public static final int CLEAR_SLOT = 36;
	public static final int TYPE_SLOT = 38;
	public static final int EXACT_SLOT = 40;
	public static final int DELETE_SLOT = 42;
	public static final int CANCEL_SLOT = 43;
	public static final int SAVE_SLOT = 44;

	@Getter
	private final String recipeKey;
	private final Set<UUID> resolvedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
	@Getter
	private CraftingRecipeData.ShapeType recipeType = CraftingRecipeData.ShapeType.SHAPED;
	private boolean exactIngredients = true;
	private boolean closeOnSave = true;
	@Getter
	private CraftingRecipeStore store;

	private final ItemStack fillerItem;
	private BiConsumer<Player, CraftingRecipeData> saveAction = (player, recipe) -> {
	};
	private Consumer<Player> cancelAction = player -> {
	};
	private BiConsumer<Player, String> deleteAction = (player, key) -> {
	};
	private BiConsumer<Player, String> errorAction = (player, message) ->
			player.sendMessage(ColorUtils.color("&c" + message));

	public CraftingEditorGui(String recipeKey) {
		this(recipeKey, null);
	}

	public CraftingEditorGui(String recipeKey, Gui parent) {
		super(5, parent);
		this.recipeKey = CraftingRecipeData.normalizeKey(recipeKey);
		this.title = "&8Crafting: " + this.recipeKey;
		this.fillerItem = ItemCreator.of(ZMaterial.GRAY_STAINED_GLASS_PANE)
				.name(" ")
				.makeMenuItem();
		this.setAcceptsItems(true);
		this.setupLayout();
	}

	public CraftingEditorGui(CraftingRecipeData recipe) {
		this(recipe.getKey());
		this.loadRecipe(recipe);
	}

	public static CraftingEditorGui callback(String recipeKey,
											 BiConsumer<Player, CraftingRecipeData> onSave) {
		return new CraftingEditorGui(recipeKey).onSave(onSave);
	}

	public static CraftingEditorGui callback(String recipeKey, Consumer<CraftingRecipeData> onSave) {
		return new CraftingEditorGui(recipeKey).onSave(onSave);
	}

	public static CraftingEditorGui stored(CraftingRecipeStore store, String recipeKey) {
		return stored(store, recipeKey, (player, recipe) -> {
		});
	}

	public static CraftingEditorGui stored(CraftingRecipeStore store, String recipeKey,
										   BiConsumer<Player, CraftingRecipeData> onSave) {
		CraftingEditorGui editor = new CraftingEditorGui(recipeKey)
				.store(store)
				.onSave(onSave);
		store.find(editor.recipeKey).ifPresent(editor::loadRecipe);
		return editor;
	}

	public CraftingEditorGui setRecipeType(CraftingRecipeData.ShapeType recipeType) {
		this.recipeType = Objects.requireNonNull(recipeType, "recipeType");
		this.refreshModeItems();
		return this;
	}

	public boolean hasExactIngredients() {
		return this.exactIngredients;
	}

	public CraftingEditorGui setExactIngredients(boolean exactIngredients) {
		this.exactIngredients = exactIngredients;
		this.refreshModeItems();
		return this;
	}

	public CraftingEditorGui setIngredient(int index, ItemStack ingredient) {
		if (index < 0 || index >= INGREDIENT_SLOTS.length) {
			throw new IndexOutOfBoundsException("Crafting ingredient index must be between 0 and 8");
		}
		ItemStack copy = copyOrNull(ingredient);
		if (copy != null) copy.setAmount(1);
		this.setItem(INGREDIENT_SLOTS[index], copy);
		return this;
	}

	public CraftingEditorGui setIngredient(int row, int column, ItemStack ingredient) {
		if (row < 1 || row > 3 || column < 1 || column > 3) {
			throw new IndexOutOfBoundsException("Crafting row and column must be between 1 and 3");
		}
		return setIngredient((row - 1) * 3 + column - 1, ingredient);
	}

	public ItemStack getIngredient(int index) {
		if (index < 0 || index >= INGREDIENT_SLOTS.length) {
			throw new IndexOutOfBoundsException("Crafting ingredient index must be between 0 and 8");
		}
		return this.getItem(INGREDIENT_SLOTS[index]);
	}

	public CraftingEditorGui setResult(ItemStack result) {
		this.setItem(RESULT_SLOT, copyOrNull(result));
		return this;
	}

	public ItemStack getResult() {
		return this.getItem(RESULT_SLOT);
	}

	public CraftingEditorGui loadRecipe(CraftingRecipeData recipe) {
		if (!this.recipeKey.equals(recipe.getKey())) {
			throw new IllegalArgumentException("Recipe key does not match this editor: " + recipe.getKey());
		}
		this.recipeType = recipe.getType();
		this.exactIngredients = recipe.hasExactIngredients();
		ItemStack[] ingredients = recipe.getIngredients();
		for (int index = 0; index < ingredients.length; index++) {
			this.setIngredient(index, ingredients[index]);
		}
		this.setResult(recipe.getResult());
		this.refreshModeItems();
		return this;
	}

	public CraftingRecipeData getRecipeData() {
		ItemStack[] ingredients = new ItemStack[INGREDIENT_SLOTS.length];
		for (int index = 0; index < ingredients.length; index++) {
			ingredients[index] = this.getIngredient(index);
		}
		return new CraftingRecipeData(this.recipeKey, this.recipeType,
				this.exactIngredients, ingredients, this.getResult());
	}

	public CraftingEditorGui clearRecipe() {
		for (int index = 0; index < INGREDIENT_SLOTS.length; index++) {
			this.setIngredient(index, null);
		}
		this.setResult(null);
		return this;
	}

	public CraftingEditorGui store(CraftingRecipeStore store) {
		this.store = Objects.requireNonNull(store, "store");
		this.refreshDeleteButton();
		return this;
	}

	public CraftingEditorGui onSave(BiConsumer<Player, CraftingRecipeData> saveAction) {
		this.saveAction = Objects.requireNonNull(saveAction, "saveAction");
		return this;
	}

	public CraftingEditorGui onSave(Consumer<CraftingRecipeData> saveAction) {
		Objects.requireNonNull(saveAction, "saveAction");
		return this.onSave((player, recipe) -> saveAction.accept(recipe));
	}

	public CraftingEditorGui onCancel(Consumer<Player> cancelAction) {
		this.cancelAction = Objects.requireNonNull(cancelAction, "cancelAction");
		return this;
	}

	public CraftingEditorGui onDelete(BiConsumer<Player, String> deleteAction) {
		this.deleteAction = Objects.requireNonNull(deleteAction, "deleteAction");
		return this;
	}

	public CraftingEditorGui onError(BiConsumer<Player, String> errorAction) {
		this.errorAction = Objects.requireNonNull(errorAction, "errorAction");
		return this;
	}

	public CraftingEditorGui closeOnSave(boolean closeOnSave) {
		this.closeOnSave = closeOnSave;
		return this;
	}

	public CraftingEditorGui title(String title) {
		this.setTitle(title);
		return this;
	}

	@Override
	protected void onOpen(GuiManager manager, Player player) {
		this.resolvedPlayers.remove(player.getUniqueId());
	}

	@Override
	protected void onClose(GuiManager manager, Player player) {
		if (this.resolvedPlayers.remove(player.getUniqueId())) return;
		this.cancelAction.accept(player);
	}

	private void setupLayout() {
		this.fill(this.fillerItem);
		for (int index = 0; index < INGREDIENT_SLOTS.length; index++) {
			int ingredientIndex = index;
			this.setItem(INGREDIENT_SLOTS[index], null);
			this.setAction(INGREDIENT_SLOTS[index], event ->
					this.setIngredient(ingredientIndex, event.getCursor()));
		}
		this.setItem(RESULT_SLOT, null);
		this.setAction(RESULT_SLOT, event -> this.setResult(event.getCursor()));
		this.setItem(ARROW_SLOT, ItemCreator.of(ZMaterial.ARROW)
				.name("&7Result")
				.makeMenuItem());

		this.setButton(CLEAR_SLOT, ItemCreator.of(ZMaterial.BARRIER)
				.name("&cClear recipe")
				.lore("&7Removes all ingredients", "&7and the result.")
				.makeMenuItem(), event -> this.clearRecipe());
		this.setButton(TYPE_SLOT, this.createTypeItem(), event -> this.setRecipeType(
				this.recipeType == CraftingRecipeData.ShapeType.SHAPED
						? CraftingRecipeData.ShapeType.SHAPELESS
						: CraftingRecipeData.ShapeType.SHAPED));
		this.setButton(EXACT_SLOT, this.createExactItem(), event ->
				this.setExactIngredients(!this.exactIngredients));
		this.setButton(CANCEL_SLOT, ItemCreator.of(ZMaterial.RED_DYE)
				.name("&cCancel")
				.makeMenuItem(), event -> this.cancel(event.getPlayer()));
		this.setButton(SAVE_SLOT, ItemCreator.of(ZMaterial.LIME_DYE)
				.name("&aSave recipe")
				.makeMenuItem(), event -> this.save(event.getPlayer()));
		this.refreshDeleteButton();
	}

	private void refreshModeItems() {
		this.setItem(TYPE_SLOT, this.createTypeItem());
		this.setItem(EXACT_SLOT, this.createExactItem());
	}

	private ItemStack createTypeItem() {
		boolean shaped = this.recipeType == CraftingRecipeData.ShapeType.SHAPED;
		return ItemCreator.of(ZMaterial.CRAFTING_TABLE)
				.name(shaped ? "&aShaped recipe" : "&eShapeless recipe")
				.lore(shaped
								? "&7Ingredient positions matter."
								: "&7Ingredient positions do not matter.",
						"&eClick to switch.")
				.glow(shaped)
				.makeMenuItem();
	}

	private ItemStack createExactItem() {
		return ItemCreator.of(ZMaterial.HOPPER)
				.name(this.exactIngredients ? "&aExact items" : "&eMaterial only")
				.lore(this.exactIngredients
								? "&7Metadata, name and lore must match."
								: "&7Only the material must match.",
						"&eClick to switch.")
				.glow(this.exactIngredients)
				.makeMenuItem();
	}

	private void refreshDeleteButton() {
		if (this.store == null) {
			this.clearActions(DELETE_SLOT);
			this.setItem(DELETE_SLOT, this.fillerItem);
			return;
		}
		this.setButton(DELETE_SLOT, ItemCreator.of(ZMaterial.TNT)
				.name("&cDelete saved recipe")
				.makeMenuItem(), event -> this.delete(event.getPlayer()));
	}

	private void save(Player player) {
		CraftingRecipeData recipe;
		try {
			recipe = this.getRecipeData();
		} catch (IllegalArgumentException exception) {
			this.errorAction.accept(player, exception.getMessage());
			return;
		}
		if (this.store != null) {
			ConfigSaveResult result = this.store.save(recipe);
			if (!result.isSuccess()) {
				this.errorAction.accept(player, saveProblem("Could not save recipe", result));
				return;
			}
		}
		this.saveAction.accept(player, recipe);
		if (this.closeOnSave) {
			this.resolvedPlayers.add(player.getUniqueId());
			this.close();
		}
	}

	private void cancel(Player player) {
		this.resolvedPlayers.add(player.getUniqueId());
		this.cancelAction.accept(player);
		this.close();
	}

	private void delete(Player player) {
		if (this.store == null) return;
		ConfigSaveResult result = this.store.delete(this.recipeKey);
		if (!result.isSuccess()) {
			this.errorAction.accept(player, saveProblem("Could not delete recipe", result));
			return;
		}
		this.resolvedPlayers.add(player.getUniqueId());
		this.deleteAction.accept(player, this.recipeKey);
		this.close();
	}

	private static String saveProblem(String fallback, ConfigSaveResult result) {
		return result.getProblems().isEmpty() ? fallback : result.getProblems().get(0).toString();
	}

	private static ItemStack copyOrNull(ItemStack item) {
		return item == null || item.getType() == Material.AIR ? null : item.clone();
	}
}

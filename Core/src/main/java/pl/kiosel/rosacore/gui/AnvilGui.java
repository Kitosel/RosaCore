package pl.kiosel.rosacore.gui;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.kiosel.rosacore.nms.api.anvil.CustomAnvil;
import pl.kiosel.rosacore.utils.ColorUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public class AnvilGui extends Gui {

	@Getter
	private final Player player;
	private CustomAnvil anvil;
	private String inputText = "";
	private boolean inputTextConfigured;
	@Getter
	private List<String> outputPrompt;
	private Consumer<String> textChangeHandler;

	public AnvilGui(Player player) {
		super(1);
		this.player = Objects.requireNonNull(player, "player");
	}

	public AnvilGui(Player player, Gui parent) {
		super(1, parent);
		this.player = Objects.requireNonNull(player, "player");
	}

	@Override
	public int getSize() {
		return 3;
	}

	@Override
	public AnvilGui setRows(int rows) {
		if (rows != 1) throw new IllegalArgumentException("AnvilGui always has one row");
		return this;
	}

	@Override
	public AnvilGui setTitle(String title) {
		super.setTitle(title);
		return this;
	}

	@Override
	public AnvilGui setParent(Gui parent) {
		super.setParent(parent);
		return this;
	}

	public AnvilGui setInput(ItemStack item) {
		setItem(CustomAnvil.LEFT_INPUT_SLOT, item);
		syncInputName();
		updateOutputPrompt();
		return this;
	}

	public ItemStack getInput() {
		return getItem(CustomAnvil.LEFT_INPUT_SLOT);
	}

	public AnvilGui setRightInput(ItemStack item) {
		setItem(CustomAnvil.RIGHT_INPUT_SLOT, item);
		return this;
	}

	public ItemStack getRightInput() {
		return getItem(CustomAnvil.RIGHT_INPUT_SLOT);
	}

	public AnvilGui setOutput(ItemStack item) {
		setItem(CustomAnvil.RESULT_SLOT, item);
		return this;
	}

	public ItemStack getOutput() {
		return getItem(CustomAnvil.RESULT_SLOT);
	}

	public AnvilGui setAction(Consumer<GuiClickEvent> action) {
		super.setAction(CustomAnvil.RESULT_SLOT, action);
		return this;
	}

	public AnvilGui setAction(ClickType clickType, Consumer<GuiClickEvent> action) {
		super.setAction(CustomAnvil.RESULT_SLOT, clickType, action);
		return this;
	}

	public AnvilGui setInputText(String text) {
		this.inputText = text == null ? "" : text;
		this.inputTextConfigured = true;
		syncInputName();
		updateOutputPrompt();
		if (anvil != null && anvil.isOpen()) anvil.setRenameText(this.inputText);
		return this;
	}

	public AnvilGui setInputText(int value) {
		return setInputText(Integer.toString(value));
	}

	public AnvilGui setInputText(long value) {
		return setInputText(Long.toString(value));
	}

	public String getInputText() {
		return anvil != null && anvil.isOpen() ? anvil.getRenameText() : inputText;
	}

	public OptionalInt getInputInt() {
		String value = getInputText();
		if (value == null) return OptionalInt.empty();
		try {
			return OptionalInt.of(Integer.parseInt(value.trim()));
		} catch (NumberFormatException ignored) {
			return OptionalInt.empty();
		}
	}

	public OptionalLong getInputLong() {
		String value = getInputText();
		if (value == null) return OptionalLong.empty();
		try {
			return OptionalLong.of(Long.parseLong(value.trim()));
		} catch (NumberFormatException ignored) {
			return OptionalLong.empty();
		}
	}

	public AnvilGui setStringAction(Consumer<String> action) {
		Objects.requireNonNull(action, "action");
		return setAction(event -> action.accept(getInputText()));
	}

	public AnvilGui setIntAction(IntConsumer action) {
		Objects.requireNonNull(action, "action");
		return setAction(event -> getInputInt().ifPresent(action));
	}

	public AnvilGui setLongAction(LongConsumer action) {
		Objects.requireNonNull(action, "action");
		return setAction(event -> getInputLong().ifPresent(action));
	}

	public AnvilGui setTextChangeHandler(Consumer<String> handler) {
		this.textChangeHandler = handler;
		return this;
	}

	public AnvilGui setOutputPrompt(String... lines) {
		return setOutputPrompt(lines == null ? null : Arrays.asList(lines));
	}

	public AnvilGui setOutputPrompt(List<String> lines) {
		this.outputPrompt = lines == null
				? null
				: Collections.unmodifiableList(new ArrayList<>(lines));
		updateOutputPrompt();
		return this;
	}

	@Override
	public AnvilGui fillBorder(ItemStack item) {
		fill(item);
		return this;
	}

	@Override
	protected void checkStoredSlot(int slot) {
		if (slot < CustomAnvil.LEFT_INPUT_SLOT || slot > CustomAnvil.RESULT_SLOT) {
			throw new IndexOutOfBoundsException("Anvil slot must be between 0 and 2: " + slot);
		}
	}

	@Override
	protected void updateSlot(int slot) {
		if (slot < CustomAnvil.LEFT_INPUT_SLOT || slot > CustomAnvil.RESULT_SLOT) return;
		ItemStack item = cellItems.get(slot);
		if (item == null && !isUnlocked(slot)) item = defaultItem;
		if (anvil != null && anvil.isOpen()) {
			anvil.setItem(slot, item);
		} else {
			super.updateSlot(slot);
		}
	}

	protected void onTextChange(String text) {
	}

	Inventory openAnvil(GuiManager manager, Player viewer) {
		if (!player.equals(viewer)) {
			throw new IllegalArgumentException("This AnvilGui belongs to " + player.getName());
		}

		inventory = null;
		ensureInputItem();
		syncInputName();
		updateOutputPrompt();

		CustomAnvil created = manager.getPlugin().getNMS().getCustomAnvilFactory().create(player);

		created.setTitle(title).setRenameText(inputText);
		created.setClickHandler((openedAnvil, slot, currentItem) -> false);
		for (int slot = CustomAnvil.LEFT_INPUT_SLOT; slot <= CustomAnvil.RESULT_SLOT; slot++) {
			ItemStack item = cellItems.get(slot);
			if (item == null && !isUnlocked(slot)) item = defaultItem;
			created.setItem(slot, item);
		}
		created.setTextChangeHandler((openedAnvil, text) -> handleTextChange(text));

		this.anvil = created;
		manager.getPlugin().getCustomAnvils().open(created);
		Inventory openedInventory = created.getInventory();
		if (openedInventory == null) {
			throw new IllegalStateException("NMS anvil did not expose its inventory after opening");
		}
		bindInventory(manager, openedInventory);
		updateOutputPrompt();
		update();
		return openedInventory;
	}

	private void handleTextChange(String text) {
		this.inputText = text == null ? "" : text;
		this.inputTextConfigured = true;
		updateOutputPrompt();
		if (textChangeHandler != null) textChangeHandler.accept(this.inputText);
		onTextChange(this.inputText);
	}

	private void ensureInputItem() {
		if (acceptsItems() || cellItems.containsKey(CustomAnvil.LEFT_INPUT_SLOT)
				|| cellItems.containsKey(CustomAnvil.RIGHT_INPUT_SLOT) || defaultItem != null) return;

		ItemStack paper = new ItemStack(Material.PAPER);
		ItemMeta meta = paper.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(" ");
			paper.setItemMeta(meta);
		}
		cellItems.put(CustomAnvil.LEFT_INPUT_SLOT, paper);
	}

	private void syncInputName() {
		if (!inputTextConfigured) return;
		ItemStack input = getInput();
		if (input == null || input.getType() == Material.AIR) return;
		ItemMeta meta = input.getItemMeta();
		if (meta == null) return;
		meta.setDisplayName(inputText.isEmpty() ? " " : inputText);
		input.setItemMeta(meta);
		setItem(CustomAnvil.LEFT_INPUT_SLOT, input);
	}

	private void updateOutputPrompt() {
		if (outputPrompt == null) return;
		ItemStack input = cellItems.get(CustomAnvil.LEFT_INPUT_SLOT);
		if (input == null || input.getType() == Material.AIR) return;

		ItemStack output = input.clone();
		ItemMeta meta = output.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(inputText.isEmpty() ? " " : inputText);
			List<String> lore = new ArrayList<>(outputPrompt.size());
			for (String line : outputPrompt) lore.add(ColorUtils.color(line == null ? "" : "&f" + line));
			meta.setLore(lore);
			output.setItemMeta(meta);
		}
		setItem(CustomAnvil.RESULT_SLOT, output);
	}
}

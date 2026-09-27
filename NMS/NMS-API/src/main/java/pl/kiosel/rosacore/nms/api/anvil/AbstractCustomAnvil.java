package pl.kiosel.rosacore.nms.api.anvil;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class AbstractCustomAnvil implements CustomAnvil {

	private final Player player;
	private final ItemStack[] items = new ItemStack[3];
	private String title = "";
	private String renameText = "";
	private AnvilTextChangeHandler textChangeHandler;
	private AnvilClickHandler clickHandler;
	private Inventory inventory;
	private boolean open;

	protected AbstractCustomAnvil(Player player) {
		if (player == null) throw new NullPointerException("player");
		this.player = player;
	}

	@Override
	public final Player getPlayer() {
		return player;
	}

	@Override
	public final String getTitle() {
		return title;
	}

	@Override
	public final CustomAnvil setTitle(String title) {
		this.title = title == null ? "" : title;
		return this;
	}

	@Override
	public final String getRenameText() {
		return renameText;
	}

	@Override
	public final CustomAnvil setRenameText(String text) {
		this.renameText = text == null ? "" : text;
		if (inventory != null) applyRenameText(renameText);
		return this;
	}

	@Override
	public final ItemStack getItem(int slot) {
		validateSlot(slot);
		ItemStack item = items[slot];
		return item == null ? null : item.clone();
	}

	@Override
	public final CustomAnvil setItem(int slot, ItemStack item) {
		validateSlot(slot);
		items[slot] = item == null ? null : item.clone();
		if (inventory != null) inventory.setItem(slot, items[slot]);
		return this;
	}

	@Override
	public final Inventory getInventory() {
		return inventory;
	}

	@Override
	public final CustomAnvil setTextChangeHandler(AnvilTextChangeHandler handler) {
		this.textChangeHandler = handler;
		return this;
	}

	@Override
	public final CustomAnvil setClickHandler(AnvilClickHandler handler) {
		this.clickHandler = handler;
		return this;
	}

	@Override
	public final boolean handleClick(int slot, ItemStack currentItem) {
		return clickHandler == null || clickHandler.onClick(this, slot, currentItem);
	}

	@Override
	public final void handleClose() {
		cleanup();
	}

	@Override
	public final void open() {
		if (!player.isOnline()) throw new IllegalStateException("Player is offline");
		if (open) return;
		Inventory openedInventory = openContainer();
		if (openedInventory == null) throw new IllegalStateException("NMS anvil did not expose its inventory");
		this.inventory = openedInventory;
		for (int slot = LEFT_INPUT_SLOT; slot <= RESULT_SLOT; slot++) {
			if (items[slot] != null) inventory.setItem(slot, items[slot]);
		}
		applyRenameText(renameText);
		open = true;
	}

	@Override
	public final void close() {
		if (open && player.isOnline()) player.closeInventory();
		cleanup();
	}

	@Override
	public final boolean isOpen() {
		return open;
	}

	protected final void receiveRenameText(String text) {
		renameText = text == null ? "" : text;
		if (textChangeHandler != null) textChangeHandler.onTextChange(this, renameText);
	}

	protected abstract Inventory openContainer();

	protected abstract void applyRenameText(String text);

	protected void releaseContainer() {
	}

	private void cleanup() {
		open = false;
		inventory = null;
		releaseContainer();
	}

	private static void validateSlot(int slot) {
		if (slot < LEFT_INPUT_SLOT || slot > RESULT_SLOT) {
			throw new IllegalArgumentException("Anvil slot must be between 0 and 2: " + slot);
		}
	}
}

package pl.kiosel.rosacore.gui;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

@Getter
public final class GuiClickEvent {

	private final GuiManager manager;
	private final Gui gui;
	private final Player player;
	private final InventoryClickEvent event;
	private final int slot;
	private final int actionSlot;
	private final boolean guiClicked;

	GuiClickEvent(GuiManager manager, Gui gui, Player player, InventoryClickEvent event,
				  int slot, int actionSlot, boolean guiClicked) {
		this.manager = manager;
		this.gui = gui;
		this.player = player;
		this.event = event;
		this.slot = slot;
		this.actionSlot = actionSlot;
		this.guiClicked = guiClicked;
	}

	public ClickType getClickType() {
		return event.getClick();
	}

	public ItemStack getClickedItem() {
		ItemStack item = event.getCurrentItem();
		return item == null ? null : item.clone();
	}

	public ItemStack getCursor() {
		ItemStack item = event.getCursor();
		return item == null ? null : item.clone();
	}
}

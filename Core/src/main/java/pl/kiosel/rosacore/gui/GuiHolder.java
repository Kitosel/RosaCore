package pl.kiosel.rosacore.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class GuiHolder implements InventoryHolder {

	private final GuiManager manager;
	private final Gui gui;

	GuiHolder(GuiManager manager, Gui gui) {
		this.manager = manager;
		this.gui = gui;
	}

	GuiManager getManager() {
		return manager;
	}

	Gui getGui() {
		return gui;
	}

	@Override
	public Inventory getInventory() {
		return gui.getInventoryInternal();
	}
}

package pl.kiosel.rosacore.nms.api.anvil;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface CustomAnvil {

	int LEFT_INPUT_SLOT = 0;
	int RIGHT_INPUT_SLOT = 1;
	int RESULT_SLOT = 2;

	Player getPlayer();

	String getTitle();

	CustomAnvil setTitle(String title);

	String getRenameText();

	CustomAnvil setRenameText(String text);

	ItemStack getItem(int slot);

	CustomAnvil setItem(int slot, ItemStack item);

	Inventory getInventory();

	CustomAnvil setTextChangeHandler(AnvilTextChangeHandler handler);

	CustomAnvil setClickHandler(AnvilClickHandler handler);

	boolean handleClick(int slot, ItemStack currentItem);

	void handleClose();

	void open();

	void close();

	boolean isOpen();
}

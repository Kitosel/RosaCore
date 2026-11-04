package pl.kiosel.rosacore.config.editor;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.compatibility.ZMaterial;
import pl.kiosel.rosacore.gui.PagedGui;
import pl.kiosel.rosacore.input.ChatInput;
import pl.kiosel.rosacore.material.ItemCreator;

import java.util.ArrayList;
import java.util.List;

final class RosaConfigListEditorGui extends PagedGui {

	private final RosaConfigEditorGui parent;
	private final String path;
	private final List<String> values = new ArrayList<>();
	private int previousDisplayCount;

	RosaConfigListEditorGui(RosaConfigEditorGui parent, String path, List<?> initialValues) {
		super(parent);
		this.parent = parent;
		this.path = path;

		for (Object value : initialValues)
			this.values.add(String.valueOf(value));

		setTitle("&1Edit list");

		setUseHeader(true);
		setUseFooter(true);

		setHeaderBackItem(ItemCreator.of(ZMaterial.GRAY_STAINED_GLASS_PANE).makeMenuItem());
		setFooterBackItem(ItemCreator.of(ZMaterial.GRAY_STAINED_GLASS_PANE).makeMenuItem());

		setButton(4, item(ZMaterial.FILLED_MAP, "&e" + path, "&7Right-click an entry to remove it"), event -> {
		});
		setButton(getRows(), 5, item(ZMaterial.CHEST, "&aAdd entry", "&7Add a value to this list"), event -> {
			exit();
			promptAdd(event.getPlayer());
		});
		redraw();
	}

	private void redraw() {
		for (int slot = 9 + values.size(); slot < 9 + previousDisplayCount; slot++) {
			setItem(slot, null);
			clearActions(slot);
		}
		for (int index = 0; index < values.size(); index++) {
			int valueIndex = index;
			setButton(9 + index, item(ZMaterial.PAPER, "&e" + values.get(index), "&cRight-click to remove"),
					ClickType.RIGHT, event -> {
						values.remove(valueIndex);
						parent.saveList(event.getPlayer(), path, new ArrayList<>(values));
						redraw();
					});
		}
		previousDisplayCount = values.size();
		update();
	}

	private void promptAdd(Player player) {
		player.sendMessage(ChatColor.YELLOW + "Enter a value to add to " + path + ". Use /cancel to stop.");
		ChatInput.showPrompt(parent.plugin(), player, response -> parent.plugin().getRosaScheduler().runGlobal(() -> {
			values.add(response.getMessage().trim());
			parent.saveList(player, path, new ArrayList<>(values));
			parent.plugin().getGuiManager().openGUI(player, this);
		})).onClose(() -> reopen(player)).onCancel(() -> reopen(player));
	}

	private void reopen(Player player) {
		parent.plugin().getRosaScheduler().runGlobal(() -> parent.plugin().getGuiManager().openGUI(player, this));
	}

	private ItemStack item(ZMaterial material, String name, String lore) {
		return ItemCreator.of(material).name(name).lore(lore).makeMenuItem();
	}
}

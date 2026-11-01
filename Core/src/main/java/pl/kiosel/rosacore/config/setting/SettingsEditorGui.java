package pl.kiosel.rosacore.config.setting;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.compatibility.ZMaterial;
import pl.kiosel.rosacore.config.ConfigSaveResult;
import pl.kiosel.rosacore.gui.PagedGui;
import pl.kiosel.rosacore.input.ChatInput;
import pl.kiosel.rosacore.material.ItemCreator;

import java.util.ArrayList;
import java.util.List;

public final class SettingsEditorGui extends PagedGui {

	private final RosaPlugin plugin;
	private final RosaSettings settings;

	public SettingsEditorGui(RosaPlugin plugin, RosaSettings settings) {
		this.plugin = plugin;
		this.settings = settings;

		if (!settings.isLoaded())
			throw new IllegalStateException("Settings must be loaded before opening the editor");

		setTitle("&1Settings: " + settings.getConfig().getFile().getName());

		setUseHeader(true);
		setUseFooter(true);

		setHeaderBackItem(ItemCreator.of(ZMaterial.GRAY_STAINED_GLASS_PANE).makeMenuItem());
		setFooterBackItem(ItemCreator.of(ZMaterial.GRAY_STAINED_GLASS_PANE).makeMenuItem());
		setButton(4, item(ZMaterial.BOOK, "&e" + settings.getConfig().getFile().getName(),
				"&7Click a value to edit it"), event -> {
		});
		setButton(8, item(ZMaterial.OAK_DOOR, "&cClose", "&7Close the editor"), event -> event.getPlayer().closeInventory());
		redraw();
	}

	private void redraw() {
		for (int index = 0; index < settings.getSchema().getSettings().size(); index++) {
			SettingKey<?> key = settings.getSchema().getSettings().get(index);
			int slot = 9 + index;
			if (!key.isEditable() || !settings.getConfig().isEditable()) {
				setItem(slot, settingItem(key, false));
			} else {
				setButton(slot, settingItem(key, true), event -> edit(event.getPlayer(), key));
			}
		}
		update();
	}

	private void edit(Player player, SettingKey<?> key) {
		if (key.get() instanceof Boolean) {
			@SuppressWarnings("unchecked")
			SettingKey<Boolean> booleanKey = (SettingKey<Boolean>) key;
			ConfigSaveResult result = settings.set(booleanKey, !booleanKey.getBoolean());
			if (!result.isSuccess()) {
				player.sendMessage(ChatColor.RED + "Could not save setting " + key.getPath());
				return;
			}
			redraw();
			return;
		}

		player.sendMessage(ChatColor.YELLOW + "Enter a new value for " + key.getPath() + ". Use /cancel to stop.");
		ChatInput.showPrompt(plugin, player, response ->
				plugin.getRosaScheduler().runGlobal(() -> {
					try {
						ConfigSaveResult result = settings.setInput(key, response.getMessage().trim());
						if (!result.isSuccess()) {
							player.sendMessage(ChatColor.RED + "Could not save setting " + key.getPath());
						} else {
							player.sendMessage(ChatColor.GREEN + "Updated " + key.getPath());
							redraw();
						}
					} catch (SettingValueException | IllegalArgumentException exception) {
						player.sendMessage(ChatColor.RED + "Invalid value: " + exception.getMessage());
					}
					plugin.getGuiManager().openGUI(player, this);
				})).onCancel(() -> plugin.getRosaScheduler().runGlobal(() ->
				plugin.getGuiManager().openGUI(player, this)));
	}

	private ItemStack settingItem(SettingKey<?> key, boolean editable) {
		Object value = key.isSensitive() ? "<hidden>" : key.get();
		List<String> lore = new ArrayList<>();
		lore.add("&7Value: &f" + value);
		lore.add("&7Type: &f" + key.getExpectedValueDescription());

		if (key.isSensitive())
			lore.add("&cSensitive setting");

		for (String line : key.getComments())
			lore.add("&8" + line);
		for (String line : key.getDescription())
			lore.add("&7" + line);
		lore.add(editable ? "&eClick to edit" : "&cRead only");

		ZMaterial material = value instanceof Boolean
				? ((Boolean) value ? ZMaterial.EMERALD_BLOCK : ZMaterial.REDSTONE_BLOCK)
				: ZMaterial.PAPER;
		return ItemCreator.of(material).name("&e" + key.getPath()).lore(lore).makeMenuItem();
	}

	private ItemStack item(ZMaterial material, String name, String lore) {
		return ItemCreator.of(material).name(name).lore(lore).makeMenuItem();
	}
}

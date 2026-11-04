package pl.kiosel.rosacore.config.editor;

import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.compatibility.ZMaterial;
import pl.kiosel.rosacore.config.RosaConfig;
import pl.kiosel.rosacore.gui.PagedGui;
import pl.kiosel.rosacore.material.ItemCreator;
import pl.kiosel.rosacore.utils.ItemUtils;

import java.util.List;
import java.util.stream.Collectors;

public final class RosaConfigListGui extends PagedGui {

	public RosaConfigListGui(RosaPlugin plugin) {
		List<RosaConfig> editableConfigs = plugin.getRosaConfigs().stream()
				.filter(RosaConfig::isEditable)
				.collect(Collectors.toList());

		int rows = calculateRows(editableConfigs.size());
		setRows(rows);

		setTitle("&1Plugin configurations");
		setUseHeader(true);
		setUseFooter(true);

		setHeaderBackItem(ItemCreator.of(ZMaterial.GRAY_STAINED_GLASS_PANE).makeMenuItem());
		setFooterBackItem(ItemCreator.of(ZMaterial.GRAY_STAINED_GLASS_PANE).makeMenuItem());

		setButton(4, ItemUtils.create(ZMaterial.BOOK, "&eEditable configurations", "&7Select a configuration to edit"), event -> {
		});
		setButton(getRows(), 8, ItemUtils.create(ZMaterial.OAK_DOOR, "&cClose", "&7Close the editor"),
				event -> event.getPlayer().closeInventory());

		if (editableConfigs.isEmpty()) {
			setItem(22, ItemUtils.create(ZMaterial.RED_DYE, "&cNo configuration file found"));
			return;
		}

		int slot = 9;
		for (RosaConfig config : editableConfigs) {
			if (slot >= (rows - 1) * 9) {
				break;
			}

			String relativePath = plugin.getDataFolder().toPath().toAbsolutePath().normalize()
					.relativize(config.getPath().toAbsolutePath().normalize()).toString().replace('\\', '/');

			setButton(slot++, ItemUtils.create(ZMaterial.WRITABLE_BOOK, "&e" + relativePath, "&7Click to edit this configuration"), event ->
					event.getManager().showGUI(event.getPlayer(), new RosaConfigEditorGui(plugin, config, this))
			);
		}
	}

	private int calculateRows(int editableConfigsCount) {
		return Math.min((int) Math.ceil((double) editableConfigsCount / 9) + 2, 6);
	}
}
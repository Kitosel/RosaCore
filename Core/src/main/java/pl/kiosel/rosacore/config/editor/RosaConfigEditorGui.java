package pl.kiosel.rosacore.config.editor;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.compatibility.ZMaterial;
import pl.kiosel.rosacore.config.ConfigSaveResult;
import pl.kiosel.rosacore.config.RosaConfig;
import pl.kiosel.rosacore.gui.PagedGui;
import pl.kiosel.rosacore.input.ChatInput;
import pl.kiosel.rosacore.material.ItemCreator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RosaConfigEditorGui extends PagedGui {

	private final RosaPlugin plugin;
	private final RosaConfig config;
	private final String sectionPath;

	public RosaConfigEditorGui(RosaPlugin plugin, RosaConfig config) {
		this(plugin, config, "", null);
	}

	RosaConfigEditorGui(RosaPlugin plugin, RosaConfig config, PagedGui parent) {
		this(plugin, config, "", parent);
	}

	private RosaConfigEditorGui(RosaPlugin plugin, RosaConfig config, String sectionPath, PagedGui parent) {
		super(parent);

		this.plugin = plugin;
		this.config = Objects.requireNonNull(config, "config");

		this.sectionPath = sectionPath;
		if (!config.isLoaded())
			throw new IllegalStateException("Configuration has not been loaded: " + config.getPath());
		if (!config.isEditable())
			throw new IllegalArgumentException("Configuration is read-only: " + config.getPath());

		setTitle("&1" + config.getFile().getName());
		setUseHeader(true);
		setUseFooter(true);

		setHeaderBackItem(ItemCreator.of(ZMaterial.GRAY_STAINED_GLASS_PANE).makeMenuItem());
		setFooterBackItem(ItemCreator.of(ZMaterial.GRAY_STAINED_GLASS_PANE).makeMenuItem());

		String header = sectionPath.isEmpty() ? config.getFile().getName() : sectionPath;
		setButton(4, item(ZMaterial.FILLED_MAP, "&e" + header, "&7Click a value to edit it"), event -> {
		});
		setButton(getRows(), 5, item(ZMaterial.OAK_DOOR, "&cClose", "&7Close the editor"), event -> event.getPlayer().closeInventory());
		redraw();
	}

	private void redraw() {
		int slot = 9;
		for (String key : config.getKeys(sectionPath, false)) {
			String path = fullPath(key);
			Object value = config.get(path);
			if (config.getConfigurationSection(path) != null) {
				setButton(slot++, item(ZMaterial.WRITABLE_BOOK, "&e" + key,
								documentation(path, "&7Click to open this section")),
						event -> event.getManager().showGUI(event.getPlayer(),
								new RosaConfigEditorGui(plugin, config, path, this)));
			} else if (value instanceof Boolean) {
				ZMaterial icon = (Boolean) value ? ZMaterial.EMERALD_BLOCK : ZMaterial.REDSTONE_BLOCK;
				setButton(slot++, item(icon, "&e" + key,
								documentation(path, "&7Value: &f" + value + "\n&7Click to toggle")),
						event -> updateValue(event.getPlayer(), path, !(Boolean) config.get(path)));
			} else if (value instanceof Number) {
				setButton(slot++, valueItem(ZMaterial.CLOCK, key, path, value),
						event -> promptValue(event.getPlayer(), path, value));
			} else if (value instanceof String) {
				Optional<ZMaterial> material = ZMaterial.match((String) value);
				setButton(slot++, valueItem(material.isPresent() ? material.get() : ZMaterial.PAPER, key, path, value),
						event -> {
							if (material.isPresent() && material.get().getMaterial().isPresent()) {
								event.getManager().showGUI(event.getPlayer(), new RosaMaterialPickerGui(this, path));
							} else promptValue(event.getPlayer(), path, value);
						});
			} else if (value instanceof List<?>) {
				setButton(slot++, valueItem(ZMaterial.WRITABLE_BOOK, key, path,
								"(" + ((List<?>) value).size() + " values)"),
						event -> event.getManager().showGUI(event.getPlayer(),
								new RosaConfigListEditorGui(this, path, (List<?>) value)));
			} else {
				setItem(slot++, valueItem(ZMaterial.PAPER, key, path, value));
			}
		}
		update();
	}

	RosaPlugin plugin() {
		return plugin;
	}

	private ItemStack valueItem(ZMaterial material, String key, String path, Object value) {
		return item(material, "&e" + key, documentation(path, "&7Value: &f" + value + "\n&7Click to edit"));
	}

	private List<String> documentation(String path, String fallback) {
		List<String> lore = new ArrayList<>();
		lore.add(fallback);
		for (String line : config.getSettingDocumentation(path)) lore.add("&8" + line);
		return lore;
	}

	private String fullPath(String key) {
		return sectionPath.isEmpty() ? key : sectionPath + "." + key;
	}

	private void promptValue(Player player, String path, Object oldValue) {
		player.sendMessage(ChatColor.YELLOW + "Enter a new value for " + path + ". Use /cancel to stop.");
		ChatInput.showPrompt(plugin, player, response -> plugin.getRosaScheduler().runGlobal(() -> {
			try {
				updateValue(player, path, parseValue(oldValue, response.getMessage().trim()));
			} catch (IllegalArgumentException exception) {
				player.sendMessage(ChatColor.RED + "Invalid value: " + exception.getMessage());
				plugin.getGuiManager().openGUI(player, this);
			}
		})).onCancel(() -> plugin.getRosaScheduler().runGlobal(() -> plugin.getGuiManager().openGUI(player, this)));
	}

	private static Object parseValue(Object previous, String input) {
		if (previous instanceof Integer) return Integer.valueOf(input);
		if (previous instanceof Long) return Long.valueOf(input);
		if (previous instanceof Double) return Double.valueOf(input);
		if (previous instanceof Float) return Float.valueOf(input);
		if (previous instanceof Short) return Short.valueOf(input);
		if (previous instanceof Byte) return Byte.valueOf(input);
		return input;
	}

	void updateValue(Player player, String path, Object value) {
		Object previous = config.get(path);
		config.set(path, value);
		ConfigSaveResult result = config.save();
		if (!result.isSuccess()) {
			config.set(path, previous);
			String problem = result.getProblems().isEmpty() ? "unknown validation or file error"
					: result.getProblems().get(0).toString();
			player.sendMessage(ChatColor.RED + "Could not save configuration: " + problem);
			return;
		}
		try {
			plugin.reloadRosaConfig();
			player.sendMessage(ChatColor.GREEN + "Saved " + path);
			redraw();
		} catch (RuntimeException exception) {
			player.sendMessage(ChatColor.RED + "Saved the file, but reload failed: " + exception.getMessage());
		}
	}

	void saveList(Player player, String path, List<String> values) {
		updateValue(player, path, values);
	}

	private static ItemStack item(ZMaterial material, String name, String lore) {
		return ItemCreator.of(material).name(name).lore(lore).makeMenuItem();
	}

	private static ItemStack item(ZMaterial material, String name, List<String> lore) {
		return ItemCreator.of(material).name(name).lore(lore).makeMenuItem();
	}

	private final class RosaMaterialPickerGui extends PagedGui {
		private final String path;

		private RosaMaterialPickerGui(PagedGui parent, String path) {
			super(parent);
			this.path = path;

			setTitle("&1Select material");

			setUseHeader(true);
			setUseFooter(true);

			setHeaderBackItem(ItemCreator.of(ZMaterial.GRAY_STAINED_GLASS_PANE).makeMenuItem());
			setFooterBackItem(ItemCreator.of(ZMaterial.GRAY_STAINED_GLASS_PANE).makeMenuItem());

			setButton(4, item(ZMaterial.FILLED_MAP, "&e" + path, "&7Choose a material"), event -> {
			});
			int slot = 9;
			for (ZMaterial candidate : ZMaterial.values()) {
				if (!candidate.getMaterial().isPresent()) continue;
				try {
					setButton(slot++, ItemCreator.of(candidate).name("&e" + candidate.name()).makeMenuItem(), event -> {
						updateValue(event.getPlayer(), this.path, candidate.name());
						event.getManager().showGUI(event.getPlayer(), RosaConfigEditorGui.this);
					});
				} catch (IllegalArgumentException ignored) {
				}
			}
		}
	}
}

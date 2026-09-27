package pl.kiosel.rosacore.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.compatibility.RosaSound;
import pl.kiosel.rosacore.compatibility.ZSound;
import pl.kiosel.rosacore.utils.ColorUtils;
import pl.kiosel.rosacore.version.MinecraftVersion;

import java.util.*;
import java.util.function.Consumer;

public abstract class Gui {

	protected Inventory inventory;
	protected String title = "";
	protected int rows = 3;
	protected final Map<Integer, ItemStack> cellItems = new LinkedHashMap<>();
	protected final Map<Integer, Map<ClickType, Consumer<GuiClickEvent>>> actions = new HashMap<>();
	protected final Map<Integer, Map<ClickType, RosaSound.SoundHolder>> clickSounds = new HashMap<>();
	protected final Map<Integer, Boolean> unlockedCells = new HashMap<>();
	protected ItemStack defaultItem;

	private Gui parent;
	private GuiManager manager;
	private Consumer<GuiClickEvent> defaultAction;
	private boolean acceptsItems;
	private boolean allowDropItems = true;
	private boolean allowClose = true;
	private boolean playDefaultSound = false;
	private RosaSound.SoundHolder defaultSound = new RosaSound.SoundHolder(ZSound.UI_BUTTON_CLICK, 1f, 1f);
	private boolean handlingTopClick;
	private boolean customSoundPlayed;

	protected Gui() {
	}

	protected Gui(int rows) {
		this.rows = validateRows(rows);
	}

	protected Gui(Gui parent) {
		this.parent = parent;
	}

	protected Gui(int rows, Gui parent) {
		this.rows = validateRows(rows);
		this.parent = parent;
	}

	public final String getTitle() {
		return title;
	}

	public Gui setTitle(String title) {
		String changed = title == null ? "" : title;
		if (!changed.equals(this.title)) {
			this.title = changed;
			invalidateInventory();
		}
		return this;
	}

	public final int getRows() {
		return rows;
	}

	public Gui setRows(int rows) {
		int changed = validateRows(rows);
		if (changed != this.rows) {
			this.rows = changed;
			invalidateInventory();
		}
		return this;
	}

	public Gui playSoundOnClick(boolean playDefaultSound) {
		this.playDefaultSound = playDefaultSound;
		return this;
	}

	public int getSize() {
		return rows * 9;
	}

	public final Gui getParent() {
		return parent;
	}

	public Gui setParent(Gui parent) {
		this.parent = parent;
		return this;
	}

	public final boolean acceptsItems() {
		return acceptsItems;
	}

	public Gui setAcceptsItems(boolean acceptsItems) {
		this.acceptsItems = acceptsItems;
		return this;
	}

	public final boolean allowsDropItems() {
		return allowDropItems;
	}

	public Gui setAllowDropItems(boolean allowDropItems) {
		this.allowDropItems = allowDropItems;
		return this;
	}

	public final boolean allowsClose() {
		return allowClose;
	}

	public Gui setAllowClose(boolean allowClose) {
		this.allowClose = allowClose;
		return this;
	}

	public final ZSound getDefaultSound() {
		return defaultSound == null ? null : defaultSound.getSound();
	}

	public final RosaSound.SoundHolder getDefaultSoundHolder() {
		return defaultSound;
	}

	public Gui setDefaultSound(ZSound sound) {
		this.defaultSound = soundHolder(sound);
		return this;
	}

	public Gui setDefaultSound(RosaSound.SoundHolder sound) {
		this.defaultSound = sound;
		return this;
	}

	protected final void markCustomSoundPlayed() {
		if (handlingTopClick) customSoundPlayed = true;
	}

	public Gui setDefaultItem(ItemStack item) {
		this.defaultItem = cloneOrNull(item);
		update();
		return this;
	}

	public ItemStack getDefaultItem() {
		return cloneOrNull(defaultItem);
	}

	public Gui setItem(int slot, ItemStack item) {
		checkStoredSlot(slot);
		if (item == null || item.getType() == Material.AIR) {
			cellItems.remove(slot);
		} else {
			cellItems.put(slot, item.clone());
		}
		onContentChanged(slot);
		return this;
	}

	public Gui setItem(int row, int column, ItemStack item) {
		return setItem(slot(row, column), item);
	}

	public ItemStack getItem(int slot) {
		checkStoredSlot(slot);
		return cloneOrNull(cellItems.get(slot));
	}

	public ItemStack getItem(int row, int column) {
		return getItem(slot(row, column));
	}

	public Gui setButton(int slot, ItemStack item, Consumer<GuiClickEvent> action) {
		setItem(slot, item);
		setAction(slot, null, action);
		clearClickSoundInternal(slot, null);
		return this;
	}

	public Gui setButton(int slot, ItemStack item, ZSound sound, Consumer<GuiClickEvent> action) {
		return setButton(slot, item, soundHolder(sound), action);
	}

	public Gui setButton(int slot, ItemStack item, RosaSound.SoundHolder sound, Consumer<GuiClickEvent> action) {
		setItem(slot, item);
		setAction(slot, null, action);
		setClickSoundInternal(slot, null, sound);
		return this;
	}

	public Gui setButton(int row, int column, ItemStack item, Consumer<GuiClickEvent> action) {
		return setButton(slot(row, column), item, action);
	}

	public Gui setButton(int row, int column, ItemStack item, ZSound sound, Consumer<GuiClickEvent> action) {
		return setButton(slot(row, column), item, sound, action);
	}

	public Gui setButton(int row, int column, ItemStack item, RosaSound.SoundHolder sound,
						 Consumer<GuiClickEvent> action) {
		return setButton(slot(row, column), item, sound, action);
	}

	public Gui setButton(int slot, ItemStack item, ClickType clickType, Consumer<GuiClickEvent> action) {
		setItem(slot, item);
		setAction(slot, clickType, action);
		clearClickSoundInternal(slot, clickType);
		return this;
	}

	public Gui setButton(int slot, ItemStack item, ClickType clickType, RosaSound.SoundHolder sound, Consumer<GuiClickEvent> action) {
		setItem(slot, item);
		setAction(slot, clickType, action);
		setClickSoundInternal(slot, clickType, sound);
		return this;
	}

	public Gui setButton(int slot, ItemStack item, ClickType clickType, ZSound sound, Consumer<GuiClickEvent> action) {
		return setButton(slot, item, clickType, soundHolder(sound), action);
	}

	public Gui setButton(int row, int column, ItemStack item, ClickType clickType, Consumer<GuiClickEvent> action) {
		return setButton(slot(row, column), item, clickType, action);
	}

	public Gui setButton(int row, int column, ItemStack item, ClickType clickType, ZSound sound, Consumer<GuiClickEvent> action) {
		return setButton(slot(row, column), item, clickType, sound, action);
	}

	public Gui setButton(int row, int column, ItemStack item, ClickType clickType,
						 RosaSound.SoundHolder sound, Consumer<GuiClickEvent> action) {
		return setButton(slot(row, column), item, clickType, sound, action);
	}

	public Gui setButton(int row, int column, ItemStack item, ZSound sound, ClickType clickType, Consumer<GuiClickEvent> action) {
		return setButton(row, column, item, clickType, sound, action);
	}

	public Gui setButton(int row, int column, ItemStack item, RosaSound.SoundHolder sound,
						 ClickType clickType, Consumer<GuiClickEvent> action) {
		return setButton(row, column, item, clickType, sound, action);
	}

	public Gui setAction(int slot, Consumer<GuiClickEvent> action) {
		return setAction(slot, null, action);
	}

	public Gui setAction(int row, int column, Consumer<GuiClickEvent> action) {
		return setAction(slot(row, column), null, action);
	}

	public Gui setAction(int slot, ClickType clickType, Consumer<GuiClickEvent> action) {
		checkStoredSlot(slot);
		setActionInternal(slot, clickType, action);
		return this;
	}

	public Gui setAction(int row, int column, ClickType clickType, Consumer<GuiClickEvent> action) {
		return setAction(slot(row, column), clickType, action);
	}

	public Gui setDefaultAction(Consumer<GuiClickEvent> action) {
		this.defaultAction = action;
		return this;
	}

	public Gui clearActions(int slot) {
		actions.remove(slot);
		clickSounds.remove(slot);
		return this;
	}

	public Gui clearActions(int row, int column) {
		return clearActions(slot(row, column));
	}

	public Gui setUnlocked(int slot, boolean unlocked) {
		checkPhysicalSlot(slot);
		unlockedCells.put(slot, unlocked);
		updateSlot(slot);
		return this;
	}

	public Gui setUnlocked(int slot) {
		return setUnlocked(slot, true);
	}

	public Gui setUnlocked(int row, int column, boolean unlocked) {
		return setUnlocked(slot(row, column), unlocked);
	}

	public Gui setUnlocked(int row, int column) {
		return setUnlocked(row, column, true);
	}

	public Gui setUnlockedRange(int firstSlot, int lastSlot, boolean unlocked) {
		if (firstSlot > lastSlot) throw new IllegalArgumentException("firstSlot cannot exceed lastSlot");
		for (int slot = firstSlot; slot <= lastSlot; slot++) setUnlocked(slot, unlocked);
		return this;
	}

	public Gui setUnlockedRange(int firstSlot, int lastSlot) {
		return setUnlockedRange(firstSlot, lastSlot, true);
	}

	public final boolean isUnlocked(int slot) {
		return unlockedCells.getOrDefault(slot, false);
	}

	public Gui fill(ItemStack item) {
		for (int slot = 0; slot < getSize(); slot++) setItem(slot, item);
		return this;
	}

	public Gui fillEmpty(ItemStack item) {
		for (int slot = 0; slot < getSize(); slot++) {
			if (!cellItems.containsKey(slot)) setItem(slot, item);
		}
		return this;
	}

	public Gui fillBorder(ItemStack item) {
		for (int row = 1; row <= rows; row++) {
			for (int column = 1; column <= 9; column++) {
				if (row == 1 || row == rows || column == 1 || column == 9) {
					setItem(row, column, item);
				}
			}
		}
		return this;
	}

	public final List<Player> getPlayers() {
		if (inventory == null) return Collections.emptyList();
		List<Player> players = new ArrayList<>();
		inventory.getViewers().forEach(viewer -> {
			if (viewer instanceof Player) players.add((Player) viewer);
		});
		return Collections.unmodifiableList(players);
	}

	public final boolean isOpen() {
		return inventory != null && !inventory.getViewers().isEmpty();
	}

	public final void show(GuiManager manager, Player player) {
		Objects.requireNonNull(manager, "manager").openGUI(player, this);
	}

	public final void close() {
		if (manager != null) manager.closeGui(this, false);
	}

	public final void exit() {
		if (manager != null) manager.closeGui(this, true);
	}

	public void reset() {
		cellItems.clear();
		actions.clear();
		clickSounds.clear();
		update();
	}

	public void update() {
		if (inventory == null) return;
		for (int slot = 0; slot < inventory.getSize(); slot++) updateSlot(slot);
	}

	protected void onOpen(GuiManager manager, Player player) {
	}

	protected void onClose(GuiManager manager, Player player) {
	}

	protected boolean onClick(GuiClickEvent event) {
		return false;
	}

	protected boolean onClickPlayerInventory(GuiClickEvent event) {
		return false;
	}

	protected boolean onClickOutside(GuiClickEvent event) {
		return allowDropItems;
	}

	protected void onDrag(GuiManager manager, Player player, InventoryDragEvent event) {
	}

	protected int resolveActionSlot(int physicalSlot) {
		return physicalSlot;
	}

	protected void onContentChanged(int storedSlot) {
		if (inventory != null && storedSlot >= 0 && storedSlot < inventory.getSize()) {
			updateSlot(storedSlot);
		}
	}

	protected final void setActionInternal(int slot, ClickType clickType,
										   Consumer<GuiClickEvent> action) {
		Map<ClickType, Consumer<GuiClickEvent>> byClick =
				actions.computeIfAbsent(slot, ignored -> new HashMap<>());
		if (action == null) {
			byClick.remove(clickType);
			if (byClick.isEmpty()) actions.remove(slot);
		} else {
			byClick.put(clickType, action);
		}
	}

	protected final void setClickSoundInternal(int slot, ClickType clickType, RosaSound.SoundHolder sound) {
		clickSounds.computeIfAbsent(slot, ignored -> new HashMap<>()).put(clickType, sound);
	}

	protected static RosaSound.SoundHolder soundHolder(ZSound sound) {
		return sound == null ? null : new RosaSound.SoundHolder(sound, 1f, 1f);
	}

	protected final void clearClickSoundInternal(int slot, ClickType clickType) {
		Map<ClickType, RosaSound.SoundHolder> byClick = clickSounds.get(slot);
		if (byClick == null) return;
		byClick.remove(clickType);
		if (byClick.isEmpty()) clickSounds.remove(slot);
	}

	protected void checkStoredSlot(int slot) {
		checkPhysicalSlot(slot);
	}

	protected final Inventory getInventoryInternal() {
		return inventory;
	}

	protected final synchronized Inventory getOrCreateInventory(GuiManager manager) {
		if (this.manager != null && this.manager != manager) {
			throw new IllegalStateException("One Gui instance cannot be shared by different GuiManagers");
		}
		this.manager = manager;
		if (inventory == null) {
			inventory = Bukkit.createInventory(new GuiHolder(manager, this), getSize(), inventoryTitle(manager));
			update();
		}
		return inventory;
	}

	protected final synchronized void bindInventory(GuiManager manager, Inventory inventory) {
		if (this.manager != null && this.manager != manager) {
			throw new IllegalStateException("One Gui instance cannot be shared by different GuiManagers");
		}
		this.manager = Objects.requireNonNull(manager, "manager");
		this.inventory = Objects.requireNonNull(inventory, "inventory");
	}

	protected final boolean handleTopClick(GuiManager manager, Player player, InventoryClickEvent event) {
		boolean previousHandling = handlingTopClick;
		boolean previousCustomSound = customSoundPlayed;
		handlingTopClick = true;
		customSoundPlayed = false;
		try {
			int physicalSlot = event.getRawSlot();
			int actionSlot = resolveActionSlot(physicalSlot);
			GuiClickEvent click = new GuiClickEvent(manager, this, player, event,
					physicalSlot, actionSlot, true);
			Consumer<GuiClickEvent> action = findAction(actionSlot, event.getClick());
			if (action != null) {
				action.accept(click);
				playClickSoundIfNeeded(player, actionSlot, event.getClick());
				return true;
			}
			if (defaultAction != null) {
				defaultAction.accept(click);
				playClickSoundIfNeeded(player, actionSlot, event.getClick());
				return true;
			}
			boolean handled = onClick(click);
			if (handled) playClickSoundIfNeeded(player, actionSlot, event.getClick());
			return handled;
		} finally {
			handlingTopClick = previousHandling;
			customSoundPlayed = previousCustomSound;
		}
	}

	protected final boolean handlePlayerClick(GuiManager manager, Player player, InventoryClickEvent event) {
		GuiClickEvent click = new GuiClickEvent(manager, this, player, event,
				event.getRawSlot(), event.getRawSlot(), false);
		return onClickPlayerInventory(click);
	}

	protected final boolean handleOutsideClick(GuiManager manager, Player player, InventoryClickEvent event) {
		GuiClickEvent click = new GuiClickEvent(manager, this, player, event, -1, -1, false);
		return onClickOutside(click);
	}

	protected final void handleOpen(GuiManager manager, Player player) {
		onOpen(manager, player);
	}

	protected final void handleClose(GuiManager manager, Player player) {
		onClose(manager, player);
	}

	protected final void handleDrag(GuiManager manager, Player player, InventoryDragEvent event) {
		onDrag(manager, player, event);
	}

	protected final void playDefaultSound(Player player) {
		if (playDefaultSound) playSound(player, defaultSound);
	}

	protected final RosaSound.SoundHolder resolveClickSound(int slot, ClickType clickType) {
		Map<ClickType, RosaSound.SoundHolder> byClick = clickSounds.get(slot);
		if (byClick == null) return playDefaultSound ? defaultSound : null;
		if (byClick.containsKey(clickType)) return byClick.get(clickType);
		if (byClick.containsKey(null)) return byClick.get(null);
		return playDefaultSound ? defaultSound : null;
	}

	private void playClickSound(Player player, int slot, ClickType clickType) {
		playSound(player, resolveClickSound(slot, clickType));
	}

	private static void playSound(Player player, RosaSound.SoundHolder sound) {
		if (sound == null) return;
		sound.getSound().play(player, sound.getVolume(), sound.getPitch());
	}

	private void playClickSoundIfNeeded(Player player, int slot, ClickType clickType) {
		if (!customSoundPlayed) playClickSound(player, slot, clickType);
	}

	private Consumer<GuiClickEvent> findAction(int slot, ClickType clickType) {
		Map<ClickType, Consumer<GuiClickEvent>> byClick = actions.get(slot);
		if (byClick == null) return null;
		Consumer<GuiClickEvent> exact = byClick.get(clickType);
		return exact != null ? exact : byClick.get(null);
	}

	protected void updateSlot(int slot) {
		if (inventory == null || slot < 0 || slot >= inventory.getSize()) return;
		ItemStack item = cellItems.get(slot);
		if (item == null && !isUnlocked(slot)) item = defaultItem;
		inventory.setItem(slot, cloneOrNull(item));
	}

	protected final void invalidateInventory() {
		if (inventory == null) return;
		List<Player> viewers = new ArrayList<>(getPlayers());
		inventory = null;
		if (manager != null) manager.refreshGui(this, viewers);
	}

	private String inventoryTitle(GuiManager manager) {
		String colored = ColorUtils.color(title == null ? "" : title);
		if (manager.getPlugin().getMinecraftVersion().isOlderThan(MinecraftVersion.of(1, 9, 0))
				&& colored.length() > 32) {
			int end = colored.charAt(31) == '\u00a7' ? 31 : 32;
			return colored.substring(0, end);
		}
		return colored;
	}

	private int slot(int row, int column) {
		if (row < 1 || row > rows) {
			throw new IndexOutOfBoundsException("Row " + row + " outside GUI rows 1-" + rows);
		}
		if (column < 1 || column > 9) {
			throw new IndexOutOfBoundsException("Column " + column + " outside GUI columns 1-9");
		}
		return (row - 1) * 9 + (column - 1);
	}

	private void checkPhysicalSlot(int slot) {
		if (slot < 0 || slot >= getSize()) {
			throw new IndexOutOfBoundsException("Slot " + slot + " outside GUI size " + getSize());
		}
	}

	private static int validateRows(int rows) {
		if (rows < 1 || rows > 6) throw new IllegalArgumentException("GUI rows must be between 1 and 6");
		return rows;
	}

	private static ItemStack cloneOrNull(ItemStack item) {
		return item == null ? null : item.clone();
	}
}

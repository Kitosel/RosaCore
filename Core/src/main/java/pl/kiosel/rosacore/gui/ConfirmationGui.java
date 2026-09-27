package pl.kiosel.rosacore.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.compatibility.RosaSound;
import pl.kiosel.rosacore.compatibility.ZMaterial;
import pl.kiosel.rosacore.compatibility.ZSound;
import pl.kiosel.rosacore.material.ItemCreator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ConfirmationGui extends Gui {

	private ItemStack confirmItem;
	private ItemStack cancelItem;
	private ItemStack informationItem;
	private ItemStack emptyItem;

	private int confirmRow = 1;
	private int cancelRow = 1;
	private int infoRow = 1;

	private int confirmColumn = 4;
	private int cancelColumn = 5;
	private int infoColumn = 6;

	private boolean closeOnClick = false;
	private boolean fillEmpty = false;
	private boolean showConfirm = true;
	private boolean showCancel = true;
	private boolean showInformation = true;

	private RosaSound.SoundHolder confirmSound;
	private RosaSound.SoundHolder cancelSound;
	private RosaSound.SoundHolder closeSound;

	private boolean playSound;
	private final Set<Integer> managedSlots = new LinkedHashSet<>();
	private final Set<UUID> resolvedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private Consumer<GuiClickEvent> confirmEvent = event -> {
	};
	private Consumer<GuiClickEvent> cancelEvent = event -> {
	};
	private Consumer<Player> dismissEvent = player -> {
	};

	public ConfirmationGui() {
		this.confirmSound = new RosaSound.SoundHolder(ZSound.UI_BUTTON_CLICK, 0.5f, 1.0f);
		this.cancelSound = new RosaSound.SoundHolder(ZSound.UI_BUTTON_CLICK, 0.5f, 1.0f);
		this.closeSound = new RosaSound.SoundHolder(ZSound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);

		this.emptyItem = ItemCreator.of(ZMaterial.WHITE_STAINED_GLASS_PANE).name("&7&kBLANK").makeMenuItem();
		title("Confirmation").rows(3).defaultItems().centerItems().closeOnClick(true);
	}

	public ConfirmationGui title(String title) {
		setTitle(title);
		return this;
	}

	public ConfirmationGui rows(int rows) {
		boolean centered = this.confirmRow == this.cancelRow
				&& this.confirmRow == this.infoRow
				&& this.confirmRow == (this.rows + 1) / 2;
		setRows(rows);
		if (centered) centerItems();
		return this;
	}

	public ConfirmationGui setAllInRow(int row) {
		if (row < 1 || row > 6) throw new IllegalArgumentException("row must be between 1 and 6");
		if (row > this.rows) throw new IllegalArgumentException("row cannot exceed GUI row count " + this.rows);
		this.confirmRow = row;
		this.cancelRow = row;
		this.infoRow = row;
		return this;
	}

	public ConfirmationGui centerItems() {
		setAllInRow((this.rows + 1) / 2);
		return this;
	}

	public ConfirmationGui fillSlots(boolean fill) {
		this.fillEmpty = fill;
		return this;
	}

	public ConfirmationGui setConfirmSlot(int row, int column) {
		validatePosition(row, column);
		this.confirmRow = row;
		this.confirmColumn = column;
		return this;
	}

	public ConfirmationGui setConfirmSlot(int slot) {
		int[] position = position(slot);
		return setConfirmSlot(position[0], position[1]);
	}

	public ConfirmationGui setCancelSlot(int row, int column) {
		validatePosition(row, column);
		this.cancelRow = row;
		this.cancelColumn = column;
		return this;
	}

	public ConfirmationGui setCancelSlot(int slot) {
		int[] position = position(slot);
		return setCancelSlot(position[0], position[1]);
	}

	public ConfirmationGui setInformationSlot(int row, int column) {
		validatePosition(row, column);
		this.infoRow = row;
		this.infoColumn = column;
		return this;
	}

	public ConfirmationGui setInformationSlot(int slot) {
		int[] position = position(slot);
		return setInformationSlot(position[0], position[1]);
	}

	public ConfirmationGui showConfirm(boolean show) {
		this.showConfirm = show;
		return this;
	}

	public ConfirmationGui showCancel(boolean show) {
		this.showCancel = show;
		return this;
	}

	public ConfirmationGui showInformation(boolean show) {
		this.showInformation = show;
		return this;
	}

	public ConfirmationGui confirmItem(ItemStack item) {
		this.confirmItem = cloneOrNull(item);
		return this;
	}

	public ConfirmationGui confirmItem(ZMaterial material, String name) {
		return confirmItem(material, name, null, false);
	}

	public ConfirmationGui confirmItem(ZMaterial material, String name, List<String> lore, boolean glow) {
		this.confirmItem = ItemCreator.of(material).name(name).lore(lore).glow(glow).makeMenuItem();
		return this;
	}

	public ConfirmationGui cancelItem(ItemStack item) {
		this.cancelItem = cloneOrNull(item);
		return this;
	}

	public ConfirmationGui cancelItem(ZMaterial material, String name) {
		return cancelItem(material, name, null, false);
	}

	public ConfirmationGui cancelItem(ZMaterial material, String name, List<String> lore, boolean glow) {
		this.cancelItem = ItemCreator.of(material).name(name).lore(lore).glow(glow).makeMenuItem();
		return this;
	}

	public ConfirmationGui informationItem(ItemStack item) {
		this.informationItem = cloneOrNull(item);
		return this;
	}

	public ConfirmationGui informationItem(ZMaterial material, String name) {
		return informationItem(material, name, null, false);
	}

	public ConfirmationGui informationItem(ZMaterial material, String name, List<String> lore, boolean glow) {
		this.informationItem = ItemCreator.of(material).name(name).lore(lore).glow(glow).makeMenuItem();
		return this;
	}

	public ConfirmationGui emptyItem(ItemStack item) {
		this.emptyItem = cloneOrNull(item);
		return this;
	}

	public ConfirmationGui emptyItem(ZMaterial material, String name) {
		return emptyItem(material, name, null, false);
	}

	public ConfirmationGui emptyItem(ZMaterial material, String name, List<String> lore, boolean glow) {
		this.emptyItem = ItemCreator.of(material).name(name).lore(lore).glow(glow).makeMenuItem();
		return this;
	}

	public ConfirmationGui defaultItems() {
		confirmItem(ZMaterial.LIME_DYE, "&aConfirm");
		cancelItem(ZMaterial.RED_DYE, "&cCancel");
		informationItem(ZMaterial.PAPER, "&7Do you want to proceed?");
		return this;
	}

	public ConfirmationGui closeOnClick(boolean close) {
		this.closeOnClick = close;
		return this;
	}

	@Override
	public ConfirmationGui playSoundOnClick(boolean play) {
		this.playSound = play;
		return this;
	}

	public ConfirmationGui onConfirm(Consumer<GuiClickEvent> action) {
		this.confirmEvent = action == null ? event -> {
		} : action;
		return this;
	}

	public ConfirmationGui onCancel(Consumer<GuiClickEvent> action) {
		this.cancelEvent = action == null ? event -> {
		} : action;
		return this;
	}

	public ConfirmationGui onDismiss(Consumer<Player> action) {
		this.dismissEvent = action == null ? player -> {
		} : action;
		return this;
	}

	public ConfirmationGui setConfirmSound(RosaSound.SoundHolder sound) {
		this.confirmSound = sound;
		return this;
	}

	public ConfirmationGui setCancelSound(RosaSound.SoundHolder sound) {
		this.cancelSound = sound;
		return this;
	}

	public ConfirmationGui setCloseSound(RosaSound.SoundHolder sound) {
		this.closeSound = sound;
		return this;
	}

	public ConfirmationGui setItems() {
		validateItems();
		int confirmSlot = slotIndex(this.confirmRow, this.confirmColumn);
		int cancelSlot = slotIndex(this.cancelRow, this.cancelColumn);
		int informationSlot = slotIndex(this.infoRow, this.infoColumn);
		Set<Integer> visibleSlots = new HashSet<>();
		if (this.showConfirm && !visibleSlots.add(confirmSlot)
				|| this.showCancel && !visibleSlots.add(cancelSlot)
				|| this.showInformation && !visibleSlots.add(informationSlot))
			throw new IllegalStateException("Visible confirmation GUI items must use different slots");

		clearManagedSlots();
		if (this.fillEmpty) {
			for (int slot = 0; slot < getSize(); slot++) {
				if (!this.cellItems.containsKey(slot)) {
					setItem(slot, this.emptyItem);
					this.managedSlots.add(slot);
				}
			}
		}

		if (this.showConfirm) {
			setButton(this.confirmRow, this.confirmColumn, this.confirmItem, (ZSound) null, event -> {
				this.resolvedPlayers.add(event.getPlayer().getUniqueId());
				playSound(event.getPlayer(), this.confirmSound);
				if (this.closeOnClick)
					event.getPlayer().closeInventory();
				this.confirmEvent.accept(event);
			});
			this.managedSlots.add(confirmSlot);
		}
		if (this.showCancel) {
			setButton(this.cancelRow, this.cancelColumn, this.cancelItem, (ZSound) null, event -> {
				this.resolvedPlayers.add(event.getPlayer().getUniqueId());
				playSound(event.getPlayer(), this.cancelSound);
				if (this.closeOnClick)
					event.getPlayer().closeInventory();
				this.cancelEvent.accept(event);
			});
			this.managedSlots.add(cancelSlot);
		}
		if (this.showInformation) {
			setItem(this.infoRow, this.infoColumn, this.informationItem);
			this.managedSlots.add(informationSlot);
		}
		update();
		return this;
	}

	@Override
	protected void onOpen(GuiManager manager, Player player) {
		this.resolvedPlayers.remove(player.getUniqueId());
	}

	@Override
	protected void onClose(GuiManager manager, Player player) {
		if (this.resolvedPlayers.remove(player.getUniqueId())) return;
		playSound(player, this.closeSound);
		this.dismissEvent.accept(player);
	}

	private void validatePosition(int row, int column) {
		if (row < 1 || row > this.rows) {
			throw new IllegalArgumentException("row must be between 1 and " + this.rows);
		}
		if (column < 1 || column > 9) {
			throw new IllegalArgumentException("column must be between 1 and 9");
		}
	}

	private void validateItems() {
		if (this.showConfirm) Objects.requireNonNull(this.confirmItem, "confirmItem");
		if (this.showCancel) Objects.requireNonNull(this.cancelItem, "cancelItem");
		if (this.showInformation) Objects.requireNonNull(this.informationItem, "informationItem");
		if (this.fillEmpty) Objects.requireNonNull(this.emptyItem, "emptyItem");
		if (this.showConfirm) validatePosition(this.confirmRow, this.confirmColumn);
		if (this.showCancel) validatePosition(this.cancelRow, this.cancelColumn);
		if (this.showInformation) validatePosition(this.infoRow, this.infoColumn);
	}

	private void clearManagedSlots() {
		for (Integer slot : this.managedSlots) {
			this.cellItems.remove(slot);
			this.actions.remove(slot);
			this.clickSounds.remove(slot);
		}
		this.managedSlots.clear();
	}

	private void playSound(Player player, RosaSound.SoundHolder sound) {
		if (!this.playSound || sound == null || sound.getSound() == null) return;
		sound.getSound().play(player, sound.getVolume(), sound.getPitch());
	}

	private static int slotIndex(int row, int column) {
		return (row - 1) * 9 + (column - 1);
	}

	private int[] position(int slot) {
		if (slot < 0 || slot >= getSize()) {
			throw new IllegalArgumentException("slot must be between 0 and " + (getSize() - 1));
		}
		return new int[]{slot / 9 + 1, slot % 9 + 1};
	}

	private static ItemStack cloneOrNull(ItemStack item) {
		return item == null ? null : item.clone();
	}
}

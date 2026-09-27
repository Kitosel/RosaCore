package pl.kiosel.rosacore.nms.v1_16_R3.anvil;

import net.minecraft.server.v1_16_R3.*;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.nms.api.NMSUtils;
import pl.kiosel.rosacore.nms.api.anvil.AnvilClickHandler;
import pl.kiosel.rosacore.nms.api.anvil.AnvilTextChangeHandler;
import pl.kiosel.rosacore.nms.api.anvil.CustomAnvil;

final class NMSCustomAnvil implements CustomAnvil {

	private final Player player;
	private final ItemStack[] items = new ItemStack[3];
	private String title = "";
	private String renameText = "";
	private AnvilTextChangeHandler textChangeHandler;
	private AnvilClickHandler clickHandler;
	private NMSAnvilContainer container;
	private boolean open;

	NMSCustomAnvil(Player player) {
		this.player = player;
	}

	@Override
	public Player getPlayer() {
		return player;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public CustomAnvil setTitle(String title) {
		this.title = title == null ? "" : title;
		return this;
	}

	@Override
	public String getRenameText() {
		return renameText;
	}

	@Override
	public CustomAnvil setRenameText(String text) {
		this.renameText = text == null ? "" : text;
		if (container != null) container.a(this.renameText);
		return this;
	}

	@Override
	public ItemStack getItem(int slot) {
		validateSlot(slot);
		ItemStack item = items[slot];
		return item == null ? null : item.clone();
	}

	@Override
	public CustomAnvil setItem(int slot, ItemStack item) {
		validateSlot(slot);
		items[slot] = item == null ? null : item.clone();
		if (container != null) {
			container.getBukkitView().getTopInventory().setItem(slot, items[slot]);
		}
		return this;
	}

	@Override
	public Inventory getInventory() {
		return container == null ? null : container.getBukkitView().getTopInventory();
	}

	@Override
	public CustomAnvil setTextChangeHandler(AnvilTextChangeHandler handler) {
		this.textChangeHandler = handler;
		return this;
	}

	@Override
	public CustomAnvil setClickHandler(AnvilClickHandler handler) {
		this.clickHandler = handler;
		return this;
	}

	@Override
	public boolean handleClick(int slot, ItemStack currentItem) {
		return clickHandler == null || clickHandler.onClick(this, slot, currentItem);
	}

	@Override
	public void handleClose() {
		cleanup();
	}

	@Override
	public void open() {
		if (!player.isOnline()) throw new IllegalStateException("Player is offline");
		if (open) return;

		EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
		BlockPosition position = new BlockPosition(entityPlayer.locX(), entityPlayer.locY(), entityPlayer.locZ());
		int windowId = entityPlayer.nextContainerCounter();
		container = new NMSAnvilContainer(windowId, entityPlayer.inventory,
				ContainerAccess.at(entityPlayer.world, position), this);
		entityPlayer.playerConnection.sendPacket(new PacketPlayOutOpenWindow(
				windowId,
				Containers.ANVIL,
				new ChatComponentText(NMSUtils.color(title))
		));
		entityPlayer.activeContainer = container;
		for (int slot = LEFT_INPUT_SLOT; slot <= RESULT_SLOT; slot++) {
			if (items[slot] != null) {
				container.getBukkitView().getTopInventory().setItem(slot, items[slot]);
			}
		}
		container.a(renameText);
		container.addSlotListener(entityPlayer);
		open = true;
	}

	@Override
	public void close() {
		if (open && player.isOnline()) player.closeInventory();
		cleanup();
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	void handleTextChange(String text) {
		renameText = text;
		if (textChangeHandler != null) textChangeHandler.onTextChange(this, text);
	}

	private void cleanup() {
		open = false;
		container = null;
	}

	private static void validateSlot(int slot) {
		if (slot < LEFT_INPUT_SLOT || slot > RESULT_SLOT) {
			throw new IllegalArgumentException("Anvil slot must be between 0 and 2: " + slot);
		}
	}
}

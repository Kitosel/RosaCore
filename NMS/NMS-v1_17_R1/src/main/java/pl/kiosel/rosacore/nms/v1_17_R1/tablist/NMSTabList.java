package pl.kiosel.rosacore.nms.v1_17_R1.tablist;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.nms.api.NMSUtils;
import pl.kiosel.rosacore.nms.api.packet.RosaPacketMarker;
import pl.kiosel.rosacore.nms.api.tablist.TabList;
import pl.kiosel.rosacore.nms.api.tablist.TabListCell;
import pl.kiosel.rosacore.nms.api.tablist.TabListDiff;
import pl.kiosel.rosacore.nms.api.tablist.TabListSkin;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static pl.kiosel.rosacore.nms.api.NMSUtils.color;

final class NMSTabList implements TabList {

	private final NMSTabListService service;
	private final Player player;
	private final TabListCell[] cells = new TabListCell[DEFAULT_CELL_COUNT];
	private ServerPlayer[] renderedProfiles;
	private TabListCell[] renderedCells;
	private int activeCellCount = DEFAULT_CELL_COUNT;
	private String header = "";
	private String footer = "";
	private String renderedHeader;
	private String renderedFooter;

	NMSTabList(NMSTabListService service, Player player) {
		this.service = service;
		this.player = player;
	}

	@Override
	public Player getPlayer() {
		return player;
	}

	@Override
	public int getCellCount() {
		return DEFAULT_CELL_COUNT;
	}

	@Override
	public int getActiveCellCount() {
		return activeCellCount;
	}

	@Override
	public TabList setActiveCellCount(int cellCount) {
		if (cellCount < 0 || cellCount > DEFAULT_CELL_COUNT) {
			throw new IllegalArgumentException("Active tab-list cell count must be between 0 and 80: " + cellCount);
		}
		this.activeCellCount = cellCount;
		return this;
	}

	@Override
	public String getHeader() {
		return header;
	}

	@Override
	public TabList setHeader(String header) {
		this.header = header == null ? "" : header;
		return this;
	}

	@Override
	public String getFooter() {
		return footer;
	}

	@Override
	public TabList setFooter(String footer) {
		this.footer = footer == null ? "" : footer;
		return this;
	}

	@Override
	public TabListCell getCell(int slot) {
		validateSlot(slot);
		return cells[slot];
	}

	@Override
	public TabList setCell(int slot, TabListCell cell) {
		validateSlot(slot);
		cells[slot] = cell;
		return this;
	}

	@Override
	public TabList clearCell(int slot) {
		validateSlot(slot);
		cells[slot] = null;
		return this;
	}

	@Override
	public TabList clearCells() {
		Arrays.fill(cells, null);
		return this;
	}

	@Override
	public TabList showRealPlayer(Player realPlayer) {
		if (canUseRealPlayer(realPlayer)) send(new ClientboundPlayerInfoPacket(
				ClientboundPlayerInfoPacket.Action.ADD_PLAYER,
				((CraftPlayer) realPlayer).getHandle()));
		return this;
	}

	@Override
	public TabList hideRealPlayer(Player realPlayer) {
		if (canUseRealPlayer(realPlayer)) send(new ClientboundPlayerInfoPacket(
				ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER,
				((CraftPlayer) realPlayer).getHandle()));
		return this;
	}

	@Override
	public TabList hideRealPlayer(UUID profileId) {
		if (profileId == null || !player.isOnline()) return this;
		TabListCell cell = TabListCell.profile(profileId, "removed", "", 0, null, GameMode.SURVIVAL, true);
		ServerPlayer[] profiles = createProfiles(new TabListCell[]{cell}, 1);
		if (profiles.length > 0)
			send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, profiles));
		return this;
	}

	private boolean canUseRealPlayer(Player realPlayer) {
		return realPlayer != null && player.isOnline() && realPlayer.isOnline() && player.canSee(realPlayer);
	}

	@Override
	public void sendHeaderFooter() {
		if (!player.isOnline()) return;
		sendHeaderAndFooter(header, footer);
		renderedHeader = header;
		renderedFooter = footer;
	}

	@Override
	public void send() {
		if (!player.isOnline()) return;
		TabListCell[] nextCells = Arrays.copyOf(cells, activeCellCount);
		TabListDiff diff = TabListDiff.between(renderedCells, nextCells);
		ServerPlayer[] nextProfiles = createProfiles();
		if (renderedProfiles == null) {
			hideRealProfiles();
			if (nextProfiles.length > 0)
				send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, nextProfiles));
		} else if (diff.requiresRecreate()) {
			removeRenderedProfiles();
			if (nextProfiles.length > 0)
				send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, nextProfiles));
		} else if (nextProfiles.length > 0) {
			if (diff.isTextChanged())
				send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME, nextProfiles));
			if (diff.isPingChanged())
				send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.UPDATE_LATENCY, nextProfiles));
			if (diff.isGameModeChanged())
				send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.UPDATE_GAME_MODE, nextProfiles));
		}
		renderedProfiles = nextProfiles;
		renderedCells = nextCells;
		if (!Objects.equals(renderedHeader, header) || !Objects.equals(renderedFooter, footer)) {
			sendHeaderFooter();
		}
	}

	@Override
	public void clear() {
		clearInternal();
		service.remove(this);
	}

	void clearInternal() {
		if (!player.isOnline()) return;
		removeRenderedProfiles();
		restoreRealProfiles();
		sendHeaderAndFooter("", "");
	}

	private ServerPlayer[] createProfiles() {
		return createProfiles(cells, activeCellCount);
	}

	private ServerPlayer[] createProfiles(TabListCell[] sourceCells, int cellCount) {
		List<ServerPlayer> profiles = new ArrayList<>();
		ServerLevel world = ((CraftWorld) player.getWorld()).getHandle();
		MinecraftServer server = MinecraftServer.getServer();

		for (int slot = 0; slot < cellCount; slot++) {
			TabListCell cell = sourceCells[slot];
			if (cell != null && !cell.isListed()) continue;
			GameProfile profile = new GameProfile(profileId(slot, cell), profileName(slot, cell));
			if (cell != null && cell.getSkin() != null) addSkin(profile, cell.getSkin());

			ServerPlayer entry = new ServerPlayer(server, world, profile);
			entry.latency = cell == null ? 0 : Math.max(0, cell.getPing());
			entry.listName = Component.nullToEmpty(color(cell == null ? "" : cell.getText()));
			NMSUtils.applyGameMode(entry, cell == null ? GameMode.SURVIVAL : cell.getGameMode());
			profiles.add(entry);
		}
		return profiles.toArray(new ServerPlayer[0]);
	}

	private void hideRealProfiles() {
		ServerPlayer[] profiles = visibleRealProfiles();
		if (profiles.length > 0) {
			send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, profiles));
		}
	}

	private void restoreRealProfiles() {
		ServerPlayer[] profiles = visibleRealProfiles();
		if (profiles.length > 0) {
			send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, profiles));
		}
	}

	private ServerPlayer[] visibleRealProfiles() {
		List<ServerPlayer> profiles = new ArrayList<>();
		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			if (player.canSee(onlinePlayer)) {
				profiles.add(((CraftPlayer) onlinePlayer).getHandle());
			}
		}
		return profiles.toArray(new ServerPlayer[0]);
	}

	private void removeRenderedProfiles() {
		if (renderedProfiles == null || !player.isOnline()) return;
		if (renderedProfiles.length > 0)
			send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, renderedProfiles));
		renderedProfiles = null;
		renderedCells = null;
	}

	private void sendHeaderAndFooter(String header, String footer) {
		ClientboundTabListPacket packet = new ClientboundTabListPacket(
				Component.nullToEmpty(color(header)),
				Component.nullToEmpty(color(footer)));
		send(packet);
	}

	private void send(Packet<?> packet) {
		((CraftPlayer) player).getHandle().connection.send(RosaPacketMarker.mark(packet));
	}

	private static void addSkin(GameProfile profile, TabListSkin skin) {
		String value = skin.getValue();
		if (value == null || value.isEmpty()) return;
		String signature = skin.getSignature();
		profile.getProperties().put("textures", signature == null || signature.isEmpty()
				? new Property("textures", value)
				: new Property("textures", value, signature));
	}

	private static UUID profileId(int slot, TabListCell cell) {
		if (cell != null && cell.getUniqueId() != null) return cell.getUniqueId();
		return UUID.nameUUIDFromBytes(("rosacore:tablist:" + slot).getBytes(StandardCharsets.UTF_8));
	}

	private static String profileName(int slot, TabListCell cell) {
		if (cell != null && cell.getProfileName() != null && !cell.getProfileName().isEmpty()) {
			return cell.getProfileName();
		}
		return String.format("rosa%012d", slot);
	}

	private static void validateSlot(int slot) {
		if (slot < 0 || slot >= DEFAULT_CELL_COUNT) {
			throw new IllegalArgumentException("Tab-list slot must be between 0 and 79: " + slot);
		}
	}
}

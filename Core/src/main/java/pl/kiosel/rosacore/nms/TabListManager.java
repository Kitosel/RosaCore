package pl.kiosel.rosacore.nms;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.nms.api.packet.*;
import pl.kiosel.rosacore.nms.api.status.ServerStatusInterceptor;
import pl.kiosel.rosacore.nms.api.tablist.TabListService;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TabListManager implements Listener, AutoCloseable {

	private final RosaPlugin plugin;
	private final TabListService service;
	private final TabPacketInterceptor packets;
	private final ServerStatusInterceptor statusPackets;
	private final Map<UUID, TabPacketInterceptor.Subscription> subscriptions = new ConcurrentHashMap<>();
	private final Set<UUID> pendingRefreshes = ConcurrentHashMap.newKeySet();
	private final TabPacketListener packetListener = new TabPacketListener() {
		@Override
		public void onPlayerInfo(PlayerInfoPacketEvent event) {
			handlePlayerInfo(event);
		}
	};

	public TabListManager(RosaPlugin plugin, TabListService service, TabPacketInterceptor packets,
						  ServerStatusInterceptor statusPackets) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.service = Objects.requireNonNull(service, "service");
		this.packets = Objects.requireNonNull(packets, "packets");
		this.statusPackets = Objects.requireNonNull(statusPackets, "statusPackets");
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		for (Player player : Bukkit.getOnlinePlayers()) subscribe(player);
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		subscribe(event.getPlayer());
		plugin.getRosaScheduler().runGlobalLater(this::refreshActiveTabs, 1L);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		UUID playerId = event.getPlayer().getUniqueId();
		pendingRefreshes.remove(playerId);
		TabPacketInterceptor.Subscription subscription = subscriptions.remove(playerId);
		if (subscription != null) subscription.close();
		service.clear(event.getPlayer());
		packets.clear(event.getPlayer());
	}

	@Override
	public void close() {
		for (TabPacketInterceptor.Subscription subscription : subscriptions.values()) subscription.close();
		subscriptions.clear();
		pendingRefreshes.clear();
		service.close();
		packets.close();
		statusPackets.close();
		HandlerList.unregisterAll(this);
	}

	private void refreshActiveTabs() {
		for (Player viewer : Bukkit.getOnlinePlayers()) {
			if (service.has(viewer)) service.get(viewer).send();
		}
	}

	private void subscribe(Player player) {
		TabPacketInterceptor.Subscription previous = subscriptions.put(
				player.getUniqueId(), packets.subscribe(player, packetListener));
		if (previous != null) previous.close();
	}

	private void handlePlayerInfo(PlayerInfoPacketEvent event) {
		if (event.isRosaCorePacket()
				|| (!event.hasAction(TabPacketAction.ADD_PLAYER)
				&& !event.hasAction(TabPacketAction.UPDATE_LISTED))) return;

		UUID viewerId = event.getViewer().getUniqueId();
		for (TabPacketEntry entry : event.getEntries()) {
			if (!viewerId.equals(entry.getUniqueId()) || !entry.hasListedFlag() || !entry.isListed()) continue;
			scheduleRefresh(viewerId);
			return;
		}
	}

	private void scheduleRefresh(UUID viewerId) {
		if (!pendingRefreshes.add(viewerId)) return;
		plugin.getRosaScheduler().runGlobalLater(() -> {
			pendingRefreshes.remove(viewerId);
			Player viewer = Bukkit.getPlayer(viewerId);
			if (viewer != null && viewer.isOnline() && service.has(viewer)) {
				service.get(viewer).send();
			}
		}, 2L);
	}
}

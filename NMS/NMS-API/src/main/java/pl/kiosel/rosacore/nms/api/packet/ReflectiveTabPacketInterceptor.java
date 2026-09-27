package pl.kiosel.rosacore.nms.api.packet;

import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

final class ReflectiveTabPacketInterceptor implements TabPacketInterceptor {

	private final Map<UUID, Binding> bindings = new ConcurrentHashMap<>();
	private final String handlerPrefix = "rosacore_tab_packets_" + Integer.toHexString(System.identityHashCode(this));

	@Override
	public Subscription subscribe(Player viewer, TabPacketListener listener) {
		if (viewer == null) throw new NullPointerException("viewer");
		if (listener == null) throw new NullPointerException("listener");

		UUID uniqueId = viewer.getUniqueId();
		Binding binding = bindings.get(uniqueId);
		if (binding == null || binding.viewer != viewer || !binding.channel.isOpen()) {
			if (binding != null) removeBinding(uniqueId, binding);
			Binding candidate = new Binding(viewer, findChannel(viewer), handlerName(uniqueId));
			Binding previous = bindings.put(uniqueId, candidate);
			if (previous != null) uninstall(previous);
			install(candidate);
			binding = candidate;
		}
		binding.listeners.addIfAbsent(listener);
		return new SubscriptionImpl(viewer, listener);
	}

	@Override
	public void unsubscribe(Player viewer, TabPacketListener listener) {
		if (viewer == null || listener == null) return;
		Binding binding = bindings.get(viewer.getUniqueId());
		if (binding == null) return;
		binding.listeners.remove(listener);
		if (binding.listeners.isEmpty()) removeBinding(viewer.getUniqueId(), binding);
	}

	@Override
	public void clear(Player viewer) {
		if (viewer == null) return;
		Binding binding = bindings.remove(viewer.getUniqueId());
		if (binding != null) uninstall(binding);
	}

	@Override
	public void clearAll() {
		List<Binding> existing = new ArrayList<>(bindings.values());
		bindings.clear();
		for (Binding binding : existing) uninstall(binding);
	}

	@Override
	public boolean isInjected(Player viewer) {
		return viewer != null && bindings.containsKey(viewer.getUniqueId());
	}

	@Override
	public int size() {
		return bindings.size();
	}

	private void install(final Binding binding) {
		runOnEventLoop(binding.channel, new Runnable() {
			@Override
			public void run() {
				ChannelPipeline pipeline = binding.channel.pipeline();
				if (pipeline.get(binding.handlerName) != null) pipeline.remove(binding.handlerName);
				ChannelDuplexHandler handler = new ChannelDuplexHandler() {
					@Override
					public void write(ChannelHandlerContext context, Object message, ChannelPromise promise)
							throws Exception {
						boolean rosaPacket = RosaPacketMarker.claim(message);
						TabOutboundPacketEvent event = TabPacketDecoder.decode(binding.viewer, message, rosaPacket);
						if (event != null) binding.dispatch(event);
						if (event != null && event.isCancelled()) {
							ReferenceCountUtil.release(message);
							promise.trySuccess();
							return;
						}
						super.write(context, message, promise);
						if (event != null) {
							try {
								event.runAfterSendTasks();
							} catch (RuntimeException exception) {
								Bukkit.getLogger().log(Level.WARNING,
										"A RosaCore post-send tab packet task failed for "
												+ binding.viewer.getName(), exception);
							}
						}
					}
				};
				if (pipeline.get("packet_handler") != null) {
					pipeline.addBefore("packet_handler", binding.handlerName, handler);
				} else {
					pipeline.addLast(binding.handlerName, handler);
				}
			}
		});
	}

	private void uninstall(final Binding binding) {
		binding.listeners.clear();
		if (!binding.channel.isOpen()) return;
		runOnEventLoop(binding.channel, new Runnable() {
			@Override
			public void run() {
				ChannelPipeline pipeline = binding.channel.pipeline();
				if (pipeline.get(binding.handlerName) != null) pipeline.remove(binding.handlerName);
			}
		});
	}

	private void removeBinding(UUID uniqueId, Binding binding) {
		if (bindings.remove(uniqueId, binding)) uninstall(binding);
	}

	private static void runOnEventLoop(Channel channel, Runnable task) {
		if (channel.eventLoop().inEventLoop()) {
			task.run();
		} else {
			channel.eventLoop().submit(task).syncUninterruptibly();
		}
	}

	private String handlerName(UUID uniqueId) {
		return handlerPrefix + '_' + uniqueId.toString().replace("-", "");
	}

	private static Channel findChannel(Player player) {
		try {
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Channel channel = findChannel(handle, 0,
					Collections.newSetFromMap(new IdentityHashMap<>()));
			if (channel != null) return channel;
			throw new IllegalStateException("Could not find the network channel for " + player.getName());
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Could not access the network channel for " + player.getName(), exception);
		}
	}

	private static Channel findChannel(Object value, int depth, java.util.Set<Object> visited) {
		if (value == null || depth > 4 || !visited.add(value)) return null;
		if (value instanceof Channel) return (Channel) value;

		for (Field field : fields(value.getClass())) {
			if (Channel.class.isAssignableFrom(field.getType())) {
				Object channel = read(field, value);
				if (channel instanceof Channel) return (Channel) channel;
			}
		}
		for (Field field : fields(value.getClass())) {
			if (!isConnectionType(field.getType())) continue;
			Object nested = read(field, value);
			Channel channel = findChannel(nested, depth + 1, visited);
			if (channel != null) return channel;
		}
		return null;
	}

	private static boolean isConnectionType(Class<?> type) {
		String name = type.getName().toLowerCase(java.util.Locale.ROOT);
		return name.contains("connection") || name.contains("networkmanager")
				|| name.contains("packetlistener") || name.contains("network")
				|| name.contains("playerconnection");
	}

	private static List<Field> fields(Class<?> type) {
		List<Field> fields = new ArrayList<>();
		Class<?> current = type;
		while (current != null && current != Object.class) {
			Collections.addAll(fields, current.getDeclaredFields());
			current = current.getSuperclass();
		}
		return fields;
	}

	private static Object read(Field field, Object owner) {
		try {
			field.setAccessible(true);
			return field.get(owner);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	private static final class Binding {
		private final Player viewer;
		private final Channel channel;
		private final String handlerName;
		private final CopyOnWriteArrayList<TabPacketListener> listeners = new CopyOnWriteArrayList<>();

		private Binding(Player viewer, Channel channel, String handlerName) {
			this.viewer = viewer;
			this.channel = channel;
			this.handlerName = handlerName;
		}

		private void dispatch(TabOutboundPacketEvent event) {
			for (TabPacketListener listener : listeners) {
				try {
					if (event instanceof PlayerInfoPacketEvent) {
						listener.onPlayerInfo((PlayerInfoPacketEvent) event);
					} else if (event instanceof PlayerSpawnPacketEvent) {
						listener.onPlayerSpawn((PlayerSpawnPacketEvent) event);
					} else if (event instanceof PlayerRespawnPacketEvent) {
						listener.onPlayerRespawn((PlayerRespawnPacketEvent) event);
					}
				} catch (RuntimeException exception) {
					Bukkit.getLogger().log(Level.WARNING,
							"A RosaCore tab packet listener failed for " + viewer.getName(), exception);
				}
			}
		}
	}

	private final class SubscriptionImpl implements Subscription {
		private final Player viewer;
		private final TabPacketListener listener;
		private volatile boolean closed;

		private SubscriptionImpl(Player viewer, TabPacketListener listener) {
			this.viewer = viewer;
			this.listener = listener;
		}

		@Override
		public Player getViewer() {
			return viewer;
		}

		@Override
		public TabPacketListener getListener() {
			return listener;
		}

		@Override
		public boolean isClosed() {
			return closed;
		}

		@Override
		public void close() {
			if (closed) return;
			closed = true;
			unsubscribe(viewer, listener);
		}
	}
}

package pl.kiosel.rosacore.nms.api.status;

import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

final class ReflectiveServerStatusInterceptor implements ServerStatusInterceptor {

	private final CopyOnWriteArrayList<ServerStatusPacketListener> listeners = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<Channel> parentChannels = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<Channel> childChannels = new CopyOnWriteArrayList<>();
	private final String handlerId = Integer.toHexString(System.identityHashCode(this));
	private final String parentHandlerName = "rosacore_status_accept_" + handlerId;
	private final String childHandlerName = "rosacore_status_packet_" + handlerId;
	private volatile boolean installed;

	@Override
	public synchronized Subscription subscribe(ServerStatusPacketListener listener) {
		if (listener == null) throw new NullPointerException("listener");
		if (!installed) install();
		listeners.addIfAbsent(listener);
		return new SubscriptionImpl(listener);
	}

	@Override
	public synchronized void unsubscribe(ServerStatusPacketListener listener) {
		if (listener == null) return;
		listeners.remove(listener);
		if (listeners.isEmpty()) uninstall();
	}

	@Override
	public int size() {
		return listeners.size();
	}

	@Override
	public synchronized void clear() {
		listeners.clear();
		uninstall();
	}

	private void install() {
		List<Channel> channels = serverChannels();
		if (channels.isEmpty()) throw new IllegalStateException("Could not find Minecraft server channels");
		for (Channel channel : channels) installParent(channel);
		installed = true;
	}

	private void installParent(final Channel channel) {
		runOnEventLoop(channel, new Runnable() {
			@Override
			public void run() {
				ChannelPipeline pipeline = channel.pipeline();
				if (pipeline.get(parentHandlerName) != null) return;
				pipeline.addFirst(parentHandlerName, new ChannelInboundHandlerAdapter() {
					@Override
					public void channelRead(ChannelHandlerContext context, Object message) throws Exception {
						super.channelRead(context, message);
						if (message instanceof Channel) installChild((Channel) message);
					}
				});
			}
		});
		parentChannels.addIfAbsent(channel);
	}

	private void installChild(final Channel channel) {
		channel.eventLoop().execute(new Runnable() {
			@Override
			public void run() {
				if (!installed || !channel.isOpen()) return;
				ChannelPipeline pipeline = channel.pipeline();
				if (pipeline.get(childHandlerName) != null) return;
				ChannelDuplexHandler handler = new ChannelDuplexHandler() {
					@Override
					public void write(ChannelHandlerContext context, Object message, ChannelPromise promise)
							throws Exception {
						if (!ServerStatusPacketCodec.supports(message)) {
							super.write(context, message, promise);
							return;
						}
						Object rewritten = message;
						try {
							ServerStatusPacketEvent event = createEvent(channel);
							dispatch(event);
							rewritten = ServerStatusPacketCodec.rewrite(message, event);
						} catch (ReflectiveOperationException | RuntimeException exception) {
							Bukkit.getLogger().log(Level.WARNING,
									"RosaCore could not rewrite a server status response", exception);
						}
						if (rewritten != message) ReferenceCountUtil.release(message);
						super.write(context, rewritten, promise);
					}
				};
				if (pipeline.get("packet_handler") != null) {
					pipeline.addBefore("packet_handler", childHandlerName, handler);
				} else {
					pipeline.addLast(childHandlerName, handler);
				}
				childChannels.addIfAbsent(channel);
			}
		});
	}

	private ServerStatusPacketEvent createEvent(Channel channel) {
		List<ServerStatusSample> sample = new ArrayList<>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			sample.add(new ServerStatusSample(player.getUniqueId(), player.getName()));
		}
		return new ServerStatusPacketEvent(channel.remoteAddress(), Bukkit.getOnlinePlayers().size(),
				Bukkit.getMaxPlayers(), sample);
	}

	private void dispatch(ServerStatusPacketEvent event) {
		for (ServerStatusPacketListener listener : listeners) {
			try {
				listener.onServerStatus(event);
			} catch (RuntimeException exception) {
				Bukkit.getLogger().log(Level.WARNING, "A RosaCore server status listener failed", exception);
			}
		}
	}

	private void uninstall() {
		if (!installed && parentChannels.isEmpty() && childChannels.isEmpty()) return;
		installed = false;
		for (Channel channel : parentChannels) removeHandler(channel, parentHandlerName);
		for (Channel channel : childChannels) removeHandler(channel, childHandlerName);
		parentChannels.clear();
		childChannels.clear();
	}

	private static void removeHandler(final Channel channel, final String name) {
		if (channel == null || !channel.isOpen()) return;
		runOnEventLoop(channel, new Runnable() {
			@Override
			public void run() {
				if (channel.pipeline().get(name) != null) channel.pipeline().remove(name);
			}
		});
	}

	private static List<Channel> serverChannels() {
		try {
			Object minecraftServer = minecraftServer();
			Object connection = serverConnection(minecraftServer);
			List<Channel> result = new ArrayList<>();
			collectChannels(connection, result,
					Collections.newSetFromMap(new IdentityHashMap<>()), 0);
			return result;
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Could not access Minecraft server channels", exception);
		}
	}

	private static Object minecraftServer() throws ReflectiveOperationException {
		return minecraftServer(Bukkit.getServer());
	}

	static Object minecraftServer(Object craftServer) throws ReflectiveOperationException {
		if (craftServer == null) throw new NullPointerException("craftServer");

		for (Method method : craftServer.getClass().getMethods()) {
			if (!method.getName().equals("getServer") || method.getParameterTypes().length != 0) continue;
			Object server = invoke(method, craftServer);
			if (server != null && server != craftServer) return server;
		}
		for (Method method : methods(craftServer.getClass())) {
			if (method.getParameterTypes().length != 0 || !isMinecraftServerType(method.getReturnType())) continue;
			Object server = invoke(method, craftServer);
			if (server != null && server != craftServer) return server;
		}
		for (Field field : fields(craftServer.getClass())) {
			Object server = read(field, craftServer);
			if (server != null && (isMinecraftServerType(field.getType())
					|| isMinecraftServerType(server.getClass()))) return server;
		}
		throw new NoSuchMethodException("MinecraftServer in " + craftServer.getClass().getName());
	}

	private static Object serverConnection(Object server) throws ReflectiveOperationException {
		for (Method method : methods(server.getClass())) {
			if (method.getParameterTypes().length != 0 || !isServerConnectionType(method.getReturnType())) continue;
			Object connection = invoke(method, server);
			if (connection != null) return connection;
		}
		for (Field field : fields(server.getClass())) {
			if (!isServerConnectionType(field.getType())) continue;
			Object value = read(field, server);
			if (value != null) return value;
		}
		for (Field field : fields(server.getClass())) {
			if (Modifier.isStatic(field.getModifiers())) continue;
			Object candidate = read(field, server);
			if (candidate == null) continue;
			List<Channel> channels = new ArrayList<>();
			collectChannels(candidate, channels,
					Collections.newSetFromMap(new IdentityHashMap<>()), 0);
			if (!channels.isEmpty()) return candidate;
		}
		throw new IllegalStateException("Server connection in " + server.getClass().getName());
	}

	private static boolean isMinecraftServerType(Class<?> type) {
		for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
			String name = current.getSimpleName().toLowerCase(Locale.ROOT);
			if (name.contains("minecraftserver") || name.contains("dedicatedserver")) return true;
		}
		return false;
	}

	private static boolean isServerConnectionType(Class<?> type) {
		for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
			String name = current.getSimpleName().toLowerCase(Locale.ROOT);
			if (name.contains("serverconnection") || name.contains("connectionlistener")) return true;
		}
		return false;
	}

	private static void collectChannels(Object value, List<Channel> result, Set<Object> visited, int depth) {
		if (value == null || depth > 2 || !visited.add(value)) return;
		if (value instanceof ChannelFuture) {
			result.add(((ChannelFuture) value).channel());
			return;
		}
		if (value instanceof Collection<?>) {
			for (Object element : new ArrayList<>((Collection<?>) value)) {
				collectChannels(element, result, visited, depth + 1);
			}
			return;
		}
		for (Field field : fields(value.getClass())) {
			if (Modifier.isStatic(field.getModifiers())) continue;
			Class<?> type = field.getType();
			if (!ChannelFuture.class.isAssignableFrom(type) && !Collection.class.isAssignableFrom(type)) continue;
			collectChannels(read(field, value), result, visited, depth + 1);
		}
	}

	private static void runOnEventLoop(Channel channel, Runnable task) {
		if (channel.eventLoop().inEventLoop()) task.run();
		else channel.eventLoop().submit(task).syncUninterruptibly();
	}

	private static List<Field> fields(Class<?> type) {
		List<Field> result = new ArrayList<>();
		Class<?> current = type;
		while (current != null && current != Object.class) {
			Collections.addAll(result, current.getDeclaredFields());
			current = current.getSuperclass();
		}
		return result;
	}

	private static List<Method> methods(Class<?> type) {
		List<Method> result = new ArrayList<>();
		Class<?> current = type;
		while (current != null && current != Object.class) {
			Collections.addAll(result, current.getDeclaredMethods());
			current = current.getSuperclass();
		}
		return result;
	}

	private static Object invoke(Method method, Object owner) {
		try {
			method.setAccessible(true);
			return method.invoke(owner);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return null;
		}
	}

	private static Object read(Field field, Object owner) {
		try {
			field.setAccessible(true);
			return field.get(owner);
		} catch (IllegalAccessException | RuntimeException ignored) {
			return null;
		}
	}

	private final class SubscriptionImpl implements Subscription {
		private final ServerStatusPacketListener listener;
		private volatile boolean closed;

		private SubscriptionImpl(ServerStatusPacketListener listener) {
			this.listener = listener;
		}

		@Override
		public ServerStatusPacketListener getListener() {
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
			unsubscribe(listener);
		}
	}
}

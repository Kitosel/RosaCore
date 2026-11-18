package pl.kiosel.rosacore.nms.v26.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.nms.api.entity.FakeEntity;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

abstract class FakeEntityImpl implements FakeEntity {

	private final Set<UUID> viewers = new LinkedHashSet<>();
	protected Entity entity;
	private Location location;

	protected abstract Entity createEntity(Location location);

	@Override
	public final void spawn(Location location, Iterable<? extends Player> requestedViewers) {
		Location target = copyLocation(location);
		Objects.requireNonNull(requestedViewers, "viewers");

		if (entity == null) {
			this.location = target;
			entity = createEntity(target);
		} else {
			teleport(target);
		}

		for (Player player : requestedViewers) {
			if (player == null || !player.isOnline() ||
					target.getWorld() == null || !target.getWorld().equals(player.getWorld())) continue;

			UUID playerId = player.getUniqueId();
			if (viewers.contains(playerId)) {
				sendPacket(new ClientboundRemoveEntitiesPacket(entity.getId()), player);
			} else {
				viewers.add(playerId);
			}
			sendSpawn(player);
		}
	}

	@Override
	public final void teleport(Location location) {
		Location target = copyLocation(location);
		if (entity == null) {
			this.location = target;
			return;
		}
		if (!location.getWorld().getUID().equals(this.location.getWorld().getUID())) {
			throw new IllegalArgumentException("A fake entity cannot be teleported to another world");
		}

		entity.setPos(target.getX(), target.getY(), target.getZ());
		entity.setYRot(target.getYaw());
		entity.setXRot(target.getPitch());
		this.location = target;

		sendPacketToViewers(createTeleportPacket(target));
	}

	@Override
	public final void remove() {
		if (entity == null) {
			viewers.clear();
			return;
		}
		ClientboundRemoveEntitiesPacket packet = new ClientboundRemoveEntitiesPacket(entity.getId());
		for (UUID viewerId : new LinkedHashSet<>(viewers)) {
			Player player = Bukkit.getPlayer(viewerId);
			if (player != null && player.isOnline()) sendPacket(packet, player);
		}
		viewers.clear();
	}

	@Override
	public final void remove(Player viewer) {
		Player checkedViewer = Objects.requireNonNull(viewer, "viewer");
		if (entity == null || !viewers.remove(checkedViewer.getUniqueId())) return;
		if (checkedViewer.isOnline()) {
			sendPacket(new ClientboundRemoveEntitiesPacket(entity.getId()), checkedViewer);
		}
	}

	@Override
	public final boolean isSpawned() {
		return !viewers.isEmpty();
	}

	@Override
	public final int getEntityId() {
		return entity == null ? -1 : entity.getId();
	}

	protected final void sendMetadataToViewers() {
		if (entity == null) return;
		if (entity.getEntityData().getNonDefaultValues().isEmpty()) return;
		sendPacketToViewers(createMetadataPacket());
	}

	private void sendSpawn(Player player) {
		Location current = location;
		ClientboundAddEntityPacket spawnPacket = new ClientboundAddEntityPacket(
				entity.getId(), entity.getUUID(), current.getX(), current.getY(), current.getZ(),
				current.getPitch(), current.getYaw(), entity.getType(), 0,
				entity.getDeltaMovement(), current.getYaw());
		sendPacket(spawnPacket, player);

		if (!entity.getEntityData().getNonDefaultValues().isEmpty()) {
			sendPacket(createMetadataPacket(), player);
		}
	}

	private Packet<?> createMetadataPacket() {
		try {
			Class<?> packetType = Class.forName(
					"net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket",
					true, FakeEntityImpl.class.getClassLoader());
			Object packet = packetType.getConstructor(int.class, List.class)
					.newInstance(entity.getId(), entity.getEntityData().getNonDefaultValues());
			return Packet.class.cast(packet);
		} catch (ReflectiveOperationException | LinkageError exception) {
			throw new IllegalStateException("Could not create the v26 fake entity metadata packet", exception);
		}
	}

	private Packet<?> createTeleportPacket(Location target) {
		try {
			ClassLoader loader = FakeEntityImpl.class.getClassLoader();
			Class<?> changeType = Class.forName("net.minecraft.world.entity.PositionMoveRotation", true, loader);
			Object change = changeType
					.getConstructor(Vec3.class, Vec3.class, float.class, float.class)
					.newInstance(
							new Vec3(target.getX(), target.getY(), target.getZ()),
							entity.getDeltaMovement(), target.getYaw(), target.getPitch());

			Class<?> packetType = Class.forName(
					"net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket", true, loader);
			Object packet = packetType
					.getConstructor(int.class, changeType, Set.class, boolean.class)
					.newInstance(entity.getId(), change, Collections.emptySet(), true);
			return Packet.class.cast(packet);
		} catch (ReflectiveOperationException | LinkageError exception) {
			throw new IllegalStateException("Could not create the v26 fake entity teleport packet", exception);
		}
	}

	private void sendPacketToViewers(Packet<?> packet) {
		for (UUID viewerId : new LinkedHashSet<>(viewers)) {
			Player player = Bukkit.getPlayer(viewerId);
			if (player == null || !player.isOnline()) {
				viewers.remove(viewerId);
				continue;
			}
			sendPacket(packet, player);
		}
	}

	private void sendPacket(Packet<?> packet, Player player) {
		((CraftPlayer) player).getHandle().connection.send(packet);
	}
}

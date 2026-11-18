package pl.kiosel.rosacore.nms.api.entity;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Objects;

public interface FakeEntity {

	void spawn(Location location, Iterable<? extends Player> viewers);

	void teleport(Location location);

	void remove();

	void remove(Player viewer);

	boolean isSpawned();

	int getEntityId();

	default Location copyLocation(Location location) {
		Location copy = Objects.requireNonNull(location, "location").clone();
		if (copy.getWorld() == null) throw new IllegalArgumentException("location must have a world");
		return copy;
	}
}

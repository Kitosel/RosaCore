package pl.kiosel.rosacore.nms.api.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;

public interface FakeBlock {

	void createForPlayer(Player player, Location location, Material material);

	void createForPlayer(Player player, Map<Location, Material> schematic);

	default void validateSameWorld(Player player, Location location) {
		World locationWorld = location.getWorld();
		if (locationWorld == null) {
			throw new IllegalArgumentException("Fake block location must have a world");
		}
		if (!player.getWorld().equals(locationWorld)) {
			throw new IllegalArgumentException("Fake block location must be in the player's current world");
		}
	}
}

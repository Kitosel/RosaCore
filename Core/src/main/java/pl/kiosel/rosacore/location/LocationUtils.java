package pl.kiosel.rosacore.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtils {
	/**
	 * Compares the block coordinates of two locations <strong>ignoring the world</strong>
	 */
	public static boolean isLocationMatching(Location location1, Location location2) {
		return location1.getBlockX() == location2.getBlockX() &&
				location1.getBlockY() == location2.getBlockY() &&
				location1.getBlockZ() == location2.getBlockZ();
	}

	public static boolean isInArea(Location location, Location pos1, Location pos2) {
		double x1 = Math.min(pos1.getX(), pos2.getX());
		double y1 = Math.min(pos1.getY(), pos2.getY());
		double z1 = Math.min(pos1.getZ(), pos2.getZ());

		double x2 = Math.max(pos1.getX(), pos2.getX());
		double y2 = Math.max(pos1.getY(), pos2.getY());
		double z2 = Math.max(pos1.getZ(), pos2.getZ());

		return location.getX() >= x1 && location.getX() <= x2 &&
				location.getY() >= y1 && location.getY() <= y2 &&
				location.getZ() >= z1 && location.getZ() <= z2;
	}

	public static Location getCenter(Location location) {
		double xOffset = location.getBlockX() > 0 ? 0.5 : -0.5;
		double zOffset = location.getBlockZ() > 0 ? 0.5 : -0.5;
		return new Location(
				location.getWorld(),
				location.getBlockX() + xOffset,
				location.getBlockY(),
				location.getBlockZ() + zOffset,
				0,
				0
		);
	}

	public static String convertLocactionToString(Location location) {
		if (location == null) {
			return "";
		}

		World world = location.getWorld();
		if (world == null) {
			return "";
		}
		return world.getName() + ";" + location.getX() + ";" + location.getY() + ";" + location.getZ() + ";" + location.getYaw() + ";" + location.getPitch() + ";";
	}

	public static Location getLocationFromString(String loc) {
		if (loc == null) {
			return null;
		}

		String[] array = loc.split(";");
		if (array.length < 4) {
			return null;
		}

		World world = Bukkit.getWorld(array[0]);
		if (world == null) {
			world = Bukkit.getWorlds().get(0);
		}
		double blockX = Double.parseDouble(array[1]);
		double blockY = Double.parseDouble(array[2]);
		double blockZ = Double.parseDouble(array[3]);
		float yaw = Float.parseFloat(array[4]);
		float pitch = Float.parseFloat(array[5]);
		return new Location(world, blockX, blockY, blockZ, yaw, pitch);
	}

	public static Position adapt(Location location) {
		World world = location.getWorld();
		String wordName = world == null ? null : world.getName();

		return new Position(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(), wordName);
	}

	public static Location adapt(Position position) {
		World world = position.getWorld()
				.map(Bukkit::getWorld)
				.orElse(null);
		return new Location(world, position.getX(), position.getY(), position.getZ(), position.getYaw(), position.getPitch());
	}

	public static int getMinHeight(World world) {
		try {
			return world.getMinHeight();
		} catch (NoSuchMethodError exception) {
			return 0;
		}
	}
}

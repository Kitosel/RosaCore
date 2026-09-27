package pl.kiosel.rosacore.location;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

import static pl.kiosel.rosacore.utils.NumberUtils.formatNumber;

public final class LocationUtils {

	private LocationUtils() {
	}

	public static Location getLocationWithPrecision(Location location, int precision) {
		Objects.requireNonNull(location, "location");
		double x = Double.parseDouble(formatNumber(location.getX(), precision));
		double y = Double.parseDouble(formatNumber(location.getY(), precision));
		double z = Double.parseDouble(formatNumber(location.getZ(), precision));
		float yaw = Float.parseFloat(formatNumber(location.getYaw(), precision));
		float pitch = Float.parseFloat(formatNumber(location.getPitch(), precision));

		return new Location(
				location.getWorld(),
				x, y, z, yaw, pitch
		);
	}

	public static boolean isLocationMatching(Location first, Location second) {
		Objects.requireNonNull(first, "first");
		Objects.requireNonNull(second, "second");
		return first.getBlockX() == second.getBlockX()
				&& first.getBlockY() == second.getBlockY()
				&& first.getBlockZ() == second.getBlockZ();
	}

	public static boolean isInArea(Location location, Location firstCorner, Location secondCorner) {
		Objects.requireNonNull(location, "location");
		Objects.requireNonNull(firstCorner, "firstCorner");
		Objects.requireNonNull(secondCorner, "secondCorner");
		if (!Objects.equals(firstCorner.getWorld(), secondCorner.getWorld())
				|| !Objects.equals(location.getWorld(), firstCorner.getWorld())) {
			return false;
		}

		double x1 = Math.min(firstCorner.getX(), secondCorner.getX());
		double y1 = Math.min(firstCorner.getY(), secondCorner.getY());
		double z1 = Math.min(firstCorner.getZ(), secondCorner.getZ());
		double x2 = Math.max(firstCorner.getX(), secondCorner.getX());
		double y2 = Math.max(firstCorner.getY(), secondCorner.getY());
		double z2 = Math.max(firstCorner.getZ(), secondCorner.getZ());

		return location.getX() >= x1 && location.getX() <= x2
				&& location.getY() >= y1 && location.getY() <= y2
				&& location.getZ() >= z1 && location.getZ() <= z2;
	}

	public static Location getCenter(Location location) {
		Objects.requireNonNull(location, "location");
		return new Location(location.getWorld(),
				location.getBlockX() + 0.5,
				location.getBlockY(),
				location.getBlockZ() + 0.5,
				0.0f,
				0.0f);
	}

	public static String convertLocationToString(Location location, String separator) {
		if (location == null || location.getWorld() == null) {
			return "";
		}
		if (separator == null || separator.isEmpty()) {
			separator = ";";
		}
		return location.getWorld().getName() + separator +
				location.getX() + separator +
				location.getY() + separator +
				location.getZ() + separator +
				location.getYaw() + separator +
				location.getPitch() + separator;
	}

	public static String convertLocationToString(Location location) {
		return convertLocationToString(location, ";");
	}

	public static Location getLocationFromString(String serialized, String separator) {
		if (serialized == null) {
			return null;
		}
		if (separator == null || separator.isEmpty()) {
			separator = ";";
		}
		String[] values = serialized.split(separator);
		if (values.length < 6) {
			return null;
		}
		World world = Bukkit.getWorld(values[0]);
		if (world == null) {
			return null;
		}
		try {
			return new Location(world,
					Double.parseDouble(values[1]),
					Double.parseDouble(values[2]),
					Double.parseDouble(values[3]),
					Float.parseFloat(values[4]),
					Float.parseFloat(values[5]));
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	public static Location getLocationFromString(String serialized) {
		return getLocationFromString(serialized, ";");
	}

	public static Position adapt(Location location) {
		Objects.requireNonNull(location, "location");
		World world = location.getWorld();
		String worldName = world == null ? null : world.getName();
		return new Position(location.getX(), location.getY(), location.getZ(),
				location.getYaw(), location.getPitch(), worldName);
	}

	public static Location adapt(Position position) {
		Objects.requireNonNull(position, "position");
		World world = position.getWorld().map(Bukkit::getWorld).orElse(null);
		return new Location(world, position.getX(), position.getY(), position.getZ(),
				position.getYaw(), position.getPitch());
	}

	public static int getMinHeight(World world) {
		Objects.requireNonNull(world, "world");
		try {
			Object value = world.getClass().getMethod("getMinHeight").invoke(world);
			return value instanceof Integer ? (Integer) value : 0;
		} catch (ReflectiveOperationException | LinkageError ignored) {
			return 0;
		}
	}
}

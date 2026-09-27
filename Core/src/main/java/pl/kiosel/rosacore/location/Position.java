package pl.kiosel.rosacore.location;

import lombok.Getter;

import java.util.Objects;
import java.util.Optional;

public final class Position {

	public static final Position ZERO = new Position(0, 0, 0);

	@Getter
	private final double x;
	@Getter
	private final double y;
	@Getter
	private final double z;
	@Getter
	private final float yaw;
	@Getter
	private final float pitch;
	private final String world;

	public Position(double x, double y, double z, float yaw, float pitch, String world) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.world = world;
	}

	public Position(double x, double y, double z, String world) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
		this.yaw = 0.0f;
		this.pitch = 0.0f;
	}

	public Position(double x, double y, double z) {
		this(x, y, z, null);
	}

	public Optional<String> getWorld() {
		return Optional.ofNullable(this.world);
	}

	public Position add(Position position) {
		return new Position(this.x + position.x, this.y + position.y, this.z + position.z, this.yaw, this.pitch, this.world);
	}

	public Position add(double x, double y, double z) {
		return new Position(this.x + x, this.y + y, this.z + z, this.yaw, this.pitch, this.world);
	}

	public Position subtract(Position position) {
		return new Position(this.x - position.x, this.y - position.y, this.z - position.z, this.yaw, this.pitch, this.world);
	}

	public Position subtract(double x, double y, double z) {
		return new Position(this.x - x, this.y - y, this.z - z, this.yaw, this.pitch, this.world);
	}

	public Position multiply(Position position) {
		return new Position(this.x * position.x, this.y * position.y, this.z * position.z, this.yaw, this.pitch, this.world);
	}

	public Position multiply(double x, double y, double z) {
		return new Position(this.x * x, this.y * y, this.z * z, this.yaw, this.pitch, this.world);
	}

	public Position divide(Position position) {
		return new Position(this.x / position.x, this.y / position.y, this.z / position.z, this.yaw, this.pitch, this.world);
	}

	public Position divide(double x, double y, double z) {
		return new Position(this.x / x, this.y / y, this.z / z, this.yaw, this.pitch, this.world);
	}

	public Position changeWorld(String world) {
		return new Position(this.x, this.y, this.z, this.yaw, this.pitch, world);
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof Position)) return false;
		Position other = (Position) object;
		return Double.compare(other.x, x) == 0
				&& Double.compare(other.y, y) == 0
				&& Double.compare(other.z, z) == 0
				&& Float.compare(other.yaw, yaw) == 0
				&& Float.compare(other.pitch, pitch) == 0
				&& Objects.equals(world, other.world);
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y, z, yaw, pitch, world);
	}
}

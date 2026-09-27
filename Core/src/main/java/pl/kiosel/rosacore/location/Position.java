package pl.kiosel.rosacore.utils;

import lombok.Getter;

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
		return Optional.of(this.world);
	}

	public Position add(Position position) {
		return new Position(this.x + position.x, this.y + position.y, this.z + position.z, this.pitch, this.yaw, this.world);
	}

	public Position add(double x, double y, double z) {
		return new Position(this.x + x, this.y + y, this.z + z, this.pitch, this.yaw, this.world);
	}

	public Position subtract(Position position) {
		return new Position(this.x - position.x, this.y - position.y, this.z - position.z, this.pitch, this.yaw, this.world);
	}

	public Position subtract(double x, double y, double z) {
		return new Position(this.x - x, this.y - y, this.z - z, this.pitch, this.yaw, this.world);
	}

	public Position multiply(Position position) {
		return new Position(this.x * position.x, this.y * position.y, this.z * position.z, this.pitch, this.yaw, this.world);
	}

	public Position multiply(double x, double y, double z) {
		return new Position(this.x * x, this.y * y, this.z * z, this.pitch, this.yaw, this.world);
	}

	public Position divide(Position position) {
		return new Position(this.x / position.x, this.y / position.y, this.z / position.z, this.pitch, this.yaw, this.world);
	}

	public Position divide(double x, double y, double z) {
		return new Position(this.x / x, this.y / y, this.z / z, this.pitch, this.yaw, this.world);
	}

	public Position changeWorld(String world) {
		return new Position(this.x, this.y, this.z, this.pitch, this.yaw, world);
	}
}

package pl.kiosel.rosacore.material;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.compatibility.ZParticle;

import java.util.Objects;

@Getter
public final class ParticleBuilder {

	private final ZParticle particle;
	private int count = 1;
	private double offsetX;
	private double offsetY;
	private double offsetZ;
	private double extra;

	public ParticleBuilder(ZParticle particle) {
		this.particle = Objects.requireNonNull(particle, "particle");
	}

	public ParticleBuilder(String particle) {
		this(ZParticle.matchOrThrow(particle));
	}

	public static ParticleBuilder of(ZParticle particle) {
		return new ParticleBuilder(particle);
	}

	public static ParticleBuilder of(String particle) {
		return new ParticleBuilder(particle);
	}

	public ParticleBuilder count(int count) {
		if (count < 0) {
			throw new IllegalArgumentException("Particle count cannot be negative");
		}
		this.count = count;
		return this;
	}

	public ParticleBuilder offset(double offset) {
		return offset(offset, offset, offset);
	}

	public ParticleBuilder offset(double offsetX, double offsetY, double offsetZ) {
		this.offsetX = requireNonNegativeFinite(offsetX, "Particle X offset");
		this.offsetY = requireNonNegativeFinite(offsetY, "Particle Y offset");
		this.offsetZ = requireNonNegativeFinite(offsetZ, "Particle Z offset");
		return this;
	}

	public ParticleBuilder extra(double extra) {
		this.extra = requireNonNegativeFinite(extra, "Particle extra value");
		return this;
	}

	public boolean spawn(Location location) {
		return this.particle.spawn(requireLocation(location), this.count,
				this.offsetX, this.offsetY, this.offsetZ, this.extra);
	}

	public boolean spawn(Player player, Location location) {
		return this.particle.spawn(Objects.requireNonNull(player, "player"), requireLocation(location),
				this.count, this.offsetX, this.offsetY, this.offsetZ, this.extra);
	}

	public int spawn(Iterable<? extends Player> players, Location location) {
		Objects.requireNonNull(players, "players");
		Location checkedLocation = requireLocation(location);
		int sent = 0;
		for (Player player : players) {
			if (spawn(Objects.requireNonNull(player, "player"), checkedLocation)) {
				sent++;
			}
		}
		return sent;
	}

	public int spawnNearby(Location location, double radius) {
		Location checkedLocation = requireLocation(location);
		double checkedRadius = requireNonNegativeFinite(radius, "Particle radius");
		World world = checkedLocation.getWorld();
		if (world == null) {
			return 0;
		}

		double radiusSquared = checkedRadius * checkedRadius;
		int sent = 0;
		for (Player player : world.getPlayers()) {
			if (player.getLocation().distanceSquared(checkedLocation) <= radiusSquared
					&& spawn(player, checkedLocation)) {
				sent++;
			}
		}
		return sent;
	}

	public boolean isSupported() {
		return this.particle.isSupported();
	}

	private static Location requireLocation(Location location) {
		return Objects.requireNonNull(location, "location");
	}

	private static double requireNonNegativeFinite(double value, String name) {
		if (value < 0D || Double.isNaN(value) || Double.isInfinite(value)) {
			throw new IllegalArgumentException(name + " must be a finite non-negative number");
		}
		return value;
	}
}

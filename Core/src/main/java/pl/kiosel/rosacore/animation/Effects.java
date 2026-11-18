package pl.kiosel.rosacore.animation;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.kiosel.rosacore.compatibility.RosaSound;
import pl.kiosel.rosacore.compatibility.ZMaterial;
import pl.kiosel.rosacore.compatibility.ZParticle;
import pl.kiosel.rosacore.compatibility.ZSound;
import pl.kiosel.rosacore.message.RosaMessage;
import pl.kiosel.rosacore.nms.api.entity.FakeItem;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class Effects {

	Effects() {
	}

	static Effects delay(int ticks) {
		return new DelayEffect(ticks);
	}

	abstract int durationTicks();

	abstract Effects copy();

	void start(AnimationContext context) {
	}

	void tick(AnimationContext context, int elapsedTicks) {
	}

	void finish(AnimationContext context) {
	}

	void cancel(AnimationContext context) {
		finish(context);
	}

	public static SoundEffect sound(RosaSound.SoundHolder sound) {
		return new SoundEffect(sound);
	}

	public static SoundEffect sound(ZSound sound) {
		return new SoundEffect(new RosaSound.SoundHolder(sound, 1.0f, 1.0f));
	}

	public static SoundEffect sound(ZSound sound, float volume, float pitch) {
		return new SoundEffect(new RosaSound.SoundHolder(sound, volume, pitch));
	}

	public static ParticleEffect particle(ZParticle particle) {
		return new ParticleEffect(particle);
	}

	public static FloatingItemEffect floatingItem(ItemStack item) {
		return new FloatingItemEffect(item);
	}

	public static FloatingItemEffect floatingItem(ZMaterial material) {
		Optional<Material> resolved = material.getMaterial();
		return new FloatingItemEffect(new ItemStack(resolved.orElse(Material.STONE)));
	}

	public static TitleEffect title(String title) {
		return new TitleEffect(title, "");
	}

	public static TitleEffect title(String title, String subtitle) {
		return new TitleEffect(title, subtitle);
	}

	public static TitleEffect title(RosaMessage message) {
		return new TitleEffect(message.plain(), "");
	}

	private static void forEachViewer(AnimationContext context, Consumer<Player> action) {
		for (Player player : context.getViewers()) {
			if (player == null || !player.isOnline()) continue;
			context.getPlugin().getRosaScheduler().runForEntity(player, () -> {
				if (player.isOnline()) action.accept(player);
			}, () -> { });
		}
	}

	private static final class DelayEffect extends Effects {
		private final int ticks;

		private DelayEffect(int ticks) {
			if (ticks < 0) throw new IllegalArgumentException("ticks cannot be negative");
			this.ticks = ticks;
		}

		@Override
		int durationTicks() {
			return ticks;
		}

		@Override
		Effects copy() {
			return new DelayEffect(ticks);
		}
	}

	public static final class SoundEffect extends Effects {
		private final RosaSound.SoundHolder sound;

		private SoundEffect(RosaSound.SoundHolder sound) {
			this.sound = sound;
		}

		@Override
		int durationTicks() {
			return 1;
		}

		@Override
		Effects copy() {
			return new SoundEffect(sound);
		}

		@Override
		void start(AnimationContext context) {
			Location location = context.getLocation();
			forEachViewer(context, player -> sound.getSound().play(
					player, location, sound.getVolume(), sound.getPitch()));
		}
	}

	public static final class ParticleEffect extends Effects {
		private final ZParticle particle;
		private int count = 1;
		private double offsetX;
		private double offsetY;
		private double offsetZ;
		private double extra;

		private ParticleEffect(ZParticle particle) {
			this.particle = particle;
		}

		public ParticleEffect count(int count) {
			if (count < 0) throw new IllegalArgumentException("count cannot be negative");
			this.count = count;
			return this;
		}

		public ParticleEffect spread(double x, double y, double z, double extra) {
			this.offsetX = x;
			this.offsetY = y;
			this.offsetZ = z;
			this.extra = extra;
			return this;
		}

		@Override
		int durationTicks() {
			return 1;
		}

		@Override
		Effects copy() {
			ParticleEffect copy = new ParticleEffect(particle);
			copy.count = count;
			copy.offsetX = offsetX;
			copy.offsetY = offsetY;
			copy.offsetZ = offsetZ;
			copy.extra = extra;
			return copy;
		}

		@Override
		void start(AnimationContext context) {
			Location location = context.getLocation();
			forEachViewer(context, player -> particle.spawn(
					player, location, count, offsetX, offsetY, offsetZ, extra));
		}
	}

	public static final class FloatingItemEffect extends Effects {

		private final ItemStack item;
		private Location destination;
		private int durationTicks = 20;
		private double orbitRadius;
		private FakeItem fakeItem;
		private Location start;

		private FloatingItemEffect(ItemStack item) {
			this.item = item.clone();
		}

		public FloatingItemEffect moveTo(Location location) {
			this.destination = location.clone();
			return this;
		}

		public FloatingItemEffect rotate() {
			return orbit(0.2D);
		}

		public FloatingItemEffect orbit(double radius) {
			if (radius < 0.0D) throw new IllegalArgumentException("radius cannot be negative");
			this.orbitRadius = radius;
			return this;
		}

		public FloatingItemEffect duration(int durationTicks) {
			if (durationTicks < 1) {
				throw new IllegalArgumentException("duration must be at least one tick");
			}
			this.durationTicks = durationTicks;
			return this;
		}

		@Override
		int durationTicks() {
			return durationTicks;
		}

		@Override
		Effects copy() {
			FloatingItemEffect copy = new FloatingItemEffect(item);
			copy.destination = destination == null ? null : destination.clone();
			copy.durationTicks = durationTicks;
			copy.orbitRadius = orbitRadius;
			return copy;
		}

		@Override
		void start(AnimationContext context) {
			start = context.getLocation();
			Location end = destination == null ? start.clone().add(0.0D, 1.0D, 0.0D) : destination.clone();
			if (end.getWorld() == null || end.getWorld() == null || !Objects.equals(start.getWorld(), end.getWorld())) {
				throw new IllegalArgumentException("Fake item destination must be in the animation world");
			}
			fakeItem = context.getPlugin().getNMS().getFakeItemFactory().createItem(item.clone());
			fakeItem.spawn(start, context.getViewers());
		}

		@Override
		void tick(AnimationContext context, int elapsedTicks) {
			if (fakeItem == null) return;
			Location end = destination == null ? start.clone().add(0.0D, 1.0D, 0.0D) : destination;
			double progress = durationTicks <= 1 ? 1.0D
					: Math.min(1.0D, elapsedTicks / (double) (durationTicks - 1));
			Location point = start.clone();
			point.setX(start.getX() + (end.getX() - start.getX()) * progress);
			point.setY(start.getY() + (end.getY() - start.getY()) * progress);
			point.setZ(start.getZ() + (end.getZ() - start.getZ()) * progress);
			if (orbitRadius > 0.0D) {
				double angle = elapsedTicks * (Math.PI / 8.0D);
				point.add(Math.cos(angle) * orbitRadius, 0.0D, Math.sin(angle) * orbitRadius);
			}
			fakeItem.teleport(point);
		}

		@Override
		void finish(AnimationContext context) {
			if (fakeItem != null) {
				fakeItem.remove();
				fakeItem = null;
			}
		}
	}

	public static final class TitleEffect extends Effects {
		private final String title;
		private final String subtitle;

		private TitleEffect(String title, String subtitle) {
			this.title = title;
			this.subtitle = subtitle;
		}

		@Override
		int durationTicks() {
			return 1;
		}

		@Override
		Effects copy() {
			return new TitleEffect(title, subtitle);
		}

		@Override
		void start(AnimationContext context) {
			forEachViewer(context, player -> context.getPlugin().getMessenger().title(player, title, subtitle));
		}
	}
}

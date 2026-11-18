package pl.kiosel.rosacore.animation;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.scheduler.RosaTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RosaAnimation {

	private final RosaPlugin plugin;
	private final List<Player> viewers;
	private final Location location;
	private final List<Effects> effects;

	private RosaAnimation(RosaPlugin plugin, Builder builder) {
		this.plugin = plugin;
		this.viewers = Collections.unmodifiableList(new ArrayList<>(builder.viewers));
		this.location = builder.location.clone();

		List<Effects> copiedEffects = new ArrayList<>(builder.effects.size());
		for (Effects effect : builder.effects)
			copiedEffects.add(effect.copy());
		this.effects = Collections.unmodifiableList(copiedEffects);
	}

	public static Builder builder(RosaPlugin plugin) {
		return new Builder(plugin);
	}

	public AnimationHandle startAnimation() {
		if (viewers.isEmpty()) throw new IllegalStateException("An animation needs at least one viewer");
		if (effects.isEmpty()) throw new IllegalStateException("An animation needs at least one effect");

		AnimationContext context = new AnimationContext(plugin, viewers, location);
		AnimationRunner runner = new AnimationRunner(context, effects);
		AnimationHandle handle = new AnimationHandle(runner::cancelCurrentEffect);
		runner.setHandle(handle);

		RosaTask task = plugin.getRosaScheduler().runAtTimer(location, runner, 0L, 1L);
		handle.attach(task);
		return handle;
	}

	public AnimationHandle play() {
		return startAnimation();
	}

	public static final class Builder {
		private final RosaPlugin plugin;
		private final List<Player> viewers = new ArrayList<>();
		private final List<Effects> effects = new ArrayList<>();
		private Location location;

		private Builder(RosaPlugin plugin) {
			this.plugin = plugin;
		}

		public Builder viewer(Player player) {
			this.viewers.add(player);
			return this;
		}

		public Builder viewers(List<Player> players) {
			this.viewers.clear();
			this.viewers.addAll(players);
			return this;
		}

		public Builder at(Location location) {
			Location copy = location.clone();
			if (copy.getWorld() == null) throw new IllegalArgumentException("location must have a world");
			this.location = copy;
			return this;
		}

		public Builder then(Effects effect) {
			this.effects.add(effect);
			return this;
		}

		public Builder waitTicks(int ticks) {
			this.effects.add(Effects.delay(ticks));
			return this;
		}

		public RosaAnimation build() {
			if (location == null) throw new IllegalStateException("Set the animation location with at(location)");
			return new RosaAnimation(plugin, this);
		}
	}

	private static final class AnimationRunner implements Runnable {
		private final AnimationContext context;
		private final List<Effects> effects;
		private AnimationHandle handle;
		private int index;
		private int elapsedTicks;
		private Effects currentEffect;
		private boolean cancelled;

		private AnimationRunner(AnimationContext context, List<Effects> effects) {
			this.context = context;
			List<Effects> copies = new ArrayList<>(effects.size());
			for (Effects effect : effects) copies.add(effect.copy());
			this.effects = copies;
		}

		private void setHandle(AnimationHandle handle) {
			this.handle = handle;
		}

		@Override
		public void run() {
			if (cancelled || !handle.isRunning()) return;
			try {
				while (index < effects.size()) {
					if (currentEffect == null) {
						currentEffect = effects.get(index);
						currentEffect.start(context);
					}
					currentEffect.tick(context, elapsedTicks);
					elapsedTicks++;
					if (elapsedTicks < Math.max(1, currentEffect.durationTicks())) return;

					currentEffect.finish(context);
					currentEffect = null;
					elapsedTicks = 0;
					index++;
				}
				handle.complete();
			} catch (RuntimeException | Error exception) {
				handle.cancel();
				throw exception;
			}
		}

		private void cancelCurrentEffect() {
			if (cancelled) return;
			cancelled = true;
			if (currentEffect != null) {
				currentEffect.cancel(context);
				currentEffect = null;
			}
		}
	}
}

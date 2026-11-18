package pl.kiosel.rosacore.animation;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.scheduler.RosaTask;
import pl.kiosel.rosacore.version.MajorVersion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class StructureAnimator {

	private final Comparator<StructureBlock> BUILD_ORDER = Comparator
			.comparingInt(StructureBlock::getY)
			.thenComparingInt(StructureBlock::getX)
			.thenComparingInt(StructureBlock::getZ);

	private final RosaPlugin plugin;
	private final Set<Animation> animations = ConcurrentHashMap.newKeySet();

	public StructureAnimator(RosaPlugin plugin) {
		if (MajorVersion.isServerVersionBelow(MajorVersion.V1_13))
			throw new IllegalArgumentException("Structure animator is not supported bellow 1.13 server version");
		this.plugin = plugin;
	}

	public Animation animate(Location origin, Collection<StructureBlock> blocks) {
		return animate(origin, blocks, AnimationSettings.defaults(), () -> {});
	}

	public Animation animate(Location origin, Collection<StructureBlock> blocks,
							 AnimationSettings settings, Runnable completion) {
		if (!Bukkit.isPrimaryThread())
			throw new IllegalStateException("Structure animations must be started on the server thread");

		Location start = Objects.requireNonNull(origin, "origin").clone();
		Objects.requireNonNull(start.getWorld(), "origin world");
		List<StructureBlock> ordered = new ArrayList<>(Objects.requireNonNull(blocks, "blocks"));
		ordered.replaceAll(StructureBlock::copy);
		ordered.sort(BUILD_ORDER);

		Animation animation = new Animation(
				start, ordered, Objects.requireNonNull(settings, "settings"),
				Objects.requireNonNull(completion, "completion")
		);
		this.animations.add(animation);
		animation.start();
		return animation;
	}

	public int getActiveCount() {
		return this.animations.size();
	}

	public void cancelAll() {
		for (Animation animation : this.animations)
			animation.cancel();
	}

	public final class Animation implements Runnable {

		private final Location origin;
		private final List<StructureBlock> blocks;
		private final AnimationSettings settings;
		private final Runnable completion;
		private RosaTask task;
		private int cursor;
		private boolean finished;

		private Animation(Location origin, List<StructureBlock> blocks,
						  AnimationSettings settings, Runnable completion) {
			this.origin = origin;
			this.blocks = blocks;
			this.settings = settings;
			this.completion = completion;
		}

		private void start() {
			if (this.blocks.isEmpty()) {
				finish(true);
				return;
			}
			this.task = plugin.getRosaScheduler().runGlobalTimer(this, 0L, this.settings.getPeriodTicks());
		}

		@Override
		public void run() {
			if (this.finished) return;

			int layerY = this.blocks.get(this.cursor).getY();
			int placedThisTick = 0;

			while (this.cursor < this.blocks.size() && placedThisTick < this.settings.getBlocksPerTick()) {
				StructureBlock placement = this.blocks.get(this.cursor);
				if (placement.getY() != layerY) {
					break;
				}

				placement.place(this.origin, this.settings.isApplyPhysics());
				this.cursor++;
				placedThisTick++;
			}
			if (this.cursor >= this.blocks.size()) finish(true);
		}

		public void cancel() {
			finish(false);
		}

		public boolean isRunning() {
			return !this.finished;
		}

		public int getPlacedBlocks() {
			return this.cursor;
		}

		public int getTotalBlocks() {
			return this.blocks.size();
		}

		private void finish(boolean completed) {
			if (this.finished) return;
			this.finished = true;

			if (this.task != null) {
				this.task.cancel();
				this.task = null;
			}

			animations.remove(this);
			if (completed && this.settings.isRefreshAtEnd())
				refreshBlocks();
			if (completed)
				this.completion.run();
		}

		private void refreshBlocks() {
			World world = this.origin.getWorld();
			if (world == null) return;

			for (StructureBlock placement : this.blocks) {
				Block block = world.getBlockAt(
						this.origin.getBlockX() + placement.getX(),
						this.origin.getBlockY() + placement.getY(),
						this.origin.getBlockZ() + placement.getZ());
				block.setBlockData(block.getBlockData(), true);
			}
		}
	}

	@Getter
	public static final class AnimationSettings {

		private final long periodTicks;
		private final int blocksPerTick;
		private final boolean applyPhysics;
		private final boolean refreshAtEnd;

		public AnimationSettings(long periodTicks, int blocksPerTick, boolean applyPhysics, boolean refreshAtEnd) {
			if (periodTicks < 1L) throw new IllegalArgumentException("periodTicks must be at least 1");
			if (blocksPerTick < 1) throw new IllegalArgumentException("blocksPerTick must be at least 1");
			this.periodTicks = periodTicks;
			this.blocksPerTick = blocksPerTick;
			this.applyPhysics = applyPhysics;
			this.refreshAtEnd = refreshAtEnd;
		}

		public static AnimationSettings defaults() {
			return new AnimationSettings(2L, 32, false, true);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			AnimationSettings settings = (AnimationSettings) o;
			return periodTicks == settings.periodTicks && blocksPerTick == settings.blocksPerTick && applyPhysics == settings.applyPhysics && refreshAtEnd == settings.refreshAtEnd;
		}

		@Override
		public int hashCode() {
			return Objects.hash(periodTicks, blocksPerTick, applyPhysics, refreshAtEnd);
		}

		@Override
		public String toString() {
			return "AnimationSettings[" +
					"periodTicks=" + periodTicks +
					", blocksPerTick=" + blocksPerTick +
					", applyPhysics=" + applyPhysics +
					", refreshAtEnd=" + refreshAtEnd +
					']';
		}
	}

	public static final class StructureBlock {

		@Getter private final int x;
		@Getter private final int y;
		@Getter private final int z;
		private final BlockData data;

		public StructureBlock(int x, int y, int z, BlockData data) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.data = Objects.requireNonNull(data, "data").clone();
		}

		public static StructureBlock of(Material material, int x, int y, int z) {
			return new StructureBlock(x, y, z, Objects.requireNonNull(material, "material").createBlockData());
		}

		public BlockData getData() {
			return this.data.clone();
		}

		private StructureBlock copy() {
			return new StructureBlock(this.x, this.y, this.z, this.data);
		}

		private void place(Location origin, boolean applyPhysics) {
			World world = Objects.requireNonNull(origin.getWorld(), "origin world");
			world.getBlockAt(origin.getBlockX() + this.x, origin.getBlockY() + this.y,
					origin.getBlockZ() + this.z).setBlockData(this.data, applyPhysics);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			StructureBlock that = (StructureBlock) o;
			return x == that.x && y == that.y && z == that.z && Objects.equals(data, that.data);
		}

		@Override
		public int hashCode() {
			return Objects.hash(x, y, z, data);
		}

		@Override
		public String toString() {
			return "StructureBlock[x=" + x + ", y=" + y + ", z=" + z + ", data=" + data + ']';
		}
	}
}
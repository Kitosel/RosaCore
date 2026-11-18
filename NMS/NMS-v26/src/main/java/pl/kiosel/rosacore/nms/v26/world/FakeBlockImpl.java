package pl.kiosel.rosacore.nms.v26.world;

import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.nms.api.world.FakeBlock;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class FakeBlockImpl implements FakeBlock {

	@Override
	public void createForPlayer(Player player, Location location, Material material) {
		validateSameWorld(player, location);

		BlockPos blockPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		BlockState blockState = ((CraftBlockData) material.createBlockData()).getState();

		((CraftPlayer) player).getHandle().connection.send(
				new ClientboundBlockUpdatePacket(blockPos, blockState)
		);
	}

	@Override
	public void createForPlayer(Player player, Map<Location, Material> schematic) {
		Map<SectionPos, Short2ObjectMap<BlockState>> section = new HashMap<>();
		Set<Location> seenLocations = Collections.newSetFromMap(new IdentityHashMap<>());

		for (Map.Entry<Location, Material> entry : schematic.entrySet()) {
			Location location = Objects.requireNonNull(entry.getKey(), "schematic location");
			if (!seenLocations.add(location)) {
				throw new IllegalArgumentException(
						"Schematic reuses the same mutable Location instance for multiple entries. "
								+ "Use location.clone().add(...) for each map key.");
			}
			validateSameWorld(player, location);

			Material material = Objects.requireNonNull(entry.getValue(), "schematic material");
			BlockPos blockPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
			SectionPos sectionPos = SectionPos.of(blockPos);

			short localPosition = SectionPos.sectionRelativePos(blockPos);
			BlockState state = ((CraftBlockData) material.createBlockData()).getState();

			section.computeIfAbsent(sectionPos, k -> new Short2ObjectOpenHashMap<>()).put(localPosition, state);
		}

		ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

		for (Map.Entry<SectionPos, Short2ObjectMap<BlockState>> entry : section.entrySet()) {
			Short2ObjectMap<BlockState> blockStateMap = entry.getValue();
			BlockState[] blockStates = blockStateMap.values().toArray(new BlockState[0]);

			serverPlayer.connection.send(new ClientboundSectionBlocksUpdatePacket(
					entry.getKey(),
					blockStateMap.keySet(),
					blockStates
			));
		}
	}
}

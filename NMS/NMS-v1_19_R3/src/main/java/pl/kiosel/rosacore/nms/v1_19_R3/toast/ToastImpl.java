package pl.kiosel.rosacore.nms.v1_19_R3.toast;

import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.kiosel.rosacore.nms.api.toasts.NmsToasts;

import java.util.*;

public final class ToastImpl implements NmsToasts {

	@Override
	public void sendToast(Plugin plugin, Player player, String titleText, Material material, NmsAdvancementType frameType) {
		ResourceLocation toastId = ResourceLocation.tryBuild(plugin.getName().toLowerCase(Locale.ROOT), "toast_" + UUID.randomUUID());
		if (toastId == null) return;

		Criterion toastCriterion = new Criterion(new ImpossibleTrigger.TriggerInstance());
		Map<String, Criterion> criteria = Collections.singletonMap("toast", toastCriterion);
		String[][] requirements = RequirementsStrategy.AND.createRequirements(Collections.singleton("toast"));

		Advancement advancement = getAdvancement(titleText, material, frameType, criteria, requirements);
		ClientboundUpdateAdvancementsPacket packetShow = getPacketShow(toastId, advancement, criteria, requirements);

		ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
		connection.send(packetShow);

		Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
			if (player.isOnline()) {
				ClientboundUpdateAdvancementsPacket packetClear = new ClientboundUpdateAdvancementsPacket(
						false,
						Collections.emptyList(),
						Collections.singleton(toastId),
						Collections.emptyMap()
				);
				connection.send(packetClear);
			}
		}, 40L);
	}

	private ClientboundUpdateAdvancementsPacket getPacketShow(ResourceLocation toastId, Advancement advancement, Map<String, Criterion> criteria, String[][] requirements) {
		Collection<Advancement> toAdd = Collections.singletonList(advancement);
		Set<ResourceLocation> toRemove = Collections.emptySet();

		AdvancementProgress progress = new AdvancementProgress();
		progress.update(criteria, requirements);

		Iterable<String> remainingCriteria = progress.getRemainingCriteria();
		for (String criterion : remainingCriteria) {
			progress.grantProgress(criterion);
		}

		Map<ResourceLocation, AdvancementProgress> toProgress = Collections.singletonMap(toastId, progress);

		return new ClientboundUpdateAdvancementsPacket(false, toAdd, toRemove, toProgress);
	}

	private Advancement getAdvancement(String titleText, Material material, NmsAdvancementType frameType, Map<String, Criterion> criteria, String[][] requirements) {
		org.bukkit.inventory.ItemStack bukkitItem = new org.bukkit.inventory.ItemStack(material);
		net.minecraft.world.item.ItemStack icon = CraftItemStack.asNMSCopy(bukkitItem);

		Component title = Component.literal(titleText);
		Component description = Component.empty();

		DisplayInfo displayInfo = new DisplayInfo(
				icon,
				title,
				description,
				null,
				FrameType.valueOf(frameType.getName().toUpperCase(Locale.ROOT)),
				true,
				false,
				true
		);

		return new Advancement(
				null,
				null,
				displayInfo,
				AdvancementRewards.EMPTY,
				criteria,
				requirements
		);
	}
}

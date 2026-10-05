package pl.kiosel.rosacore.nms.v1_21_R5.toast;

import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_21_R5.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R5.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.kiosel.rosacore.nms.api.toasts.NmsToasts;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public final class ToastImpl implements NmsToasts {

	private static final Method REQ_ALL_OF_METHOD;
	private static final Constructor<?> CRITERION_CONSTRUCTOR;

	static {
		try {
			Class<?> reqClass = Class.forName("net.minecraft.advancements.AdvancementRequirements");
			REQ_ALL_OF_METHOD = reqClass.getMethod("allOf", Collection.class);

			Class<?> criterionClass = Class.forName("net.minecraft.advancements.Criterion");
			CRITERION_CONSTRUCTOR = criterionClass.getConstructor(
					CriterionTrigger.class,
					CriterionTriggerInstance.class
			);
		} catch (Exception e) {
			throw new ExceptionInInitializerError("Couldn't initialize reflection for ToastImpl: " + e.getMessage());
		}
	}

	@Override
	public void sendToast(Plugin plugin, Player player, String titleText, Material material, NmsAdvancementType frameType) {
		ResourceLocation toastId = ResourceLocation.fromNamespaceAndPath(plugin.getName().toLowerCase(Locale.ROOT), "toast_" + UUID.randomUUID());

		Collection<AdvancementHolder> toAdd = getToAdd(titleText, material, frameType, toastId);
		ClientboundUpdateAdvancementsPacket packetShow = getPacketShow(toastId, toAdd);

		ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
		connection.send(packetShow);

		Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
			if (player.isOnline()) {
				ClientboundUpdateAdvancementsPacket packetClear = new ClientboundUpdateAdvancementsPacket(
						false,
						Collections.emptyList(),
						Collections.singleton(toastId),
						Collections.emptyMap(),
						false
				);
				connection.send(packetClear);
			}
		}, 40L);
	}

	private ClientboundUpdateAdvancementsPacket getPacketShow(ResourceLocation toastId, Collection<AdvancementHolder> toAdd) {
		Set<ResourceLocation> toRemove = Collections.emptySet();

		AdvancementRequirements requirements;
		try {
			requirements = (AdvancementRequirements) REQ_ALL_OF_METHOD.invoke(null, Collections.singleton("toast"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		AdvancementProgress progress = new AdvancementProgress();
		progress.update(requirements);

		Iterable<String> remainingCriteria = progress.getRemainingCriteria();
		for (String criterion : remainingCriteria) {
			progress.grantProgress(criterion);
		}

		Map<ResourceLocation, AdvancementProgress> toProgress = Collections.singletonMap(toastId, progress);

		return new ClientboundUpdateAdvancementsPacket(false, toAdd, toRemove, toProgress, true);
	}

	private Collection<AdvancementHolder> getToAdd(String titleText, Material material, NmsAdvancementType frameType, ResourceLocation toastId) {
		org.bukkit.inventory.ItemStack bukkitItem = new org.bukkit.inventory.ItemStack(material);
		net.minecraft.world.item.ItemStack icon = CraftItemStack.asNMSCopy(bukkitItem);

		Component title = Component.literal(titleText);
		Component description = Component.empty();

		DisplayInfo displayInfo = new DisplayInfo(
				icon,
				title,
				description,
				null,
				AdvancementType.valueOf(frameType.getName().toUpperCase(Locale.ROOT)),
				true,
				false,
				true
		);

		Criterion<?> toastCriterion;
		try {
			toastCriterion = (Criterion<?>) CRITERION_CONSTRUCTOR.newInstance(
					new ImpossibleTrigger(),
					new ImpossibleTrigger.TriggerInstance()
			);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Map<String, Criterion<?>> criteria = Collections.singletonMap("toast", toastCriterion);

		AdvancementRequirements requirements;
		try {
			requirements = (AdvancementRequirements) REQ_ALL_OF_METHOD.invoke(null, Collections.singleton("toast"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Advancement advancement = new Advancement(
				Optional.empty(),
				Optional.of(displayInfo),
				AdvancementRewards.EMPTY,
				criteria,
				requirements,
				false
		);

		return Collections.singletonList(new AdvancementHolder(toastId, advancement));
	}
}

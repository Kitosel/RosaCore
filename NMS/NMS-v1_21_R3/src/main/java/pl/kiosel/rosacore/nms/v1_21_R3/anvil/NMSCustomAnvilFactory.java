package pl.kiosel.rosacore.nms.v1_21_R3.anvil;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.nms.api.NMSUtils;
import pl.kiosel.rosacore.nms.api.anvil.CustomAnvil;
import pl.kiosel.rosacore.nms.api.anvil.CustomAnvilFactory;

public final class NMSCustomAnvilFactory implements CustomAnvilFactory {

	@Override
	public CustomAnvil create(Player player) {
		if (player == null) throw new NullPointerException("player");
		ServerPlayer serverPlayer = (ServerPlayer) NMSUtils.getPlayerHandle(player);
		return new NMSCustomAnvil(player, serverPlayer);
	}
}
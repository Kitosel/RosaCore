package pl.kiosel.rosacore.nms.v26.anvil;

import org.bukkit.entity.Player;
import pl.kiosel.rosacore.nms.api.anvil.CustomAnvil;
import pl.kiosel.rosacore.nms.api.anvil.CustomAnvilFactory;

public final class NMSCustomAnvilFactory implements CustomAnvilFactory {

	@Override
	public CustomAnvil create(Player player) {
		if (player == null) throw new NullPointerException("player");
		return new NMSCustomAnvil(player);
	}
}

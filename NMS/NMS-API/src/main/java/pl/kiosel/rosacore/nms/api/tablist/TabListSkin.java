package pl.kiosel.rosacore.nms.api.tablist;

import org.bukkit.entity.Player;

public interface TabListSkin {

	static TabListSkin of(String value, String signature) {
		return new SimpleTabListSkin(value, signature);
	}

	static TabListSkin unsigned(String value) {
		return new SimpleTabListSkin(value, null);
	}

	static TabListSkin fromPlayer(Player player) {
		return TabListSkins.fromPlayer(player);
	}

	String getValue();

	String getSignature();
}

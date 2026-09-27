package pl.kiosel.rosacore.nms.api.tablist;

import org.bukkit.GameMode;

import java.util.UUID;

public interface TabListCell {

	static TabListCell of(String text) {
		return new SimpleTabListCell(text, 0, null);
	}

	static TabListCell of(String text, int ping) {
		return new SimpleTabListCell(text, ping, null);
	}

	static TabListCell of(String text, int ping, TabListSkin skin) {
		return new SimpleTabListCell(text, ping, skin);
	}

	static TabListCell profile(UUID uniqueId, String profileName, String text, int ping,
							   TabListSkin skin, GameMode gameMode, boolean listed) {
		return new SimpleTabListCell(uniqueId, profileName, text, ping, skin, gameMode, listed);
	}

	default UUID getUniqueId() {
		return null;
	}

	default String getProfileName() {
		return null;
	}

	String getText();

	int getPing();

	TabListSkin getSkin();

	default GameMode getGameMode() {
		return GameMode.SURVIVAL;
	}

	default boolean isListed() {
		return true;
	}
}

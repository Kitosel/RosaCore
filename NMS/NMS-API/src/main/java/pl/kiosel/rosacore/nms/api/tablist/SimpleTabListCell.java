package pl.kiosel.rosacore.nms.api.tablist;

import org.bukkit.GameMode;

import java.util.Objects;
import java.util.UUID;

public final class SimpleTabListCell implements TabListCell {

	private final String text;
	private final int ping;
	private final TabListSkin skin;
	private final UUID uniqueId;
	private final String profileName;
	private final GameMode gameMode;
	private final boolean listed;

	public SimpleTabListCell(String text) {
		this(text, 0, null);
	}

	public SimpleTabListCell(String text, int ping) {
		this(text, ping, null);
	}

	public SimpleTabListCell(String text, int ping, TabListSkin skin) {
		this(null, null, text, ping, skin, GameMode.SURVIVAL, true);
	}

	public SimpleTabListCell(UUID uniqueId, String profileName, String text, int ping,
	                         TabListSkin skin, GameMode gameMode, boolean listed) {
		this.uniqueId = uniqueId;
		this.profileName = profileName == null || profileName.isEmpty() ? null : profileName;
		this.text = text == null ? "" : text;
		this.ping = Math.max(0, ping);
		this.skin = skin;
		this.gameMode = gameMode == null ? GameMode.SURVIVAL : gameMode;
		this.listed = listed;
	}

	@Override
	public UUID getUniqueId() {
		return uniqueId;
	}

	@Override
	public String getProfileName() {
		return profileName;
	}

	@Override
	public String getText() {
		return text;
	}

	@Override
	public int getPing() {
		return ping;
	}

	@Override
	public TabListSkin getSkin() {
		return skin;
	}

	@Override
	public GameMode getGameMode() {
		return gameMode;
	}

	@Override
	public boolean isListed() {
		return listed;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof SimpleTabListCell)) return false;
		SimpleTabListCell that = (SimpleTabListCell) object;
		return ping == that.ping && listed == that.listed
				&& text.equals(that.text) && Objects.equals(skin, that.skin)
				&& Objects.equals(uniqueId, that.uniqueId)
				&& Objects.equals(profileName, that.profileName)
				&& gameMode == that.gameMode;
	}

	@Override
	public int hashCode() {
		return Objects.hash(text, ping, skin, uniqueId, profileName, gameMode, listed);
	}

	@Override
	public String toString() {
		return "SimpleTabListCell{uniqueId=" + uniqueId + ", profileName='" + profileName
				+ "', text='" + text + "', ping=" + ping + ", skin=" + skin
				+ ", gameMode=" + gameMode + ", listed=" + listed + '}';
	}
}

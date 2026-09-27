package pl.kiosel.rosacore.nms.api.packet;

import java.util.UUID;

public final class TabPacketEntry {

	private final UUID uniqueId;
	private final String profileName;
	private final Boolean listed;

	public TabPacketEntry(UUID uniqueId, String profileName, Boolean listed) {
		this.uniqueId = uniqueId;
		this.profileName = profileName;
		this.listed = listed;
	}

	public UUID getUniqueId() {
		return uniqueId;
	}

	public String getProfileName() {
		return profileName;
	}

	public boolean isListed() {
		return listed == null || listed;
	}

	public boolean hasListedFlag() {
		return listed != null;
	}

	@Override
	public String toString() {
		return "TabPacketEntry{uniqueId=" + uniqueId + ", profileName='" + profileName
				+ "', listed=" + listed + '}';
	}
}

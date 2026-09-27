package pl.kiosel.rosacore.nms.api.status;

import java.util.Objects;
import java.util.UUID;

public final class ServerStatusSample {

	private final UUID uniqueId;
	private final String name;

	public ServerStatusSample(UUID uniqueId, String name) {
		this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId");
		this.name = name == null ? "" : name;
	}

	public UUID getUniqueId() {
		return uniqueId;
	}

	public String getName() {
		return name;
	}
}

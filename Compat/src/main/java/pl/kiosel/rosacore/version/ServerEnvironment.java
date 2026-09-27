package pl.kiosel.rosacore.version;

import java.util.Objects;

public final class ServerEnvironment {

	private final MinecraftVersion minecraftVersion;
	private final ServerPlatform platform;
	private final String serverName;

	public ServerEnvironment(MinecraftVersion minecraftVersion, ServerPlatform platform, String serverName) {
		this.minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion");
		this.platform = Objects.requireNonNull(platform, "platform");
		this.serverName = Objects.requireNonNull(serverName, "serverName");
	}

	public MinecraftVersion getMinecraftVersion() {
		return this.minecraftVersion;
	}

	public ServerPlatform getPlatform() {
		return this.platform;
	}

	public String getServerName() {
		return this.serverName;
	}

	public boolean isFolia() {
		return this.platform == ServerPlatform.FOLIA;
	}

	public boolean isPaperBased() {
		return this.platform == ServerPlatform.PAPER || this.platform == ServerPlatform.FOLIA;
	}

	@Override
	public String toString() {
		return this.serverName + " " + this.minecraftVersion + " (" + this.platform + ")";
	}
}

package pl.kiosel.rosacore.api;

import pl.kiosel.rosacore.version.MinecraftVersion;
import pl.kiosel.rosacore.version.ServerEnvironment;

public interface RosaApi {

	String getRosaCoreVersion();

	MinecraftVersion getMinecraftVersion();

	ServerEnvironment getServerEnvironment();
}

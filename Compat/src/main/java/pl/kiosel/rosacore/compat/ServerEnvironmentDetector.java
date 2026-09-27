package pl.kiosel.rosacore.compat;

import pl.kiosel.rosacore.version.MinecraftVersion;
import pl.kiosel.rosacore.version.ServerEnvironment;
import pl.kiosel.rosacore.version.ServerPlatform;

import java.util.Locale;
import java.util.Objects;

public final class ServerEnvironmentDetector {

	private static final String FOLIA_MARKER = "io.papermc.paper.threadedregions.RegionizedServer";
	private static final String MODERN_PAPER_MARKER = "io.papermc.paper.configuration.Configuration";
	private static final String LEGACY_PAPER_MARKER = "com.destroystokyo.paper.PaperConfig";
	private static final String SPIGOT_MARKER = "org.spigotmc.SpigotConfig";

	private ServerEnvironmentDetector() {
	}

	public static ServerEnvironment detect(String minecraftVersion, String serverName, ClassLookup classLookup) {
		Objects.requireNonNull(serverName, "serverName");
		Objects.requireNonNull(classLookup, "classLookup");

		return new ServerEnvironment(
				MinecraftVersion.parse(minecraftVersion),
				detectPlatform(serverName, classLookup),
				serverName
		);
	}

	public static ServerPlatform detectPlatform(String serverName, ClassLookup classLookup) {
		Objects.requireNonNull(serverName, "serverName");
		Objects.requireNonNull(classLookup, "classLookup");

		if (classLookup.isPresent(FOLIA_MARKER))
			return ServerPlatform.FOLIA;

		if (classLookup.isPresent(MODERN_PAPER_MARKER)
				|| classLookup.isPresent(LEGACY_PAPER_MARKER)
				|| isKnownPaperFork(serverName))
			return ServerPlatform.PAPER;

		if (classLookup.isPresent(SPIGOT_MARKER)
				|| serverName.toLowerCase(Locale.ROOT).contains("spigot"))
			return ServerPlatform.SPIGOT;

		return ServerPlatform.BUKKIT;
	}

	private static boolean isKnownPaperFork(String serverName) {
		String normalized = serverName.toLowerCase(Locale.ROOT);
		return normalized.contains("paper")
				|| normalized.contains("purpur")
				|| normalized.contains("pufferfish")
				|| normalized.contains("leaf");
	}
}

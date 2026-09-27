package pl.kiosel.rosacore.version;

import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Version {

	UNKNOWN,

	V1_8, V1_8_1, V1_8_2, V1_8_3, V1_8_4, V1_8_5, V1_8_6, V1_8_7, V1_8_8, V1_8_9,

	V1_9, V1_9_1, V1_9_2, V1_9_3, V1_9_4,

	V1_10, V1_10_1, V1_10_2,

	V1_11, V1_11_1, V1_11_2,

	V1_12, V1_12_1, V1_12_2,

	V1_13, V1_13_1, V1_13_2,

	V1_14, V1_14_1, V1_14_2, V1_14_3, V1_14_4,

	V1_15, V1_15_1, V1_15_2,

	V1_16, V1_16_1, V1_16_2, V1_16_3, V1_16_4, V1_16_5,

	V1_17, V1_17_1,

	V1_18, V1_18_1, V1_18_2,

	V1_19, V1_19_1, V1_19_2, V1_19_3, V1_19_4,

	V1_20, V1_20_1, V1_20_2, V1_20_3, V1_20_4, V1_20_5, V1_20_6,

	V1_21, V1_21_1, V1_21_2, V1_21_3, V1_21_4, V1_21_5, V1_21_6, V1_21_7, V1_21_8, V1_21_9, V1_21_10, V1_21_11,

	V26_1, V26_1_1, V26_1_2, V26_2, V26_2_1, V26_3, V26_4,

	V27_1, V27_2, V27_3;

	private static final Pattern REVISION_PATTERN =
			Pattern.compile("(?:^|\\.)(v\\d+_\\d+_R\\d+)(?:\\.|$)");

	private final int major;
	private final int minor;
	private final int patch;
	private final int revision;

	Version() {
		if ("UNKNOWN".equals(name())) {
			this.major = -1;
			this.minor = -1;
			this.patch = -1;
			this.revision = -1;
			return;
		}

		String[] parts = name().substring(1).split("_");
		this.major = Integer.parseInt(parts[0]);
		this.minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
		this.patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
		this.revision = resolveRevision(major, minor, patch);
	}

	public static Version getServerVersion() {
		Server server = Bukkit.getServer();
		return server == null ? UNKNOWN : fromString(server.getBukkitVersion());
	}

	public static String getServerRevisionVersion() {
		Server server = Bukkit.getServer();
		if (server == null) return UNKNOWN.getRevisionVersion();
		return resolveRevisionVersion(server.getClass().getPackage().getName(), fromString(server.getBukkitVersion()));
	}

	public static Version fromString(String value) {
		if (value == null || value.trim().isEmpty()) return UNKNOWN;
		try {
			return fromMinecraftVersion(MinecraftVersion.parse(value));
		} catch (IllegalArgumentException exception) {
			return UNKNOWN;
		}
	}

	public static Version fromMinecraftVersion(MinecraftVersion version) {
		Objects.requireNonNull(version, "version");
		String name = "V" + version.getMajor() + "_" + version.getMinor();
		if (version.getPatch() != 0) name += "_" + version.getPatch();
		try {
			return valueOf(name);
		} catch (IllegalArgumentException exception) {
			return UNKNOWN;
		}
	}

	public int getMajorNumber() {
		return major;
	}

	public int getMinorNumber() {
		return minor;
	}

	public int getPatchNumber() {
		return patch;
	}

	public int getRevisionNumber() {
		return revision;
	}

	public String getRevisionVersion() {
		return this == UNKNOWN || revision < 0
				? "unknown"
				: "v" + major + "_" + minor + "_R" + revision;
	}

	public MajorVersion getMajorVersion() {
		return MajorVersion.from(this);
	}

	public MinecraftVersion toMinecraftVersion() {
		if (this == UNKNOWN) throw new IllegalStateException("UNKNOWN has no numeric Minecraft version");
		return MinecraftVersion.of(major, minor, patch);
	}

	public boolean isBelow(Version other) {
		return isComparable(other) && compareNumbers(other) < 0;
	}

	public boolean isAtOrBelow(Version other) {
		return isComparable(other) && compareNumbers(other) <= 0;
	}

	public boolean isAbove(Version other) {
		return isComparable(other) && compareNumbers(other) > 0;
	}

	public boolean isAtLeast(Version other) {
		return isComparable(other) && compareNumbers(other) >= 0;
	}

	public boolean isLessThan(Version other) {
		return isBelow(other);
	}

	public boolean isGreaterThan(Version other) {
		return isAbove(other);
	}

	public boolean isAny(Version... versions) {
		if (versions == null) return false;
		for (Version version : versions) {
			if (this == version) return true;
		}
		return false;
	}

	public static boolean isServerVersion(Version... versions) {
		return getServerVersion().isAny(versions);
	}

	public static boolean isServerVersionBelow(Version version) {
		return getServerVersion().isBelow(version);
	}

	public static boolean isServerVersionAtOrBelow(Version version) {
		return getServerVersion().isAtOrBelow(version);
	}

	public static boolean isServerVersionAbove(Version version) {
		return getServerVersion().isAbove(version);
	}

	public static boolean isServerVersionAtLeast(Version version) {
		return getServerVersion().isAtLeast(version);
	}

	private boolean isComparable(Version other) {
		return other != null && this != UNKNOWN && other != UNKNOWN;
	}

	private int compareNumbers(Version other) {
		int result = Integer.compare(major, other.major);
		if (result != 0) return result;
		result = Integer.compare(minor, other.minor);
		return result != 0 ? result : Integer.compare(patch, other.patch);
	}

	static String resolveRevisionVersion(String serverPackage, Version fallbackVersion) {
		if (serverPackage != null) {
			Matcher matcher = REVISION_PATTERN.matcher(serverPackage);
			if (matcher.find()) return matcher.group(1);
		}
		return fallbackVersion == null ? "unknown" : fallbackVersion.getRevisionVersion();
	}

	private static int resolveRevision(int major, int minor, int patch) {
		if (major == 26) return 1;
		if (major != 1) return -1;

		switch (minor) {
			case 8:
			case 19:
				if (patch <= 2) return 1;
				return patch == 3 ? 2 : 3;
			case 9:
				return patch <= 2 ? 1 : 2;
			case 10:
			case 11:
			case 12:
			case 14:
			case 15:
			case 17:
				return 1;
			case 13:
				return patch == 0 ? 1 : 2;
			case 16:
				if (patch <= 1) return 1;
				return patch <= 3 ? 2 : 3;
			case 18:
				return patch <= 1 ? 1 : 2;
			case 20:
				if (patch <= 1) return 1;
				if (patch == 2) return 2;
				return patch <= 4 ? 3 : 4;
			case 21:
				if (patch <= 1) return 1;
				if (patch <= 3) return 2;
				if (patch == 4) return 3;
				if (patch == 5) return 4;
				if (patch <= 8) return 5;
				return patch <= 10 ? 6 : 7;
			default:
				return -1;
		}
	}

	@Override
	public String toString() {
		return this == UNKNOWN ? "unknown" : toMinecraftVersion().toString();
	}
}

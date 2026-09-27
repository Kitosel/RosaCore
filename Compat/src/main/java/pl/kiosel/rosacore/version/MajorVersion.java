package pl.kiosel.rosacore.version;

import org.bukkit.Bukkit;
import org.bukkit.Server;

public enum MajorVersion {

	UNKNOWN,
	V1_8,
	V1_9,
	V1_10,
	V1_11,
	V1_12,
	V1_13,
	V1_14,
	V1_15,
	V1_16,
	V1_17,
	V1_18,
	V1_19,
	V1_20,
	V1_21,
	V26_1,
	V26_2,
	V26_3,
	V26_4,
	V27_1,
	V27_2,
	V27_3;

	private final int major;
	private final int minor;

	MajorVersion() {
		if ("UNKNOWN".equals(name())) {
			this.major = -1;
			this.minor = -1;
			return;
		}

		String[] parts = name().substring(1).split("_");
		this.major = Integer.parseInt(parts[0]);
		this.minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
	}

	public static MajorVersion getServerVersion() {
		Server server = Bukkit.getServer();
		return server == null ? UNKNOWN : fromString(server.getBukkitVersion());
	}

	public static MajorVersion from(Version version) {
		if (version == null || version == Version.UNKNOWN) return UNKNOWN;
		return fromNumbers(version.getMajorNumber(), version.getMinorNumber());
	}

	public static MajorVersion fromString(String value) {
		if (value == null || value.trim().isEmpty()) return UNKNOWN;
		try {
			MinecraftVersion version = MinecraftVersion.parse(value);
			return fromNumbers(version.getMajor(), version.getMinor());
		} catch (IllegalArgumentException exception) {
			return UNKNOWN;
		}
	}

	private static MajorVersion fromNumbers(int major, int minor) {
		try {
			return valueOf("V" + major + "_" + minor);
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

	public boolean isBelow(MajorVersion other) {
		return isComparable(other) && compareNumbers(other) < 0;
	}

	public boolean isAtOrBelow(MajorVersion other) {
		return isComparable(other) && compareNumbers(other) <= 0;
	}

	public boolean isAbove(MajorVersion other) {
		return isComparable(other) && compareNumbers(other) > 0;
	}

	public boolean isAtLeast(MajorVersion other) {
		return isComparable(other) && compareNumbers(other) >= 0;
	}

	public boolean isLessThan(MajorVersion other) {
		return isBelow(other);
	}

	public boolean isGreaterThan(MajorVersion other) {
		return isAbove(other);
	}

	public boolean isAny(MajorVersion... versions) {
		if (versions == null) return false;
		for (MajorVersion version : versions) {
			if (this == version) return true;
		}
		return false;
	}

	public static boolean isServerVersion(MajorVersion... versions) {
		return getServerVersion().isAny(versions);
	}

	public static boolean isServerVersionBelow(MajorVersion version) {
		return getServerVersion().isBelow(version);
	}

	public static boolean isServerVersionAtOrBelow(MajorVersion version) {
		return getServerVersion().isAtOrBelow(version);
	}

	public static boolean isServerVersionAbove(MajorVersion version) {
		return getServerVersion().isAbove(version);
	}

	public static boolean isServerVersionAtLeast(MajorVersion version) {
		return getServerVersion().isAtLeast(version);
	}

	private boolean isComparable(MajorVersion other) {
		return other != null && this != UNKNOWN && other != UNKNOWN;
	}

	private int compareNumbers(MajorVersion other) {
		int result = Integer.compare(major, other.major);
		return result != 0 ? result : Integer.compare(minor, other.minor);
	}

	@Override
	public String toString() {
		return this == UNKNOWN ? "unknown" : major + "." + minor;
	}
}

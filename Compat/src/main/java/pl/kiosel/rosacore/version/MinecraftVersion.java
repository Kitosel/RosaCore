package pl.kiosel.rosacore.version;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MinecraftVersion implements Comparable<MinecraftVersion> {

	private static final Pattern MC_MARKER = Pattern.compile(
			"(?i)(?:MC|Minecraft)\\s*[:=]?\\s*(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");
	private static final Pattern LEADING_VERSION = Pattern.compile(
			"^\\s*(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

	private final int major;
	private final int minor;
	private final int patch;

	private MinecraftVersion(int major, int minor, int patch) {
		if (major < 0 || minor < 0 || patch < 0)
			throw new IllegalArgumentException("Version components cannot be negative");

		this.major = major;
		this.minor = minor;
		this.patch = patch;
	}

	public static MinecraftVersion of(int major, int minor, int patch) {
		return new MinecraftVersion(major, minor, patch);
	}

	public static MinecraftVersion parse(String value) {
		Objects.requireNonNull(value, "value");

		Matcher markerMatcher = MC_MARKER.matcher(value);
		if (markerMatcher.find())
			return fromMatcher(markerMatcher);

		Matcher leadingMatcher = LEADING_VERSION.matcher(value);
		if (leadingMatcher.find())
			return fromMatcher(leadingMatcher);

		throw new IllegalArgumentException("Cannot determine Minecraft version from: " + value);
	}

	private static MinecraftVersion fromMatcher(Matcher matcher) {
		return new MinecraftVersion(
				Integer.parseInt(matcher.group(1)),
				parseOptional(matcher.group(2)),
				parseOptional(matcher.group(3))
		);
	}

	private static int parseOptional(String value) {
		return value == null ? 0 : Integer.parseInt(value);
	}

	public int getMajor() {
		return this.major;
	}

	public int getMinor() {
		return this.minor;
	}

	public int getPatch() {
		return this.patch;
	}

	public boolean isAtLeast(MinecraftVersion other) {
		return compareTo(other) >= 0;
	}

	public boolean isOlderThan(MinecraftVersion other) {
		return compareTo(other) < 0;
	}

	@Override
	public int compareTo(MinecraftVersion other) {
		Objects.requireNonNull(other, "other");

		int result = Integer.compare(this.major, other.major);
		if (result != 0)
			return result;

		result = Integer.compare(this.minor, other.minor);
		if (result != 0)
			return result;

		return Integer.compare(this.patch, other.patch);
	}

	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (!(object instanceof MinecraftVersion))
			return false;

		MinecraftVersion other = (MinecraftVersion) object;
		return this.major == other.major
				&& this.minor == other.minor
				&& this.patch == other.patch;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.major, this.minor, this.patch);
	}

	@Override
	public String toString() {
		if (this.patch == 0)
			return this.major + "." + this.minor;

		return this.major + "." + this.minor + "." + this.patch;
	}
}

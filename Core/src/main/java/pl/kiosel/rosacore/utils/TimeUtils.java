package pl.kiosel.rosacore.utils;

import lombok.Getter;
import pl.kiosel.rosacore.RosaLogger;

import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

public class TimeUtils {

	private static final TimeDivision[] DISPLAY_DIVISIONS = {
			TimeDivision.YEAR, TimeDivision.MONTH, TimeDivision.DAY,
			TimeDivision.HOUR, TimeDivision.MINUTE, TimeDivision.SECOND
	};

	private TimeUtils() {
	}

	public static String getStringDate(long time) {
		return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date(time));
	}

	public static Instant positiveOrNullInstant(long time) {
		return time <= 0L ? null : Instant.ofEpochMilli(time);
	}

	public static long convert(long value, char unit) {
		long multiplier;
		switch (Character.toLowerCase(unit)) {
			case 'd':
				multiplier = 86_400_000L;
				break;
			case 'h':
				multiplier = 3_600_000L;
				break;
			case 'm':
				multiplier = 60_000L;
				break;
			case 's':
				multiplier = 1_000L;
				break;
			default:
				return 0;
		}
		try {
			return Math.multiplyExact(value, multiplier);
		} catch (ArithmeticException ignored) {
			return value < 0 ? Long.MIN_VALUE : Long.MAX_VALUE;
		}
	}

	public static long convert(long value, String unit) {
		char charUnit;
		switch (unit.toLowerCase()) {
			case "d":
				charUnit = 'd';
				break;
			case "h":
				charUnit = 'h';
				break;
			case "m":
				charUnit = 'm';
				break;
			default:
				charUnit = 's';
				break;
		}
		return convert(value, charUnit);
	}

	public static Duration getDuration(String unit, Integer number) {
		if (unit == null || number == null) return null;
		Duration duration;
		switch (unit.toLowerCase(Locale.ROOT)) {
			case "d":
			case "day":
			case "days":
				duration = Duration.ofDays(number);
				return duration;
			case "h":
			case "hor":
			case "hour":
			case "hours":
				duration = Duration.ofHours(number);
				return duration;
			case "m":
			case "min":
			case "minute":
			case "minutes":
				duration = Duration.ofMinutes(number);
				return duration;
			case "s":
			case "sec":
			case "second":
			case "seconds":
				duration = Duration.ofSeconds(number);
				return duration;
			default:
				return null;
		}
	}

	public static ZoneId readZoneId(String zoneId) {
		try {
			if (zoneId == null || zoneId.trim().isEmpty()) return ZoneId.systemDefault();
			return ZoneId.of(zoneId);
		} catch (DateTimeException exception) {
			RosaLogger.getInstance().warning("Invalid time-zone '"
					+ zoneId + "'; using system time-zone");
			return ZoneId.systemDefault();
		}
	}

	public static String formatTime(Duration duration) {
		return formatTime(duration, " ", TimeDivision::getFormatted);
	}

	public static String formatTime(Duration duration, TimeUnitFormatter formatter) {
		return formatTime(duration, " ", formatter);
	}

	public static String formatTime(Duration duration, String delimiter, TimeUnitFormatter formatter) {
		if (duration == null) throw new IllegalArgumentException("Duration cannot be null");
		if (delimiter == null) throw new IllegalArgumentException("Delimiter cannot be null");
		if (formatter == null) throw new IllegalArgumentException("Time unit formatter cannot be null");

		long remaining = Math.max(0L, duration.toMillis());
		LinkedHashMap<TimeDivision, Long> parts = new LinkedHashMap<>();
		for (TimeDivision division : DISPLAY_DIVISIONS) {
			long amount = remaining / division.millis;
			remaining -= amount * division.millis;
			parts.put(division, amount);
		}

		StringBuilder result = new StringBuilder();
		for (Map.Entry<TimeDivision, Long> part : parts.entrySet()) {
			if (part.getValue() == 0L) continue;
			String formatted = formatter.format(part.getKey(), part.getValue());
			if (result.length() > 0) result.append(delimiter);
			result.append(formatted == null || formatted.isEmpty()
					? part.getKey().getFormatted(part.getValue()) : formatted);
		}
		if (result.length() > 0) return result.toString();
		String zero = formatter.format(TimeDivision.SECOND, 0L);
		return zero == null || zero.isEmpty() ? TimeDivision.SECOND.getFormatted(0L) : zero;
	}

	public static Duration duration(String raw, Duration fallback, boolean allowZero) {
		if (raw == null) return fallback;
		String normalized = raw.trim().toLowerCase(Locale.ROOT);
		if (allowZero && normalized.matches("0+(?:s|sec|m|min|h|d|w|mo|y)")) return Duration.ZERO;
		try {
			Duration parsed = parseTimeDuration(normalized);
			if (!parsed.isNegative() && (!parsed.isZero() || allowZero)) return parsed;
		} catch (RuntimeException ignored) {
		}
		RosaLogger.getInstance().warning("Invalid duration '" + raw + "'; using " + defaultDuration(fallback));
		return fallback;
	}

	public static String defaultDuration(Duration duration) {
		long seconds = duration.getSeconds();
		if (seconds % 86_400L == 0L) return seconds / 86_400L + "d";
		if (seconds % 3_600L == 0L) return seconds / 3_600L + "h";
		if (seconds % 60L == 0L) return seconds / 60L + "m";
		return seconds + "s";
	}

	public static Duration parseTimeDuration(String value) {
		if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Time cannot be empty");
		String input = value.trim().toLowerCase(Locale.ROOT);
		StringBuilder number = new StringBuilder();
		long millis = 0L;
		for (int index = 0; index < input.length(); ) {
			char character = input.charAt(index);
			if (Character.isDigit(character)) {
				number.append(character);
				index++;
				continue;
			}
			if (Character.isWhitespace(character)) {
				index++;
				continue;
			}
			if (number.length() == 0) throw new IllegalArgumentException("Missing amount in time: " + value);

			TimeDivision matchedDivision = null;
			String matchedAbbreviation = null;
			for (TimeDivision division : TimeDivision.values()) {
				for (String abbreviation : division.abbreviations) {
					if (input.startsWith(abbreviation, index)
							&& (matchedAbbreviation == null || abbreviation.length() > matchedAbbreviation.length())) {
						matchedDivision = division;
						matchedAbbreviation = abbreviation;
					}
				}
			}
			if (matchedDivision == null) throw new IllegalArgumentException("Unknown time unit in: " + value);
			long amount = Long.parseLong(number.toString());
			millis = Math.addExact(millis, Math.multiplyExact(amount, matchedDivision.millis));
			number.setLength(0);
			index += matchedAbbreviation.length();
		}
		if (number.length() != 0) throw new IllegalArgumentException("Missing time unit in: " + value);
		return Duration.ofMillis(millis);
	}

	@FunctionalInterface
	public interface TimeUnitFormatter {
		String format(TimeDivision division, long amount);
	}

	public enum TimeDivision {
		SECOND(1_000L, "second", "seconds", "seconds", "s", "sec"),
		MINUTE(60_000L, "minute", "minutes", "minutes", "m", "min"),
		HOUR(3_600_000L, "hour", "hours", "hours", "h", "godz"),
		DAY(86_400_000L, "day", "days", "days", "d", "dni", "day"),
		WEEK(604_800_000L, "week", "weeks", "weeks", "w", "t", "tyg"),
		MONTH(2_592_000_000L, "month", "months", "months", "mo", "ms", "mc", "mies"),
		YEAR(31_536_000_000L, "year", "years", "years", "y", "r", "l", "lat", "rok");

		@Getter private final long millis;
		private final String singular;
		private final String few;
		private final String many;
		private final String[] abbreviations;

		TimeDivision(long millis, String singular, String few, String many, String... abbreviations) {
			this.millis = millis;
			this.singular = singular;
			this.few = few;
			this.many = many;
			this.abbreviations = abbreviations;
		}

		public String[] getAbbreviations() {
			return Arrays.copyOf(this.abbreviations, this.abbreviations.length);
		}

		public TimeForm getFormType(long amount) {
			if (amount == 1L) return TimeForm.ONE;
			long ones = Math.abs(amount) % 10L;
			long tens = Math.abs(amount) % 100L;
			return ones >= 2L && ones <= 4L && (tens < 12L || tens > 14L) ? TimeForm.FEW : TimeForm.MANY;
		}

		public String getForm(long amount) {
			switch (getFormType(amount)) {
				case ONE:
					return this.singular;
				case FEW:
					return this.few;
				default:
					return this.many;
			}
		}

		public String getFormatted(long amount) {
			return amount + " " + getForm(amount);
		}
	}

	@Getter
	public enum TimeForm {
		ONE("one"), FEW("few"), MANY("many");
		private final String key;

		TimeForm(String key) {
			this.key = key;
		}
	}
}

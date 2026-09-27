package pl.kiosel.rosacore.utils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import pl.kiosel.rosacore.utils.format.RangeFormatting;

public final class NumberRange {

	private static final String BOUNDARY = "(\\*|[-+]?(?:\\d+(?:\\.\\d+)?|\\.\\d+))";
	private static final Pattern RANGE_PATTERN = Pattern.compile(
			"^\\s*" + BOUNDARY + "\\s*(?:\\.\\.|:|-)\\s*" + BOUNDARY + "\\s*$");

	private final double minimum;
	private final double maximum;

	public NumberRange(Number minimum, Number maximum) {
		this(readFinite(minimum, "minimum"), readFinite(maximum, "maximum"));
	}

	public NumberRange(String range) {
		this(parseBoundaries(range));
	}

	private NumberRange(double[] boundaries) {
		this(boundaries[0], boundaries[1]);
	}

	private NumberRange(double minimum, double maximum) {
		if (Double.isNaN(minimum) || Double.isNaN(maximum)) {
			throw new IllegalArgumentException("Range boundaries cannot be NaN");
		}
		if (minimum > maximum) {
			throw new IllegalArgumentException("Minimum cannot be greater than maximum");
		}
		this.minimum = minimum;
		this.maximum = maximum;
	}

	public static NumberRange parse(String range) {
		return new NumberRange(range);
	}

	public static Optional<NumberRange> tryParse(String range) {
		try {
			return Optional.of(parse(range));
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}

	public static NumberRange atLeast(Number minimum) {
		return new NumberRange(readFinite(minimum, "minimum"), Double.POSITIVE_INFINITY);
	}

	public static NumberRange atMost(Number maximum) {
		return new NumberRange(Double.NEGATIVE_INFINITY, readFinite(maximum, "maximum"));
	}

	public static NumberRange all() {
		return new NumberRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	public boolean contains(Number value) {
		Objects.requireNonNull(value, "value");
		double number = value.doubleValue();
		return !Double.isNaN(number) && number >= this.minimum && number <= this.maximum;
	}

	public Number getMinRange() {
		return this.minimum;
	}

	public Number getMaxRange() {
		return this.maximum;
	}

	public Number getMinimum() {
		return this.minimum;
	}

	public Number getMaximum() {
		return this.maximum;
	}

	public boolean isMinimumUnbounded() {
		return this.minimum == Double.NEGATIVE_INFINITY;
	}

	public boolean isMaximumUnbounded() {
		return this.maximum == Double.POSITIVE_INFINITY;
	}

	public static <T> Optional<T> inRange(Number value, Map<NumberRange, T> ranges) {
		Objects.requireNonNull(value, "value");
		Objects.requireNonNull(ranges, "ranges");
		for (Map.Entry<NumberRange, T> entry : ranges.entrySet()) {
			NumberRange range = Objects.requireNonNull(entry.getKey(), "range");
			if (range.contains(value)) {
				return Optional.ofNullable(entry.getValue());
			}
		}
		return Optional.empty();
	}

	public static String inRangeToString(Number value, List<RangeFormatting> ranges) {
		Objects.requireNonNull(ranges, "ranges");
		for (RangeFormatting formatting : ranges) {
			if (formatting != null && formatting.getRange().contains(value)) return formatting.getValue();
		}
		return String.valueOf(value);
	}

	private static double[] parseBoundaries(String range) {
		if (range == null) {
			throw new IllegalArgumentException("Range cannot be null");
		}
		Matcher matcher = RANGE_PATTERN.matcher(range);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid number range: " + range);
		}
		double minimum = parseBoundary(matcher.group(1), true);
		double maximum = parseBoundary(matcher.group(2), false);
		return new double[]{minimum, maximum};
	}

	private static double parseBoundary(String boundary, boolean minimum) {
		if ("*".equals(boundary)) {
			return minimum ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
		}
		try {
			double parsed = Double.parseDouble(boundary);
			if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
				throw new NumberFormatException("non-finite value");
			}
			return parsed;
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Invalid range boundary: " + boundary, exception);
		}
	}

	private static double readFinite(Number value, String name) {
		Objects.requireNonNull(value, name);
		double number = value.doubleValue();
		if (Double.isNaN(number) || Double.isInfinite(number)) {
			throw new IllegalArgumentException(name + " must be a finite number");
		}
		return number;
	}

	private static String printBoundary(double boundary) {
		if (boundary == Double.NEGATIVE_INFINITY || boundary == Double.POSITIVE_INFINITY) {
			return "*";
		}
		return BigDecimal.valueOf(boundary).stripTrailingZeros().toPlainString();
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) return true;
		if (!(object instanceof NumberRange)) return false;
		NumberRange other = (NumberRange) object;
		return Double.compare(this.minimum, other.minimum) == 0
				&& Double.compare(this.maximum, other.maximum) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.minimum, this.maximum);
	}

	@Override
	public String toString() {
		return printBoundary(this.minimum) + "-" + printBoundary(this.maximum);
	}
}

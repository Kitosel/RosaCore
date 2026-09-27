package pl.kiosel.rosacore.config.setting;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public final class SettingConstraints {

    private SettingConstraints() {
    }

    public static <T> SettingConstraint<T> matching(Predicate<T> predicate, String errorMessage) {
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(errorMessage, "errorMessage");
        return value -> predicate.test(value) ? null : errorMessage;
    }

    public static SettingConstraint<String> nonBlank() {
        return matching(value -> value != null && !value.trim().isEmpty(), "Value cannot be blank");
    }

    public static SettingConstraint<Integer> integerRange(int minimum, int maximum) {
        if (minimum > maximum) {
            throw new IllegalArgumentException("minimum cannot be greater than maximum");
        }
        return matching(value -> value >= minimum && value <= maximum,
                "Expected an integer between " + minimum + " and " + maximum);
    }

    public static SettingConstraint<Long> longRange(long minimum, long maximum) {
        if (minimum > maximum) {
            throw new IllegalArgumentException("minimum cannot be greater than maximum");
        }
        return matching(value -> value >= minimum && value <= maximum,
                "Expected a long integer between " + minimum + " and " + maximum);
    }

    public static SettingConstraint<Double> numberRange(double minimum, double maximum) {
        if (Double.isNaN(minimum) || Double.isNaN(maximum) || minimum > maximum) {
            throw new IllegalArgumentException("Invalid number range");
        }
        return matching(value -> !Double.isNaN(value) && !Double.isInfinite(value)
                        && value >= minimum && value <= maximum,
                "Expected a number between " + minimum + " and " + maximum);
    }

    public static SettingConstraint<String> oneOfIgnoreCase(Collection<String> values) {
        Objects.requireNonNull(values, "values");
        Set<String> allowed = new LinkedHashSet<>();
        for (String value : values) {
            allowed.add(Objects.requireNonNull(value, "value").toLowerCase(Locale.ROOT));
        }
        return matching(value -> value != null && allowed.contains(value.toLowerCase(Locale.ROOT)),
                "Expected one of " + values);
    }

    public static SettingConstraint<java.util.List<String>> nonEmptyStringList() {
        return matching(value -> value != null && !value.isEmpty(), "List cannot be empty");
    }
}

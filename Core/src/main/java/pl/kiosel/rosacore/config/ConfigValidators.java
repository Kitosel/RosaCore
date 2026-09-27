package pl.kiosel.rosacore.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class ConfigValidators {

    private ConfigValidators() {
    }

    public static ConfigValidator all(ConfigValidator... validators) {
        Objects.requireNonNull(validators, "validators");
        ConfigValidator[] copy = validators.clone();
        return (config, context) -> {
            for (ConfigValidator validator : copy) {
                Objects.requireNonNull(validator, "validator").validate(config, context);
            }
        };
    }

    public static ConfigValidator required(String... paths) {
        Objects.requireNonNull(paths, "paths");
        String[] copy = paths.clone();
        return (config, context) -> {
            for (String path : copy) {
                if (!config.contains(path)) {
                    context.error(path, "Required value is missing");
                }
            }
        };
    }

    public static ConfigValidator nonBlankString(String path) {
        Objects.requireNonNull(path, "path");
        return (config, context) -> {
            Object value = config.get(path);
            if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
                context.error(path, "Expected a non-blank string");
            }
        };
    }

    public static ConfigValidator integerRange(String path, long minimum, long maximum) {
        Objects.requireNonNull(path, "path");
        if (minimum > maximum) {
            throw new IllegalArgumentException("minimum cannot be greater than maximum");
        }
        return (config, context) -> {
            Object value = config.get(path);
            if (!(value instanceof Number)) {
                context.error(path, "Expected an integer between " + minimum + " and " + maximum);
                return;
            }
            Number number = (Number) value;
            double asDouble = number.doubleValue();
            long asLong = number.longValue();
            if (Double.isNaN(asDouble) || Double.isInfinite(asDouble)
                    || asDouble != (double) asLong || asLong < minimum || asLong > maximum) {
                context.error(path, "Expected an integer between " + minimum + " and " + maximum);
            }
        };
    }

    public static ConfigValidator numberRange(String path, double minimum, double maximum) {
        Objects.requireNonNull(path, "path");
        if (Double.isNaN(minimum) || Double.isNaN(maximum) || minimum > maximum) {
            throw new IllegalArgumentException("Invalid number range");
        }
        return (config, context) -> {
            Object value = config.get(path);
            if (!(value instanceof Number)) {
                context.error(path, "Expected a number between " + minimum + " and " + maximum);
                return;
            }
            double number = ((Number) value).doubleValue();
            if (Double.isNaN(number) || Double.isInfinite(number) || number < minimum || number > maximum) {
                context.error(path, "Expected a number between " + minimum + " and " + maximum);
            }
        };
    }

    public static ConfigValidator oneOfIgnoreCase(String path, String... allowedValues) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(allowedValues, "allowedValues");
        Set<String> allowed = new LinkedHashSet<>();
        for (String value : allowedValues) {
            allowed.add(Objects.requireNonNull(value, "allowed value").toLowerCase(Locale.ROOT));
        }
        Collection<String> displayValues = Arrays.asList(allowedValues.clone());
        return (config, context) -> {
            Object value = config.get(path);
            if (!(value instanceof String)
                    || !allowed.contains(((String) value).toLowerCase(Locale.ROOT))) {
                context.error(path, "Expected one of " + displayValues);
            }
        };
    }
}

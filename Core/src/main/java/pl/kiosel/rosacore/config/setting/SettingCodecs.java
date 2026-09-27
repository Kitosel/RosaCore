package pl.kiosel.rosacore.config.setting;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class SettingCodecs {

    private static final SettingCodec<String> STRING = new SimpleCodec<String>("string") {
        @Override
        public String decode(Object value) {
            if (!(value instanceof String)) {
                throw wrongType("string", value);
            }
            return (String) value;
        }
    };

    private static final SettingCodec<Boolean> BOOLEAN = new SimpleCodec<Boolean>("boolean") {
        @Override
        public Boolean decode(Object value) {
            if (!(value instanceof Boolean)) {
                throw wrongType("boolean", value);
            }
            return (Boolean) value;
        }
    };

    private static final SettingCodec<Integer> INTEGER = new SimpleCodec<Integer>("integer") {
        @Override
        public Integer decode(Object value) {
            if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
                return ((Number) value).intValue();
            }
            if (value instanceof Long) {
                long number = (Long) value;
                if (number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE) {
                    return (int) number;
                }
            } else if (value instanceof BigInteger) {
                BigInteger number = (BigInteger) value;
                if (number.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0
                        && number.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0) {
                    return number.intValue();
                }
            }
            throw wrongType("32-bit integer", value);
        }
    };

    private static final SettingCodec<Long> LONG = new SimpleCodec<Long>("long integer") {
        @Override
        public Long decode(Object value) {
            if (value instanceof Byte || value instanceof Short
                    || value instanceof Integer || value instanceof Long) {
                return ((Number) value).longValue();
            }
            if (value instanceof BigInteger) {
                BigInteger number = (BigInteger) value;
                if (number.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0
                        && number.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0) {
                    return number.longValue();
                }
            }
            throw wrongType("long integer", value);
        }
    };

    private static final SettingCodec<Double> DOUBLE = new SimpleCodec<Double>("number") {
        @Override
        public Double decode(Object value) {
            if (!(value instanceof Number)) {
                throw wrongType("number", value);
            }
            double number = ((Number) value).doubleValue();
            if (Double.isNaN(number) || Double.isInfinite(number)) {
                throw wrongType("finite number", value);
            }
            return number;
        }
    };

    private static final SettingCodec<List<String>> STRING_LIST = new SettingCodec<List<String>>() {
        @Override
        public List<String> decode(Object value) {
            if (!(value instanceof List<?>)) {
                throw wrongType("list of strings", value);
            }
            List<String> strings = new ArrayList<>();
            for (Object element : (List<?>) value) {
                if (!(element instanceof String)) {
                    throw wrongType("list containing only strings", value);
                }
                strings.add((String) element);
            }
            return immutableCopy(strings);
        }

        @Override
        public Object encode(List<String> value) {
            return new ArrayList<>(checkedStrings(value));
        }

        @Override
        public List<String> copy(List<String> value) {
            return immutableCopy(checkedStrings(value));
        }

        @Override
        public String describeExpectedValue() {
            return "list of strings";
        }
    };

    private SettingCodecs() {
    }

    public static SettingCodec<String> string() {
        return STRING;
    }

    public static SettingCodec<Boolean> booleanValue() {
        return BOOLEAN;
    }

    public static SettingCodec<Integer> integer() {
        return INTEGER;
    }

    public static SettingCodec<Long> longInteger() {
        return LONG;
    }

    public static SettingCodec<Double> number() {
        return DOUBLE;
    }

    public static SettingCodec<List<String>> stringList() {
        return STRING_LIST;
    }

    public static <E extends Enum<E>> SettingCodec<E> enumeration(Class<E> enumType) {
        Objects.requireNonNull(enumType, "enumType");
        return new SimpleCodec<E>("one of " + java.util.Arrays.toString(enumType.getEnumConstants())) {
            @Override
            public E decode(Object value) {
                if (!(value instanceof String)) {
                    throw wrongType(this.describeExpectedValue(), value);
                }
                String configured = ((String) value).trim();
                for (E constant : enumType.getEnumConstants()) {
                    if (constant.name().toLowerCase(Locale.ROOT)
                            .equals(configured.toLowerCase(Locale.ROOT))) {
                        return constant;
                    }
                }
                throw wrongType(this.describeExpectedValue(), value);
            }

            @Override
            public Object encode(E value) {
                return Objects.requireNonNull(value, "value").name();
            }
        };
    }

    private static IllegalArgumentException wrongType(String expected, Object value) {
        String actual = value == null ? "null" : value.getClass().getSimpleName();
        return new IllegalArgumentException("Expected " + expected + ", got " + actual);
    }

    private static List<String> checkedStrings(List<String> values) {
        Objects.requireNonNull(values, "value");
        for (String value : values) {
            Objects.requireNonNull(value, "list element");
        }
        return values;
    }

    private static List<String> immutableCopy(List<String> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private abstract static class SimpleCodec<T> implements SettingCodec<T> {

        private final String description;

        private SimpleCodec(String description) {
            this.description = description;
        }

        @Override
        public Object encode(T value) {
            return Objects.requireNonNull(value, "value");
        }

        @Override
        public String describeExpectedValue() {
            return this.description;
        }
    }
}

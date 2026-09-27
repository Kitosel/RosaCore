package pl.kiosel.rosacore.message;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class MessagePlaceholders {

    private static final MessagePlaceholders EMPTY = new MessagePlaceholders(Collections.emptyMap());

    private final Map<String, String> values;

    private MessagePlaceholders(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static MessagePlaceholders empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MessagePlaceholders of(String name, Object value) {
        return builder().put(name, value).build();
    }

    public static MessagePlaceholders pairs(Object... nameValuePairs) {
        Objects.requireNonNull(nameValuePairs, "nameValuePairs");
        if (nameValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Placeholders must be provided as name/value pairs");
        }
        Builder builder = builder();
        for (int index = 0; index < nameValuePairs.length; index += 2) {
            builder.put(String.valueOf(nameValuePairs[index]), nameValuePairs[index + 1]);
        }
        return builder.build();
    }

    public boolean contains(String name) {
        return this.values.containsKey(name);
    }

    public String get(String name) {
        return this.values.get(name);
    }

    public Map<String, String> asMap() {
        return this.values;
    }

    public MessagePlaceholders with(String name, Object value) {
        Map<String, String> copy = new LinkedHashMap<>(this.values);
        copy.put(requireName(name), stringify(value));
        return new MessagePlaceholders(copy);
    }

    private static String requireName(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid placeholder name: " + name);
        }
        return name;
    }

    private static String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static final class Builder {

        private final Map<String, String> values = new LinkedHashMap<>();

        public Builder put(String name, Object value) {
            this.values.put(requireName(name), stringify(value));
            return this;
        }

        public Builder putAll(Map<String, ?> values) {
            Objects.requireNonNull(values, "values");
            for (Map.Entry<String, ?> entry : values.entrySet()) {
                this.put(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public MessagePlaceholders build() {
            if (this.values.isEmpty()) {
                return EMPTY;
            }
            return new MessagePlaceholders(this.values);
        }
    }
}

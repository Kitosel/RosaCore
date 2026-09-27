package pl.kiosel.rosacore.config.setting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class SettingsSnapshot {

    private final SettingsSchema schema;
    private final Map<SettingKey<?>, Object> values;

    SettingsSnapshot(SettingsSchema schema, Map<SettingKey<?>, Object> values) {
        this.schema = Objects.requireNonNull(schema, "schema");
        this.values = Collections.unmodifiableMap(new java.util.IdentityHashMap<>(values));
    }

    public <T> T get(SettingKey<T> setting) {
        Objects.requireNonNull(setting, "setting");
        if (!this.schema.contains(setting)) {
            throw new IllegalArgumentException("Setting does not belong to this schema: " + setting.getPath());
        }
        Object value = this.values.get(setting);
        @SuppressWarnings("unchecked")
        T typed = (T) value;
        return setting.copy(typed);
    }

    public Map<String, Object> asMap() {
        Map<String, Object> byPath = new LinkedHashMap<>();
        for (SettingKey<?> setting : this.schema.getSettings()) {
            byPath.put(setting.getPath(), this.copyUntyped(setting));
        }
        return Collections.unmodifiableMap(byPath);
    }

    public Map<String, Object> asDisplayMap() {
        Map<String, Object> byPath = new LinkedHashMap<>();
        for (SettingKey<?> setting : this.schema.getSettings()) {
            byPath.put(setting.getPath(), setting.isSensitive() ? "<redacted>" : this.copyUntyped(setting));
        }
        return Collections.unmodifiableMap(byPath);
    }

    private Object copyUntyped(SettingKey<?> setting) {
        return copyCaptured(setting, this.values.get(setting));
    }

    private static <T> T copyCaptured(SettingKey<T> setting, Object value) {
        @SuppressWarnings("unchecked")
        T typed = (T) value;
        return setting.copy(typed);
    }
}

package pl.kiosel.rosacore.compatibility;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class NameAliases {

	private final Map<String, String> legacyByCanonical = new HashMap<>();
	private final Map<String, String> canonicalByLegacy = new HashMap<>();

	NameAliases add(String canonical, String legacy) {
		String normalizedCanonical = normalize(canonical);
		String normalizedLegacy = normalize(legacy);
		this.legacyByCanonical.putIfAbsent(normalizedCanonical, normalizedLegacy);
		this.canonicalByLegacy.put(normalizedLegacy, normalizedCanonical);
		return this;
	}

	String canonical(String input) {
		String normalized = normalize(input);
		String canonical = this.canonicalByLegacy.get(normalized);
		return canonical == null ? normalized : canonical;
	}

	String legacy(String canonical) {
		return this.legacyByCanonical.get(canonical);
	}

	static String normalize(String input) {
		Objects.requireNonNull(input, "input");
		String normalized = input.trim();
		if (normalized.regionMatches(true, 0, "minecraft:", 0, "minecraft:".length())) {
			normalized = normalized.substring("minecraft:".length());
		}
		normalized = normalized.replace('-', '_').replace(' ', '_').replace('.', '_')
				.toUpperCase(Locale.ROOT);
		if (normalized.isEmpty() || !normalized.matches("[A-Z0-9_]+")) {
			throw new IllegalArgumentException("Invalid compatibility name: " + input);
		}
		return normalized;
	}

	static <T> T staticValue(Class<T> type, String name) {
		try {
			Object value = type.getField(name).get(null);
			return type.isInstance(value) ? type.cast(value) : null;
		} catch (ReflectiveOperationException | LinkageError exception) {
			return null;
		}
	}

	static String runtimeName(Class<?> type, Object value) {
		Objects.requireNonNull(value, "value");
		if (value instanceof Enum<?>) {
			return ((Enum<?>) value).name();
		}
		try {
			Method method = type.getMethod("name");
			Object name = method.invoke(value);
			if (name instanceof String) {
				return (String) name;
			}
		} catch (ReflectiveOperationException | LinkageError ignored) {
			// Some older registry values expose only public constants.
		}
		for (Field field : type.getFields()) {
			if (!type.isAssignableFrom(field.getType())) {
				continue;
			}
			try {
				if (field.get(null) == value) {
					return field.getName();
				}
			} catch (ReflectiveOperationException | LinkageError ignored) {
				// Try the regular name method below.
			}
		}
		throw new IllegalArgumentException("Cannot read runtime name from " + type.getName());
	}
}

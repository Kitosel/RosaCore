package pl.kiosel.rosacore.compatibility;

import java.util.Optional;

final class ZCompatibility {

	private ZCompatibility() {
	}

	static <E extends Enum<E>> Optional<E> byName(Class<E> type, String name) {
		if (name == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(Enum.valueOf(type, name));
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}

	static <E extends Enum<E>> E require(Class<E> type, String input, Optional<E> matched) {
		return matched.orElseThrow(() -> new IllegalArgumentException(
				"Unknown " + type.getSimpleName() + " value: " + input));
	}
}

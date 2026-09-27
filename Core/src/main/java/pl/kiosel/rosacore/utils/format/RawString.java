package pl.kiosel.rosacore.utils.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RawString {

	private final String value;

	public RawString(String value) {
		this.value = Objects.requireNonNull(value, "value");
	}

	public String getValue() {
		return this.value;
	}

	public boolean isEmpty() {
		return this.value.isEmpty();
	}

	public String replace(String from, String to) {
		return this.value.replace(from, to);
	}

	@Override
	public String toString() {
		return this.value;
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof RawString && this.value.equals(((RawString) object).value);
	}

	public static List<RawString> listOf(String... values) {
		List<RawString> result = new ArrayList<>();
		for (String value : values) result.add(new RawString(value));
		return result;
	}
}

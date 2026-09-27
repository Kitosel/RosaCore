package pl.kiosel.rosacore.utils.format;

import lombok.Getter;
import pl.kiosel.rosacore.utils.NumberRange;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
public final class RangeFormatting {

	private final NumberRange range;
	private String value;

	public RangeFormatting(NumberRange range, String value) {
		this.range = Objects.requireNonNull(range, "range");
		this.value = Objects.requireNonNull(value, "value");
	}

	public RangeFormatting(Number minimum, Number maximum, String value) {
		this(new NumberRange(minimum, maximum), value);
	}

	public RangeFormatting(String serialized) {
		String[] parts = Objects.requireNonNull(serialized, "serialized").trim().split("\\s+", 2);
		if (parts.length != 2) throw new IllegalArgumentException("Invalid range formatting: " + serialized);
		this.range = new NumberRange(parts[0]);
		this.value = parts[1];
	}

	public void setValue(String value) {
		this.value = Objects.requireNonNull(value, "value");
	}

	public static Map<NumberRange, String> toRangeMap(List<RangeFormatting> values) {
		Map<NumberRange, String> result = new LinkedHashMap<>();
		for (RangeFormatting value : values) result.put(value.getRange(), value.getValue());
		return result;
	}

	@Override
	public String toString() {
		return this.range + " " + this.value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.range, this.value);
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof RangeFormatting)) return false;
		RangeFormatting other = (RangeFormatting) object;
		return this.range.equals(other.range) && this.value.equals(other.value);
	}
}

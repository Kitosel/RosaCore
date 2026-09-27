package pl.kiosel.rosacore.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class RangeFormatter {

	private final String placeholder;
	private final String fallback;
	private final Map<NumberRange, String> formats;

	private RangeFormatter(Builder builder) {
		this.placeholder = builder.placeholder;
		this.fallback = builder.fallback;
		this.formats = Collections.unmodifiableMap(new LinkedHashMap<>(builder.formats));
	}

	public static Builder builder() {
		return new Builder("%value%");
	}

	public static Builder builder(String placeholder) {
		return new Builder(placeholder);
	}

	public Optional<String> findFormat(Number value) {
		return NumberRange.inRange(value, this.formats);
	}

	public String format(Number value) {
		Objects.requireNonNull(value, "value");
		return this.format(value, value);
	}

	public String format(Number value, Object displayedValue) {
		Objects.requireNonNull(value, "value");
		Objects.requireNonNull(displayedValue, "displayedValue");
		String selected = this.findFormat(value).orElse(this.fallback);
		return selected.replace(this.placeholder, String.valueOf(displayedValue));
	}

	public String getPlaceholder() {
		return this.placeholder;
	}

	public String getFallback() {
		return this.fallback;
	}

	public Map<NumberRange, String> getFormats() {
		return this.formats;
	}

	public static final class Builder {

		private final String placeholder;
		private final Map<NumberRange, String> formats = new LinkedHashMap<>();
		private String fallback;

		private Builder(String placeholder) {
			if (placeholder == null || placeholder.isEmpty()) {
				throw new IllegalArgumentException("placeholder cannot be empty");
			}
			this.placeholder = placeholder;
			this.fallback = placeholder;
		}

		public Builder range(Number minimum, Number maximum, String format) {
			return this.range(new NumberRange(minimum, maximum), format);
		}

		public Builder range(String range, String format) {
			return this.range(NumberRange.parse(range), format);
		}

		public Builder range(NumberRange range, String format) {
			Objects.requireNonNull(range, "range");
			Objects.requireNonNull(format, "format");
			if (this.formats.containsKey(range)) {
				throw new IllegalArgumentException("Duplicate range: " + range);
			}
			this.formats.put(range, format);
			return this;
		}

		public Builder fallback(String fallback) {
			this.fallback = Objects.requireNonNull(fallback, "fallback");
			return this;
		}

		public RangeFormatter build() {
			return new RangeFormatter(this);
		}
	}
}

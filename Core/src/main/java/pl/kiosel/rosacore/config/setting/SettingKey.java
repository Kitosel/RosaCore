package pl.kiosel.rosacore.config.setting;

import lombok.Getter;
import pl.kiosel.rosacore.config.ConfigView;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class SettingKey<T> {

	@Getter
	private final String path;
	private final SettingCodec<T> codec;
	private final T defaultValue;
	private final List<SettingConstraint<T>> constraints;
	@Getter
	private final List<String> description;
	@Getter
	private final boolean sensitive;
	@Getter private final List<String> comments;
	@Getter private final boolean editable;
	private final AtomicReference<SettingsSnapshot> boundSnapshot = new AtomicReference<>();

	private SettingKey(Builder<T> builder) {
		this.path = builder.path;
		this.codec = builder.codec;
		this.defaultValue = builder.codec.copy(builder.defaultValue);
		this.constraints = Collections.unmodifiableList(new ArrayList<>(builder.constraints));
		this.description = Collections.unmodifiableList(new ArrayList<>(builder.description));
		this.sensitive = builder.sensitive;
		this.comments = Collections.unmodifiableList(new ArrayList<>(builder.comments));
		this.editable = builder.editable;
	}

	public static <T> Builder<T> builder(String path, SettingCodec<T> codec, T defaultValue) {
		return new Builder<>(path, codec, defaultValue);
	}

	public static Builder<String> string(String path, String defaultValue) {
		return builder(path, SettingCodecs.string(), defaultValue);
	}

	public static Builder<Boolean> booleanValue(String path, boolean defaultValue) {
		return builder(path, SettingCodecs.booleanValue(), defaultValue);
	}

	public static Builder<Integer> integer(String path, int defaultValue) {
		return builder(path, SettingCodecs.integer(), defaultValue);
	}

	public static Builder<Long> longInteger(String path, long defaultValue) {
		return builder(path, SettingCodecs.longInteger(), defaultValue);
	}

	public static Builder<Double> number(String path, double defaultValue) {
		return builder(path, SettingCodecs.number(), defaultValue);
	}

	public static Builder<List<String>> stringList(String path, List<String> defaultValue) {
		return builder(path, SettingCodecs.stringList(), defaultValue);
	}

	public static <E extends Enum<E>> Builder<E> enumeration(String path, Class<E> type, E defaultValue) {
		return builder(path, SettingCodecs.enumeration(type), defaultValue);
	}

	public T getDefaultValue() {
		return this.codec.copy(this.defaultValue);
	}

	public String getExpectedValueDescription() {
		return this.codec.describeExpectedValue();
	}

	public T get() {
		SettingsSnapshot snapshot = boundSnapshot.get();
		if (snapshot == null) throw new IllegalStateException("SettingKey '" + path + "' is not loaded");
		return snapshot.get(this);
	}

	public List<String> getList() {
		Object value = this.get();
		if (!(value instanceof List<?>)) {
			throw new IllegalStateException("SettingKey '" + path + "' does not contain a list");
		}
		List<?> values = (List<?>) value;
		List<String> strings = new ArrayList<>(values.size());
		for (Object element : values) {
			if (!(element instanceof String)) {
				throw new IllegalStateException("SettingKey '" + path + "' does not contain a list of strings");
			}
			strings.add((String) element);
		}
		return Collections.unmodifiableList(strings);
	}

	public int getInt() {
		return ((Number) get()).intValue();
	}

	public long getLong() {
		return ((Number) get()).longValue();
	}

	public double getDouble() {
		return ((Number) get()).doubleValue();
	}

	public float getFloat() {
		return ((Number) get()).floatValue();
	}

	public boolean getBoolean() {
		return (Boolean) get();
	}

	public String getString() {
		return String.valueOf(get());
	}

	public T decodeInput(String input) throws SettingValueException {
		Objects.requireNonNull(input, "input");
		try {
			Object raw;
			if (defaultValue instanceof Integer) raw = Integer.valueOf(input.trim());
			else if (defaultValue instanceof Long) raw = Long.valueOf(input.trim());
			else if (defaultValue instanceof Double) raw = Double.valueOf(input.trim());
			else if (defaultValue instanceof Boolean) {
				String normalized = input.trim();
				if (!"true".equalsIgnoreCase(normalized) && !"false".equalsIgnoreCase(normalized)) {
					throw new IllegalArgumentException("Expected true or false");
				}
				raw = Boolean.valueOf(normalized);
			} else if (defaultValue instanceof List<?>) {
				List<String> values = new ArrayList<>();
				if (!input.trim().isEmpty()) values.addAll(Arrays.asList(input.split(",", -1)));
				raw = values;
			} else raw = input;
			T value = codec.decode(raw);
			validate(value);
			return codec.copy(value);
		} catch (IllegalArgumentException exception) {
			throw new SettingValueException(path, exception.getMessage(), exception);
		}
	}

	void bind(SettingsSnapshot snapshot) {
		boundSnapshot.set(snapshot);
	}

	void validateValue(T value) throws SettingValueException {
		validate(Objects.requireNonNull(value, "value"));
	}

	Object encodeValue(T value) {
		return codec.encode(value);
	}

	T read(ConfigView config) throws SettingValueException {
		Objects.requireNonNull(config, "config");
		T value;
		if (!config.contains(this.path)) {
			value = this.getDefaultValue();
		} else {
			Object raw = config.get(this.path);
			try {
				value = this.codec.decode(raw);
			} catch (IllegalArgumentException exception) {
				throw new SettingValueException(this.path, exception.getMessage(), exception);
			}
		}
		this.validate(value);
		return this.codec.copy(value);
	}

	Object encodedDefaultValue() {
		return this.codec.encode(this.getDefaultValue());
	}

	T copy(T value) {
		return this.codec.copy(value);
	}

	private void validate(T value) throws SettingValueException {
		for (SettingConstraint<T> constraint : this.constraints) {
			String problem;
			try {
				problem = constraint.validate(value);
			} catch (RuntimeException exception) {
				throw new SettingValueException(this.path,
						"Constraint failed: " + exception.getClass().getSimpleName(), exception);
			}
			if (problem != null) {
				throw new SettingValueException(this.path, problem);
			}
		}
	}

	public static final class Builder<T> {

		private final String path;
		private final SettingCodec<T> codec;
		private final T defaultValue;
		private final List<SettingConstraint<T>> constraints = new ArrayList<>();
		private final List<String> description = new ArrayList<>();
		private final List<String> comments = new ArrayList<>();
		private boolean sensitive;
		private boolean editable = true;

		private Builder(String path, SettingCodec<T> codec, T defaultValue) {
			if (path == null || path.trim().isEmpty()) {
				throw new IllegalArgumentException("path cannot be blank");
			}
			if (path.startsWith(".") || path.endsWith(".") || path.contains("..")) {
				throw new IllegalArgumentException("path contains an empty section: " + path);
			}
			this.path = path;
			this.codec = Objects.requireNonNull(codec, "codec");
			this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
		}

		public Builder<T> constraint(SettingConstraint<T> constraint) {
			this.constraints.add(Objects.requireNonNull(constraint, "constraint"));
			return this;
		}

		public Builder<T> description(String... lines) {
			Objects.requireNonNull(lines, "lines");
			for (String line : lines) {
				this.description.add(Objects.requireNonNull(line, "description line"));
			}
			return this;
		}

		public Builder<T> sensitive(boolean sensitive) {
			this.sensitive = sensitive;
			return this;
		}

		public Builder<T> comment(String... lines) {
			for (String line : lines) comments.add(Objects.requireNonNull(line, "comment line"));
			return this;
		}

		public Builder<T> editable(boolean editable) {
			this.editable = editable;
			return this;
		}

		public SettingKey<T> build() {
			SettingKey<T> key = new SettingKey<>(this);
			try {
				key.validate(key.defaultValue);
			} catch (SettingValueException exception) {
				throw new IllegalStateException("Invalid default for " + this.path + ": "
						+ exception.getMessage(), exception);
			}
			return key;
		}
	}
}

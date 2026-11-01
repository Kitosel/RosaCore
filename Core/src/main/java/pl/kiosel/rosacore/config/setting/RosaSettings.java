package pl.kiosel.rosacore.config.setting;

import lombok.Getter;
import pl.kiosel.rosacore.config.ConfigLoadResult;
import pl.kiosel.rosacore.config.ConfigSaveResult;
import pl.kiosel.rosacore.config.ReloadableConfig;
import pl.kiosel.rosacore.config.RosaConfig;

import java.nio.file.Path;
import java.util.Objects;

public final class RosaSettings implements ReloadableConfig {

	@Getter
	private final RosaConfig config;
	@Getter
	private final SettingsSchema schema;
	private volatile SettingsSnapshot active;

	private RosaSettings(RosaConfig config, SettingsSchema schema) {
		this.config = config;
		this.schema = schema;
	}

	public static RosaSettings create(RosaConfig.Builder configBuilder, SettingsSchema schema) {
		Objects.requireNonNull(configBuilder, "configBuilder");
		Objects.requireNonNull(schema, "schema");
		RosaConfig config = configBuilder.build();
		config.attachSettingsSchema(schema);
		return new RosaSettings(config, schema);
	}

	public static RosaSettings create(RosaConfig config, SettingsSchema schema) {
		Objects.requireNonNull(config, "configBuilder");
		Objects.requireNonNull(schema, "schema");
		config.attachSettingsSchema(schema);
		return new RosaSettings(config, schema);
	}

	public synchronized ConfigLoadResult load() {
		return this.reload();
	}

	@Override
	public synchronized ConfigLoadResult reload() {
		ConfigLoadResult result = this.config.reload();
		if (result.isSuccess()) {
			this.schema.applyMissingDefaults(this.config);
			ConfigSaveResult saved = this.config.save();
			if (!saved.isSuccess()) {
				return ConfigLoadResult.failed(this.config.getPath().toString(),
						"Could not write settings defaults and comments", saved.getCause());
			}
			this.active = this.readSnapshot();
		}
		return result;
	}

	public <T> T get(SettingKey<T> setting) {
		return this.requireSnapshot().get(setting);
	}

	public SettingsSnapshot snapshot() {
		return this.requireSnapshot();
	}

	public synchronized <T> ConfigSaveResult set(SettingKey<T> setting, T value) {
		Objects.requireNonNull(setting, "setting");
		Objects.requireNonNull(value, "value");
		if (!this.schema.contains(setting)) {
			throw new IllegalArgumentException("Setting does not belong to this schema: " + setting.getPath());
		}
		try {
			setting.validateValue(value);
		} catch (SettingValueException exception) {
			throw new IllegalArgumentException(exception.getMessage(), exception);
		}
		this.config.set(setting.getPath(), setting.encodeValue(value));
		ConfigSaveResult result = this.config.save();
		if (result.isSuccess()) this.active = this.readSnapshot();
		return result;
	}

	public synchronized ConfigSaveResult setInput(SettingKey<?> setting, String input) throws SettingValueException {
		Objects.requireNonNull(setting, "setting");
		if (!this.schema.contains(setting)) {
			throw new IllegalArgumentException("Setting does not belong to this schema: " + setting.getPath());
		}
		return decodeAndSet(setting, input);
	}

	private <T> ConfigSaveResult decodeAndSet(SettingKey<T> setting, String input)
			throws SettingValueException {
		return this.set(setting, setting.decodeInput(input));
	}

	public synchronized ConfigSaveResult writeMissingDefaults() {
		this.requireSnapshot();
		int changes = this.schema.applyMissingDefaults(this.config);
		if (changes == 0) {
			return ConfigSaveResult.success();
		}
		ConfigSaveResult result = this.config.save();
		if (result.isSuccess()) {
			this.active = this.readSnapshot();
		}
		return result;
	}

	@Override
	public boolean isLoaded() {
		return this.active != null;
	}

	@Override
	public Path getPath() {
		return this.config.getPath();
	}

	private SettingsSnapshot readSnapshot() {
		try {
			SettingsSnapshot snapshot = this.schema.read(this.config.snapshot());
			for (SettingKey<?> setting : this.schema.getSettings()) setting.bind(snapshot);
			return snapshot;
		} catch (SettingValueException exception) {
			throw new IllegalStateException("Validated setting could not be read: "
					+ exception.getPath(), exception);
		}
	}

	private SettingsSnapshot requireSnapshot() {
		SettingsSnapshot snapshot = this.active;
		if (snapshot == null) {
			throw new IllegalStateException("Settings have not been loaded");
		}
		return snapshot;
	}
}

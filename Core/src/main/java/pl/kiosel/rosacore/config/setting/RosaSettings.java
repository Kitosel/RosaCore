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
		RosaConfig config = configBuilder.validator(schema.validator()).build();
		return new RosaSettings(config, schema);
	}

	public synchronized ConfigLoadResult load() {
		return this.reload();
	}

	@Override
	public synchronized ConfigLoadResult reload() {
		ConfigLoadResult result = this.config.reload();
		if (result.isSuccess()) {
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
			return this.schema.read(this.config.snapshot());
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

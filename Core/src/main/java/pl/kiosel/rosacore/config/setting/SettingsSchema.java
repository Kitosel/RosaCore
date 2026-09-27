package pl.kiosel.rosacore.config.setting;

import lombok.Getter;
import pl.kiosel.rosacore.config.ConfigValidator;
import pl.kiosel.rosacore.config.ConfigView;
import pl.kiosel.rosacore.config.MutableConfig;

import java.util.*;

public final class SettingsSchema {

	@Getter
	private final List<SettingKey<?>> settings;
	private final Set<SettingKey<?>> identities;

	private SettingsSchema(Builder builder) {
		this.settings = Collections.unmodifiableList(new ArrayList<>(builder.settings.values()));
		Set<SettingKey<?>> identities = Collections.newSetFromMap(new IdentityHashMap<>());
		identities.addAll(this.settings);
		this.identities = Collections.unmodifiableSet(identities);
	}

	public static Builder builder() {
		return new Builder();
	}

	public boolean contains(SettingKey<?> key) {
		return this.identities.contains(key);
	}

	public ConfigValidator validator() {
		return (config, context) -> {
			for (SettingKey<?> setting : this.settings) {
				try {
					setting.read(config);
				} catch (SettingValueException exception) {
					context.error(exception.getPath(), exception.getMessage());
				}
			}
		};
	}

	public SettingsSnapshot read(ConfigView config) throws SettingValueException {
		Objects.requireNonNull(config, "config");
		Map<SettingKey<?>, Object> values = new IdentityHashMap<>();
		for (SettingKey<?> setting : this.settings) {
			values.put(setting, setting.read(config));
		}
		return new SettingsSnapshot(this, values);
	}

	public int applyMissingDefaults(MutableConfig config) {
		Objects.requireNonNull(config, "config");
		int applied = 0;
		for (SettingKey<?> setting : this.settings) {
			if (!config.isLocallySet(setting.getPath())) {
				config.set(setting.getPath(), setting.encodedDefaultValue());
				applied++;
			}
		}
		return applied;
	}

	public static final class Builder {

		private final Map<String, SettingKey<?>> settings = new LinkedHashMap<>();

		public Builder add(SettingKey<?> setting) {
			Objects.requireNonNull(setting, "setting");
			for (String existingPath : this.settings.keySet()) {
				if (existingPath.startsWith(setting.getPath() + ".")
						|| setting.getPath().startsWith(existingPath + ".")) {
					throw new IllegalArgumentException("Conflicting setting paths: "
							+ existingPath + " and " + setting.getPath());
				}
			}
			SettingKey<?> previous = this.settings.put(setting.getPath(), setting);
			if (previous != null) {
				this.settings.put(previous.getPath(), previous);
				throw new IllegalArgumentException("Duplicate setting path: " + setting.getPath());
			}
			return this;
		}

		public Builder addAll(SettingKey<?>... settings) {
			Objects.requireNonNull(settings, "settings");
			for (SettingKey<?> setting : settings) {
				this.add(setting);
			}
			return this;
		}

		public SettingsSchema build() {
			return new SettingsSchema(this);
		}
	}
}

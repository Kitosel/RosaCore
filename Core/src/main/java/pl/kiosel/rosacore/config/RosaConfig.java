package pl.kiosel.rosacore.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class RosaConfig implements MutableConfig, ReloadableConfig {

	private final Plugin plugin;
	private final Path dataDirectory;
	private final Path relativePath;
	private final Path file;
	private final String defaultsResource;
	private final String versionPath;
	@Getter private final int targetVersion;
	private final boolean backupMigrations;
	private final Map<Integer, MigrationRegistration> migrations;
	private final List<ConfigValidator> validators;

	private YamlConfiguration active = new YamlConfiguration();
	private YamlConfiguration activeDefaults;
	private int activeVersion;
	private boolean loaded;
	private boolean dirty;

	public RosaConfig(Plugin plugin, String relativePath) {
		this(new Builder(plugin, relativePath));
	}

	private RosaConfig(Builder builder) {
		this.plugin = builder.plugin;
		this.dataDirectory = builder.dataDirectory;
		this.relativePath = builder.relativePath;
		this.file = builder.file;
		this.defaultsResource = builder.defaultsResource;
		this.versionPath = builder.versionPath;
		this.targetVersion = builder.targetVersion;
		this.backupMigrations = builder.backupMigrations;
		this.migrations = Collections.unmodifiableMap(new LinkedHashMap<>(builder.migrations));
		this.validators = Collections.unmodifiableList(new ArrayList<>(builder.validators));
	}

	public static Builder builder(Plugin plugin, String relativePath) {
		return new Builder(plugin, relativePath);
	}

	public synchronized ConfigLoadResult load() {
		return this.reload();
	}

	@Override
	public synchronized ConfigLoadResult reload() {
		int previousVersion = this.loaded ? this.activeVersion : 0;
		try {
			Candidate candidate = this.readCandidate();
			VersionRead versionRead = candidate.version;
			if (!versionRead.isValid()) {
				return ConfigLoadResult.failure(previousVersion, previousVersion,
						Collections.singletonList(versionRead.problem), null);
			}

			int sourceVersion = versionRead.version;
			if (sourceVersion > this.targetVersion) {
				ConfigProblem problem = new ConfigProblem(this.versionPath,
						"Configuration version " + sourceVersion + " is newer than supported version "
								+ this.targetVersion);
				return ConfigLoadResult.failure(sourceVersion, sourceVersion,
						Collections.singletonList(problem), null);
			}

			boolean migrated = false;
			int currentVersion = sourceVersion;
			YamlConfigView mutable = new YamlConfigView(candidate.configuration);
			while (currentVersion < this.targetVersion) {
				MigrationRegistration migration = this.migrations.get(currentVersion);
				if (migration == null) {
					ConfigProblem problem = new ConfigProblem(this.versionPath,
							"Missing migration from version " + currentVersion + " to " + this.targetVersion);
					return ConfigLoadResult.failure(sourceVersion, currentVersion,
							Collections.singletonList(problem), null);
				}

				try {
					migration.action.migrate(mutable);
				} catch (Exception exception) {
					ConfigProblem problem = new ConfigProblem(this.versionPath,
							"Migration " + currentVersion + " -> " + migration.toVersion + " failed");
					return ConfigLoadResult.failure(sourceVersion, currentVersion,
							Collections.singletonList(problem), exception);
				}
				currentVersion = migration.toVersion;
				candidate.configuration.set(this.versionPath, currentVersion);
				migrated = true;
			}

			List<ConfigProblem> problems = this.validate(candidate.configuration);
			if (!problems.isEmpty()) {
				return ConfigLoadResult.failure(sourceVersion, currentVersion, problems, null);
			}

			Path backup = null;
			if (migrated && this.backupMigrations && Files.isRegularFile(this.file)) {
				backup = this.createMigrationBackup(sourceVersion, currentVersion);
			}

			if (!Files.isRegularFile(this.file)) {
				if (!migrated && candidate.bundledBytes != null) {
					this.atomicWrite(candidate.bundledBytes);
				} else {
					this.atomicWrite(candidate.configuration.saveToString().getBytes(StandardCharsets.UTF_8));
				}
			} else if (migrated) {
				this.atomicWrite(candidate.configuration.saveToString().getBytes(StandardCharsets.UTF_8));
			}

			this.active = candidate.configuration;
			this.activeDefaults = candidate.defaults;
			this.activeVersion = currentVersion;
			this.loaded = true;
			this.dirty = false;
			return ConfigLoadResult.success(sourceVersion, currentVersion, backup);
		} catch (IOException | InvalidConfigurationException exception) {
			ConfigProblem problem = new ConfigProblem(this.relativePath.toString(),
					"Could not load configuration");
			return ConfigLoadResult.failure(previousVersion, previousVersion,
					Collections.singletonList(problem), exception);
		}
	}

	public synchronized ConfigSaveResult save() {
		this.ensureLoaded();
		List<ConfigProblem> problems = this.validate(this.active);
		if (!problems.isEmpty()) {
			return ConfigSaveResult.failure(problems, null);
		}
		try {
			this.atomicWrite(this.active.saveToString().getBytes(StandardCharsets.UTF_8));
			this.dirty = false;
			return ConfigSaveResult.success();
		} catch (IOException exception) {
			return ConfigSaveResult.failure(Collections.singletonList(
					new ConfigProblem(this.relativePath.toString(), "Could not save configuration")), exception);
		}
	}

	public synchronized ConfigView snapshot() {
		this.ensureLoaded();
		try {
			YamlConfiguration copy = copyOf(this.active);
			if (this.activeDefaults != null) {
				copy.setDefaults(copyOf(this.activeDefaults));
			}
			return new YamlConfigView(copy);
		} catch (InvalidConfigurationException exception) {
			throw new IllegalStateException("Could not create configuration snapshot", exception);
		}
	}

	@Override
	public synchronized boolean isLoaded() {
		return this.loaded;
	}

	public synchronized boolean isDirty() {
		return this.dirty;
	}

	public File getFile() {
		return this.file.toFile();
	}

	@Override
	public Path getPath() {
		return this.file;
	}

	@Override
	public synchronized Object get(String path) {
		this.ensureLoaded();
		return this.active.get(path);
	}

	@Override
	public synchronized Object get(String path, Object fallback) {
		this.ensureLoaded();
		return this.active.get(path, fallback);
	}

	@Override
	public synchronized boolean contains(String path) {
		this.ensureLoaded();
		return this.active.contains(path);
	}

	@Override
	public synchronized boolean isSet(String path) {
		this.ensureLoaded();
		return this.active.isSet(path);
	}

	@Override
	public synchronized boolean isLocallySet(String path) {
		this.ensureLoaded();
		return this.active.getValues(true).containsKey(path);
	}

	@Override
	public synchronized String getString(String path) {
		this.ensureLoaded();
		return this.active.getString(path);
	}

	@Override
	public synchronized String getString(String path, String fallback) {
		this.ensureLoaded();
		return this.active.getString(path, fallback);
	}

	@Override
	public synchronized boolean getBoolean(String path) {
		this.ensureLoaded();
		return this.active.getBoolean(path);
	}

	@Override
	public synchronized boolean getBoolean(String path, boolean fallback) {
		this.ensureLoaded();
		return this.active.getBoolean(path, fallback);
	}

	@Override
	public synchronized int getInt(String path) {
		this.ensureLoaded();
		return this.active.getInt(path);
	}

	@Override
	public synchronized int getInt(String path, int fallback) {
		this.ensureLoaded();
		return this.active.getInt(path, fallback);
	}

	@Override
	public synchronized long getLong(String path) {
		this.ensureLoaded();
		return this.active.getLong(path);
	}

	@Override
	public synchronized long getLong(String path, long fallback) {
		this.ensureLoaded();
		return this.active.getLong(path, fallback);
	}

	@Override
	public synchronized double getDouble(String path) {
		this.ensureLoaded();
		return this.active.getDouble(path);
	}

	@Override
	public synchronized double getDouble(String path, double fallback) {
		this.ensureLoaded();
		return this.active.getDouble(path, fallback);
	}

	public synchronized float getFloat(String path, float fallback) {
		return (float) getDouble(path, fallback);
	}

	@Override
	public synchronized List<String> getStringList(String path) {
		this.ensureLoaded();
		return new ArrayList<>(this.active.getStringList(path));
	}

	@Override
	public synchronized ConfigurationSection getConfigurationSection(String path) {
		this.ensureLoaded();
		return this.active.getConfigurationSection(path);
	}

	@Override
	public synchronized Set<String> getKeys(boolean deep) {
		this.ensureLoaded();
		return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(this.active.getKeys(deep)));
	}

	@Override
	public synchronized Map<String, Object> getValues(boolean deep) {
		this.ensureLoaded();
		return Collections.unmodifiableMap(new LinkedHashMap<>(this.active.getValues(deep)));
	}

	@Override
	public synchronized void set(String path, Object value) {
		this.ensureLoaded();
		this.active.set(path, value);
		this.dirty = true;
	}

	@Override
	public synchronized void remove(String path) {
		this.set(path, null);
	}

	private Candidate readCandidate() throws IOException, InvalidConfigurationException {
		byte[] bundledBytes = this.readBundledDefaults();
		YamlConfiguration defaults = null;
		if (bundledBytes != null) {
			defaults = loadYaml(bundledBytes);
		}

		YamlConfiguration candidate;
		if (Files.isRegularFile(this.file)) {
			candidate = new YamlConfiguration();
			candidate.load(this.file.toFile());
		} else if (bundledBytes != null) {
			candidate = loadYaml(bundledBytes);
		} else {
			candidate = new YamlConfiguration();
			if (this.targetVersion > 0) {
				candidate.set(this.versionPath, this.targetVersion);
			}
		}
		VersionRead version = this.readVersion(candidate);
		if (defaults != null) {
			candidate.setDefaults(defaults);
		}
		return new Candidate(candidate, defaults, bundledBytes, version);
	}

	private byte[] readBundledDefaults() throws IOException {
		if (this.defaultsResource == null) {
			return null;
		}
		try (InputStream stream = this.plugin.getResource(this.defaultsResource)) {
			if (stream == null) {
				return null;
			}
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int read;
			while ((read = stream.read(buffer)) != -1) {
				output.write(buffer, 0, read);
			}
			return output.toByteArray();
		}
	}

	private VersionRead readVersion(YamlConfiguration configuration) {
		Object raw = configuration.get(this.versionPath);
		if (raw == null) {
			return VersionRead.valid(0);
		}
		if (raw instanceof Number) {
			Number number = (Number) raw;
			double value = number.doubleValue();
			int integer = number.intValue();
			if (!Double.isNaN(value) && !Double.isInfinite(value) && value == (double) integer && integer >= 0) {
				return VersionRead.valid(integer);
			}
		} else if (raw instanceof String) {
			try {
				int version = Integer.parseInt(((String) raw).trim());
				if (version >= 0) {
					return VersionRead.valid(version);
				}
			} catch (NumberFormatException ignored) {
			}
		}
		return VersionRead.invalid(new ConfigProblem(this.versionPath,
				"Expected a non-negative integer version"));
	}

	private List<ConfigProblem> validate(YamlConfiguration configuration) {
		ValidationContext context = new ValidationContext();
		ConfigView view = new YamlConfigView(configuration);
		for (ConfigValidator validator : this.validators) {
			try {
				validator.validate(view, context);
			} catch (RuntimeException exception) {
				context.error("", "Validator failed: " + exception.getClass().getSimpleName()
						+ (exception.getMessage() == null ? "" : " - " + exception.getMessage()));
			}
		}
		return context.getProblems();
	}

	private Path createMigrationBackup(int fromVersion, int toVersion) throws IOException {
		Path backup = this.dataDirectory.resolve("backups")
				.resolve(this.relativePath.getParent() == null ? Paths.get("") : this.relativePath.getParent())
				.resolve(this.relativePath.getFileName().toString() + ".v" + fromVersion + "-to-v"
						+ toVersion + "." + System.currentTimeMillis() + ".bak");
		Files.createDirectories(backup.getParent());
		return Files.copy(this.file, backup, StandardCopyOption.COPY_ATTRIBUTES);
	}

	private void atomicWrite(byte[] bytes) throws IOException {
		Path parent = this.file.getParent();
		Files.createDirectories(parent);
		Path temporary = Files.createTempFile(parent, this.file.getFileName().toString() + ".", ".tmp");
		try {
			Files.write(temporary, bytes);
			try {
				Files.move(temporary, this.file, StandardCopyOption.ATOMIC_MOVE,
						StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException ignored) {
				Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private void ensureLoaded() {
		if (!this.loaded) {
			throw new IllegalStateException("Configuration has not been loaded: " + this.relativePath);
		}
	}

	private static YamlConfiguration loadYaml(byte[] bytes) throws InvalidConfigurationException {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.loadFromString(new String(bytes, StandardCharsets.UTF_8));
		return yaml;
	}

	private static YamlConfiguration copyOf(YamlConfiguration source) throws InvalidConfigurationException {
		YamlConfiguration copy = new YamlConfiguration();
		copy.loadFromString(source.saveToString());
		return copy;
	}

	public static final class Builder {

		private final Plugin plugin;
		private final Path dataDirectory;
		private final Path relativePath;
		private final Path file;
		private String defaultsResource;
		private String versionPath = "config-version";
		private int targetVersion;
		private boolean backupMigrations = true;
		private final Map<Integer, MigrationRegistration> migrations = new LinkedHashMap<>();
		private final List<ConfigValidator> validators = new ArrayList<>();

		private Builder(Plugin plugin, String relativePath) {
			this.plugin = Objects.requireNonNull(plugin, "plugin");
			Objects.requireNonNull(relativePath, "relativePath");
			if (relativePath.trim().isEmpty()) {
				throw new IllegalArgumentException("relativePath cannot be blank");
			}

			this.dataDirectory = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
			Path requested = Paths.get(relativePath.replace('/', File.separatorChar)).normalize();
			if (requested.isAbsolute() || requested.startsWith("..") || requested.getFileName() == null) {
				throw new IllegalArgumentException("Configuration must stay inside the plugin data folder");
			}
			Path resolved = this.dataDirectory.resolve(requested).normalize();
			if (!resolved.startsWith(this.dataDirectory)) {
				throw new IllegalArgumentException("Configuration must stay inside the plugin data folder");
			}
			this.relativePath = requested;
			this.file = resolved;
			this.defaultsResource = requested.toString().replace('\\', '/');
		}

		public Builder defaults(String resourcePath) {
			Objects.requireNonNull(resourcePath, "resourcePath");
			String normalized = resourcePath.replace('\\', '/');
			if (normalized.isEmpty() || normalized.startsWith("/") || normalized.contains("../")) {
				throw new IllegalArgumentException("Invalid defaults resource path: " + resourcePath);
			}
			this.defaultsResource = normalized;
			return this;
		}

		public Builder withoutDefaults() {
			this.defaultsResource = null;
			return this;
		}

		public Builder versionPath(String versionPath) {
			if (versionPath == null || versionPath.trim().isEmpty()) {
				throw new IllegalArgumentException("versionPath cannot be blank");
			}
			this.versionPath = versionPath;
			return this;
		}

		public Builder targetVersion(int targetVersion) {
			if (targetVersion < 0) {
				throw new IllegalArgumentException("targetVersion cannot be negative");
			}
			this.targetVersion = targetVersion;
			return this;
		}

		public Builder backupMigrations(boolean backupMigrations) {
			this.backupMigrations = backupMigrations;
			return this;
		}

		public Builder migration(int fromVersion, int toVersion, ConfigMigration migration) {
			if (fromVersion < 0 || toVersion <= fromVersion) {
				throw new IllegalArgumentException("Migration versions must move forward");
			}
			Objects.requireNonNull(migration, "migration");
			MigrationRegistration previous = this.migrations.put(fromVersion,
					new MigrationRegistration(toVersion, migration));
			if (previous != null) {
				this.migrations.put(fromVersion, previous);
				throw new IllegalArgumentException("Duplicate migration from version " + fromVersion);
			}
			return this;
		}

		public Builder validator(ConfigValidator validator) {
			this.validators.add(Objects.requireNonNull(validator, "validator"));
			return this;
		}

		public RosaConfig build() {
			for (Map.Entry<Integer, MigrationRegistration> entry : this.migrations.entrySet()) {
				if (entry.getKey() >= this.targetVersion || entry.getValue().toVersion > this.targetVersion) {
					throw new IllegalStateException("Migration " + entry.getKey() + " -> "
							+ entry.getValue().toVersion + " exceeds target version " + this.targetVersion);
				}
			}
			return new RosaConfig(this);
		}
	}

	private static final class Candidate {

		private final YamlConfiguration configuration;
		private final YamlConfiguration defaults;
		private final byte[] bundledBytes;
		private final VersionRead version;

		private Candidate(YamlConfiguration configuration, YamlConfiguration defaults, byte[] bundledBytes,
						  VersionRead version) {
			this.configuration = configuration;
			this.defaults = defaults;
			this.bundledBytes = bundledBytes;
			this.version = version;
		}
	}

	private static final class MigrationRegistration {

		private final int toVersion;
		private final ConfigMigration action;

		private MigrationRegistration(int toVersion, ConfigMigration action) {
			this.toVersion = toVersion;
			this.action = action;
		}
	}

	private static final class VersionRead {

		private final int version;
		private final ConfigProblem problem;

		private VersionRead(int version, ConfigProblem problem) {
			this.version = version;
			this.problem = problem;
		}

		private static VersionRead valid(int version) {
			return new VersionRead(version, null);
		}

		private static VersionRead invalid(ConfigProblem problem) {
			return new VersionRead(0, problem);
		}

		private boolean isValid() {
			return this.problem == null;
		}
	}
}

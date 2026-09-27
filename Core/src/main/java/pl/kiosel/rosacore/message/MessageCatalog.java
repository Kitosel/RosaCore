package pl.kiosel.rosacore.message;

import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import pl.kiosel.rosacore.config.ConfigLoadResult;
import pl.kiosel.rosacore.config.ConfigView;
import pl.kiosel.rosacore.config.ReloadableConfig;
import pl.kiosel.rosacore.config.RosaConfig;
import pl.kiosel.rosacore.version.MinecraftVersion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class MessageCatalog {

	private final Plugin plugin;
	private final String dataDirectory;
	private final String resourceDirectory;
	private final String extension;
	@Getter
	private final String fallbackLocale;
	private final List<MessageKey> requiredKeys;
	private final String prefixPath;
	private final String prefixSeparator;
	private final MissingMessagePolicy missingMessagePolicy;
	private final UnresolvedPlaceholderPolicy unresolvedPlaceholderPolicy;
	private final LegacyColorizer colorizer;
	private final boolean failOnMissingRequired;

	private volatile CatalogSnapshot active;
	@Getter
	private volatile MessageLoadResult lastLoadResult;
	private String configuredLocale;
	private RosaConfig fallbackConfig;
	private RosaConfig localeConfig;

	private MessageCatalog(Builder builder) {
		this.plugin = builder.plugin;
		this.dataDirectory = builder.dataDirectory;
		this.resourceDirectory = builder.resourceDirectory;
		this.extension = builder.extension;
		this.fallbackLocale = builder.fallbackLocale;
		this.configuredLocale = builder.locale;
		this.requiredKeys = Collections.unmodifiableList(new ArrayList<>(builder.requiredKeys.values()));
		this.prefixPath = builder.prefixPath;
		this.prefixSeparator = builder.prefixSeparator;
		this.missingMessagePolicy = builder.missingMessagePolicy;
		this.unresolvedPlaceholderPolicy = builder.unresolvedPlaceholderPolicy;
		this.colorizer = builder.colorizer;
		this.failOnMissingRequired = builder.failOnMissingRequired;
	}

	public static Builder builder(Plugin plugin) {
		return new Builder(plugin);
	}

	public synchronized MessageLoadResult load() {
		return this.switchLocale(this.configuredLocale);
	}

	public synchronized MessageLoadResult reload() {
		return this.switchLocale(this.configuredLocale);
	}

	public synchronized MessageLoadResult switchLocale(String locale) {
		String previousLocale = this.active == null ? null : this.active.locale;
		if (!isValidLocale(locale)) {
			MessageLoadResult result = MessageLoadResult.failure(locale, previousLocale,
					"Invalid locale name", null, null, null);
			this.lastLoadResult = result;
			return result;
		}

		RosaConfig candidateFallback = this.createConfig(this.fallbackLocale);
		if (!this.sourceExists(candidateFallback, this.resourcePath(this.fallbackLocale))) {
			MessageLoadResult result = MessageLoadResult.failure(locale, previousLocale,
					"Fallback locale source does not exist: " + this.fallbackLocale,
					null, null, null);
			this.lastLoadResult = result;
			return result;
		}

		ConfigLoadResult fallbackResult = candidateFallback.load();
		if (!fallbackResult.isSuccess()) {
			MessageLoadResult result = MessageLoadResult.failure(locale, previousLocale,
					"Could not load fallback locale: " + this.fallbackLocale,
					fallbackResult.getCause(), fallbackResult, null);
			this.lastLoadResult = result;
			return result;
		}

		RosaConfig candidateLocale;
		ConfigLoadResult localeResult;
		if (locale.equals(this.fallbackLocale)) {
			candidateLocale = candidateFallback;
			localeResult = fallbackResult;
		} else {
			candidateLocale = this.createConfig(locale);
			if (!this.sourceExists(candidateLocale, this.resourcePath(locale))) {
				MessageLoadResult result = MessageLoadResult.failure(locale, previousLocale,
						"Locale source does not exist: " + locale,
						null, fallbackResult, null);
				this.lastLoadResult = result;
				return result;
			}
			localeResult = candidateLocale.load();
			if (!localeResult.isSuccess()) {
				MessageLoadResult result = MessageLoadResult.failure(locale, previousLocale,
						"Could not load locale: " + locale,
						localeResult.getCause(), fallbackResult, localeResult);
				this.lastLoadResult = result;
				return result;
			}
		}

		ConfigView localeView = candidateLocale.snapshot();
		ConfigView fallbackView = candidateFallback.snapshot();
		List<String> missing = this.findMissing(localeView, fallbackView);
		if (this.failOnMissingRequired && !missing.isEmpty()) {
			MessageLoadResult result = MessageLoadResult.failure(locale, previousLocale,
					"Required message keys are missing: " + missing,
					null, fallbackResult, localeResult);
			this.lastLoadResult = result;
			return result;
		}

		this.active = new CatalogSnapshot(locale, localeView, fallbackView, missing);
		this.fallbackConfig = candidateFallback;
		this.localeConfig = candidateLocale;
		this.configuredLocale = locale;
		MessageLoadResult result = MessageLoadResult.success(locale,
				fallbackResult, localeResult, missing);
		this.lastLoadResult = result;
		return result;
	}

	public RosaMessage message(MessageKey key) {
		Objects.requireNonNull(key, "key");
		return this.message(key.getPath(), key.getDefaultMessage());
	}

	public RosaMessage message(String path) {
		return this.message(path, null);
	}

	public RosaMessage message(String path, String defaultMessage) {
		MessageKey.requirePath(path);
		CatalogSnapshot snapshot = this.requireSnapshot();
		ResolvedMessage resolved = this.resolve(snapshot, path, defaultMessage, this.missingMessagePolicy);
		String prefix = "";
		if (this.prefixPath != null && !this.prefixPath.equals(path)) {
			ResolvedMessage resolvedPrefix = this.resolve(snapshot, this.prefixPath, "",
					MissingMessagePolicy.EMPTY);
			prefix = String.join("\n", resolvedPrefix.templates);
		}
		return new RosaMessage(path, resolved.templates, prefix, this.prefixSeparator,
				resolved.locale, resolved.fallback, this.unresolvedPlaceholderPolicy,
				this.colorizer, MessagePlaceholders.empty());
	}

	public RosaMessage literal(String template) {
		return this.literal(Collections.singletonList(Objects.requireNonNull(template, "template")));
	}

	public RosaMessage literal(List<String> templates) {
		Objects.requireNonNull(templates, "templates");
		List<String> copiedTemplates = new ArrayList<>(templates.size());
		for (String template : templates) {
			copiedTemplates.add(Objects.requireNonNull(template, "template"));
		}

		CatalogSnapshot snapshot = this.requireSnapshot();
		String prefix = "";
		if (this.prefixPath != null) {
			ResolvedMessage resolvedPrefix = this.resolve(snapshot, this.prefixPath, "",
					MissingMessagePolicy.EMPTY);
			prefix = String.join("\n", resolvedPrefix.templates);
		}
		return new RosaMessage("literal", copiedTemplates, prefix, this.prefixSeparator,
				snapshot.locale, false, this.unresolvedPlaceholderPolicy,
				this.colorizer, MessagePlaceholders.empty());
	}

	public RosaMessage format(MessageKey key, Object... nameValuePairs) {
		return this.message(key).withPairs(nameValuePairs);
	}

	public RosaMessage format(String path, Object... nameValuePairs) {
		return this.message(path).withPairs(nameValuePairs);
	}

	public String text(MessageKey key, MessagePlaceholders placeholders) {
		return this.message(key).with(placeholders).legacy();
	}

	public String text(String path, MessagePlaceholders placeholders) {
		return this.message(path).with(placeholders).legacy();
	}

	public String text(MessageKey key, Object... nameValuePairs) {
		return this.format(key, nameValuePairs).legacy();
	}

	public String text(String path, Object... nameValuePairs) {
		return this.format(path, nameValuePairs).legacy();
	}

	public void send(CommandSender sender, MessageKey key, Object... nameValuePairs) {
		this.format(key, nameValuePairs).send(sender);
	}

	public void send(CommandSender sender, String path, Object... nameValuePairs) {
		this.format(path, nameValuePairs).send(sender);
	}

	public void sendPrefixed(CommandSender sender, MessageKey key,
							 Object... nameValuePairs) {
		this.format(key, nameValuePairs).sendPrefixed(sender);
	}

	public void sendPrefixed(CommandSender sender, String path,
							 Object... nameValuePairs) {
		this.format(path, nameValuePairs).sendPrefixed(sender);
	}

	public boolean isLoaded() {
		return this.active != null;
	}

	public String getLocale() {
		return this.requireSnapshot().locale;
	}

	public List<String> getMissingRequiredKeys() {
		return this.requireSnapshot().missingRequiredKeys;
	}

	public Path getLocalePath() {
		RosaConfig current = this.localeConfig;
		if (current != null) {
			return current.getPath();
		}
		return this.plugin.getDataFolder().toPath().resolve(this.dataPath(this.configuredLocale))
				.toAbsolutePath().normalize();
	}

	public ReloadableConfig asReloadable() {
		return new ReloadableConfig() {
			@Override
			public ConfigLoadResult reload() {
				MessageLoadResult result = MessageCatalog.this.reload();
				if (result.isSuccess()) {
					return ConfigLoadResult.loaded();
				}
				return ConfigLoadResult.failed("messages", result.getProblem(), result.getCause());
			}

			@Override
			public boolean isLoaded() {
				return MessageCatalog.this.isLoaded();
			}

			@Override
			public Path getPath() {
				return MessageCatalog.this.getLocalePath();
			}
		};
	}

	private RosaConfig createConfig(String locale) {
		return RosaConfig.builder(this.plugin, this.dataPath(locale))
				.defaults(this.resourcePath(locale))
				.validator((config, context) -> {
					for (Map.Entry<String, Object> entry : config.getValues(true).entrySet()) {
						Object value = entry.getValue();
						if (value instanceof ConfigurationSection || value instanceof String) {
							continue;
						}
						if (value instanceof List<?> && containsOnlyStrings((List<?>) value)) {
							continue;
						}
						context.error(entry.getKey(), "Message must be a string or a list of strings");
					}
				})
				.build();
	}

	private boolean sourceExists(RosaConfig config, String resourcePath) {
		if (Files.isRegularFile(config.getPath())) {
			return true;
		}
		try (InputStream stream = this.plugin.getResource(resourcePath)) {
			return stream != null;
		} catch (IOException exception) {
			return false;
		}
	}

	private List<String> findMissing(ConfigView locale, ConfigView fallback) {
		List<String> missing = new ArrayList<>();
		for (MessageKey key : this.requiredKeys) {
			if (readTemplates(locale, key.getPath()) == null
					&& readTemplates(fallback, key.getPath()) == null) {
				missing.add(key.getPath());
			}
		}
		return Collections.unmodifiableList(missing);
	}

	private ResolvedMessage resolve(CatalogSnapshot snapshot, String path, String codeDefault,
									MissingMessagePolicy policy) {
		List<String> templates = readTemplates(snapshot.localeView, path);
		if (templates != null) {
			return new ResolvedMessage(templates, snapshot.locale, false);
		}
		templates = readTemplates(snapshot.fallbackView, path);
		if (templates != null) {
			return new ResolvedMessage(templates, this.fallbackLocale, true);
		}
		if (codeDefault != null) {
			return new ResolvedMessage(Collections.singletonList(codeDefault), "code-default", true);
		}
		if (policy == MissingMessagePolicy.EMPTY) {
			return new ResolvedMessage(Collections.singletonList(""), "missing", true);
		}
		if (policy == MissingMessagePolicy.THROW) {
			throw new MissingMessageException(path);
		}
		return new ResolvedMessage(Collections.singletonList(path), "missing", true);
	}

	private CatalogSnapshot requireSnapshot() {
		CatalogSnapshot snapshot = this.active;
		if (snapshot == null) {
			throw new IllegalStateException("Message catalog has not been loaded");
		}
		return snapshot;
	}

	private String dataPath(String locale) {
		return join(this.dataDirectory, locale + this.extension);
	}

	private String resourcePath(String locale) {
		return join(this.resourceDirectory, locale + this.extension);
	}

	private static String join(String directory, String file) {
		return directory.isEmpty() ? file : directory + "/" + file;
	}

	private static List<String> readTemplates(ConfigView config, String path) {
		Object value = config.get(path);
		if (value instanceof String) {
			return Collections.singletonList((String) value);
		}
		if (value instanceof List<?> && containsOnlyStrings((List<?>) value)) {
			List<String> strings = new ArrayList<>();
			for (Object element : (List<?>) value) {
				strings.add((String) element);
			}
			return Collections.unmodifiableList(strings);
		}
		return null;
	}

	private static boolean containsOnlyStrings(List<?> values) {
		for (Object value : values) {
			if (!(value instanceof String)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isValidLocale(String locale) {
		return locale != null && locale.matches("[A-Za-z0-9_-]+");
	}

	private static String normalizeDirectory(String directory, String field) {
		Objects.requireNonNull(directory, field);
		String normalized = directory.replace('\\', '/');
		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		if (normalized.startsWith("/") || normalized.equals("..")
				|| normalized.startsWith("../") || normalized.contains("/../")) {
			throw new IllegalArgumentException(field + " must be a relative directory");
		}
		return normalized;
	}

	public static final class Builder {

		private final Plugin plugin;
		private String dataDirectory = "locales";
		private String resourceDirectory = "locales";
		private String extension = ".yml";
		private String fallbackLocale = "en_US";
		private String locale = "en_US";
		private final Map<String, MessageKey> requiredKeys = new LinkedHashMap<>();
		private String prefixPath;
		private String prefixSeparator = " ";
		private MissingMessagePolicy missingMessagePolicy = MissingMessagePolicy.RETURN_KEY;
		private UnresolvedPlaceholderPolicy unresolvedPlaceholderPolicy = UnresolvedPlaceholderPolicy.KEEP;
		private LegacyColorizer colorizer = LegacyColorizer.legacyOnly();
		private boolean failOnMissingRequired;

		private Builder(Plugin plugin) {
			this.plugin = Objects.requireNonNull(plugin, "plugin");
		}

		public Builder dataDirectory(String directory) {
			this.dataDirectory = normalizeDirectory(directory, "dataDirectory");
			return this;
		}

		public Builder resourceDirectory(String directory) {
			this.resourceDirectory = normalizeDirectory(directory, "resourceDirectory");
			return this;
		}

		public Builder extension(String extension) {
			if (extension == null || !extension.matches("\\.[A-Za-z0-9]+")) {
				throw new IllegalArgumentException("Invalid locale file extension: " + extension);
			}
			this.extension = extension;
			return this;
		}

		public Builder fallbackLocale(String locale) {
			if (!isValidLocale(locale)) {
				throw new IllegalArgumentException("Invalid fallback locale: " + locale);
			}
			this.fallbackLocale = locale;
			return this;
		}

		public Builder locale(String locale) {
			if (!isValidLocale(locale)) {
				throw new IllegalArgumentException("Invalid locale: " + locale);
			}
			this.locale = locale;
			return this;
		}

		public Builder required(MessageKey... keys) {
			Objects.requireNonNull(keys, "keys");
			for (MessageKey key : keys) {
				Objects.requireNonNull(key, "key");
				this.requiredKeys.put(key.getPath(), key);
			}
			return this;
		}

		public Builder prefix(MessageKey key) {
			this.prefixPath = Objects.requireNonNull(key, "key").getPath();
			return this;
		}

		public Builder prefixSeparator(String separator) {
			this.prefixSeparator = Objects.requireNonNull(separator, "separator");
			return this;
		}

		public Builder missingMessagePolicy(MissingMessagePolicy policy) {
			this.missingMessagePolicy = Objects.requireNonNull(policy, "policy");
			return this;
		}

		public Builder unresolvedPlaceholderPolicy(UnresolvedPlaceholderPolicy policy) {
			this.unresolvedPlaceholderPolicy = Objects.requireNonNull(policy, "policy");
			return this;
		}

		public Builder minecraftVersion(MinecraftVersion version) {
			this.colorizer = LegacyColorizer.forVersion(version);
			return this;
		}

		public Builder colorizer(LegacyColorizer colorizer) {
			this.colorizer = Objects.requireNonNull(colorizer, "colorizer");
			return this;
		}

		public Builder failOnMissingRequired(boolean fail) {
			this.failOnMissingRequired = fail;
			return this;
		}

		public MessageCatalog build() {
			this.dataDirectory = normalizeDirectory(this.dataDirectory, "dataDirectory");
			this.resourceDirectory = normalizeDirectory(this.resourceDirectory, "resourceDirectory");
			return new MessageCatalog(this);
		}
	}

	private static final class CatalogSnapshot {

		private final String locale;
		private final ConfigView localeView;
		private final ConfigView fallbackView;
		private final List<String> missingRequiredKeys;

		private CatalogSnapshot(String locale, ConfigView localeView, ConfigView fallbackView,
								List<String> missingRequiredKeys) {
			this.locale = locale;
			this.localeView = localeView;
			this.fallbackView = fallbackView;
			this.missingRequiredKeys = Collections.unmodifiableList(new ArrayList<>(missingRequiredKeys));
		}
	}

	private static final class ResolvedMessage {

		private final List<String> templates;
		private final String locale;
		private final boolean fallback;

		private ResolvedMessage(List<String> templates, String locale, boolean fallback) {
			this.templates = templates;
			this.locale = locale;
			this.fallback = fallback;
		}
	}
}

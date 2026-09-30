package pl.kiosel.rosacore;

import de.clickism.modrinthupdatechecker.ModrinthUpdateChecker;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import pl.kiosel.rosacore.api.RosaApi;
import pl.kiosel.rosacore.command.RosaCommand;
import pl.kiosel.rosacore.compat.RuntimeClassLookup;
import pl.kiosel.rosacore.compat.ServerEnvironmentDetector;
import pl.kiosel.rosacore.compatibility.ZColor;
import pl.kiosel.rosacore.config.ConfigLoadResult;
import pl.kiosel.rosacore.config.ReloadableConfig;
import pl.kiosel.rosacore.config.RosaConfig;
import pl.kiosel.rosacore.config.setting.RosaSettings;
import pl.kiosel.rosacore.config.setting.SettingsSchema;
import pl.kiosel.rosacore.cooldown.RosaCooldowns;
import pl.kiosel.rosacore.database.DatabaseManager;
import pl.kiosel.rosacore.database.DatabaseMigration;
import pl.kiosel.rosacore.database.DatabaseSettings;
import pl.kiosel.rosacore.hologram.RosaHologramManager;
import pl.kiosel.rosacore.hook.HookManager;
import pl.kiosel.rosacore.input.Debug;
import pl.kiosel.rosacore.listener.RosaListener;
import pl.kiosel.rosacore.message.*;
import pl.kiosel.rosacore.nms.CustomAnvilManager;
import pl.kiosel.rosacore.nms.NmsResolver;
import pl.kiosel.rosacore.nms.TabListManager;
import pl.kiosel.rosacore.nms.api.NMS;
import pl.kiosel.rosacore.scheduler.RosaScheduler;
import pl.kiosel.rosacore.scheduler.RosaSchedulers;
import pl.kiosel.rosacore.scoreboard.RosaScoreboardManager;
import pl.kiosel.rosacore.utils.ColorUtils;
import pl.kiosel.rosacore.utils.ReflectionUtils;
import pl.kiosel.rosacore.version.MinecraftVersion;
import pl.kiosel.rosacore.version.ServerEnvironment;
import pl.kiosel.rosacore.version.Version;

import java.util.*;
import java.util.logging.Level;

public abstract class RosaPlugin extends JavaPlugin implements RosaApi, Listener {

	private ServerEnvironment serverEnvironment;
	private RosaScheduler rosaScheduler;

	private final List<DatabaseManager> databaseManagers = new ArrayList<>();
	private final List<ReloadableConfig> managedConfigs = new ArrayList<>();
	private final List<Command> registeredCommands = new ArrayList<>();

	private HookManager hookManager;
	private MessageCatalog locale;
	private RosaMessenger messenger;
	private RosaBossBarManager bossBars;
	private RosaScoreboardManager scoreboards;
	private RosaHologramManager holograms;
	private RosaCooldowns cooldowns;

	@Getter private String newPluginVersion;

	//NMS RELATED
	private CustomAnvilManager customAnvils;
	private TabListManager tabListManager;
	private NMS nms;

	private CommandSender console;

	private boolean emergencyStop = false;
	private boolean pluginEnableHookStarted;
	private boolean pluginDisableHookCompleted;
	protected long dataLoadDelay = 20L;

	private final String separator = "&8---------------------------------";
	private final String errorSeparator = "&4!===============================!";
	@Setter
	@Getter
	private ZColor asciColor = ZColor.DARK_PURPLE;

	@Setter
	@Getter
	private boolean dev;
	@Getter
	private Debug debug;
	private boolean usingNMS;

	@Override
	public final void onLoad() {
		console = Bukkit.getConsoleSender();
		RosaLogger.getInstance().setPlugin(this);
		try {
			this.serverEnvironment = ServerEnvironmentDetector.detect(
					getServer().getBukkitVersion(),
					getServer().getName(),
					new RuntimeClassLookup(RosaPlugin.class.getClassLoader())
			);
			this.debug = new Debug(this);

			onPluginLoad();
			this.debug.setup();

			hookManager = new HookManager(this);

			if (usingNMS) {
				this.debug.debug("Server is using NMS");
				this.nms = NmsResolver.resolve(this,
						Version.getServerRevisionVersion(),
						getClass().getClassLoader()
				);
			}
		} catch (Throwable th) {
			closeDatabases();
			this.managedConfigs.clear();
			this.locale = null;
			this.nms = null;
			criticalErrorOnPluginStartup(th);
		}
	}

	@Override
	public final void onEnable() {
		this.debug.debug("Enabling plugin");
		if (this.emergencyStop) {
			setEnabled(false);
			return;
		}
		if (this.serverEnvironment == null)
			throw new IllegalStateException("RosaCore environment was not initialized during onLoad");

		this.pluginEnableHookStarted = false;
		this.pluginDisableHookCompleted = false;
		this.rosaScheduler = RosaSchedulers.create(this, this.serverEnvironment);

		log(" ");
		log(separator);
		if (Version.isServerVersionBelow(Version.V1_12_2)) {
			log(String.format("&7%s %s", getDescription().getName(), getDescription().getVersion()));
		} else {
			log(AsciiArtCreator.create().ascii(getDescription().getName()).buildLines(), this.asciColor);
		}
		log("&7Version &6" + getDescription().getVersion());
		log("&7By &d" + getDescription().getAuthors());
		log("&7Status: &aEnabling");
		try {
			if (usingNMS && nms != null) {
				this.tabListManager = new TabListManager(this,
						nms.getTabListService(),
						nms.getTabPacketInterceptor(),
						nms.getServerStatusInterceptor());
				this.customAnvils = new CustomAnvilManager(this);
			}

			this.cooldowns = new RosaCooldowns(this.rosaScheduler);
			this.messenger = new RosaMessenger(this, getMinecraftVersion());
			this.bossBars = new RosaBossBarManager(this.messenger);
			this.scoreboards = new RosaScoreboardManager(this);
			this.holograms = new RosaHologramManager(this);


			this.pluginEnableHookStarted = true;
			onPluginEnable();

			if (this.emergencyStop) {
				log(errorSeparator);
				log(" ");
				return;
			}
			this.rosaScheduler.runGlobalLater(this::runDataLoadSafely, this.dataLoadDelay);
		} catch (RuntimeException | Error throwable) {
			criticalErrorOnPluginStartup(throwable);
			closeAll();

			log(errorSeparator);
			log(" ");
			throw throwable;
		}

		log(separator);
		log(" ");
	}

	@Override
	public final void onDisable() {
		this.debug.debug("Disabling plugin");
		log(" ");
		log(separator);
		log(String.format("&7%s %s by &dKio", getDescription().getName(), getDescription().getVersion()));
		log("&7Plugin: &cDisabling");

		try {
			runPluginDisableHook();
		} finally {
			unregisterCommands();
			closeAll();

			this.pluginEnableHookStarted = false;
			this.pluginDisableHookCompleted = false;
			if (this.debug != null) this.debug.close();
			this.serverEnvironment = null;
			this.nms = null;
		}
		log(separator);
		log(" ");
	}

	public abstract void onPluginLoad();

	public abstract void onPluginEnable();

	public abstract void onPluginDisable();

	public abstract void onDataLoad();

	public abstract void onConfigReload();

	private void closeAll() {
		this.hookManager.close();

		closeHolograms();
		closeScoreboards();
		closeBossBars();
		closeMessenger();
		closeCooldowns();
		closeCustomAnvils();
		closeTabLists();

		if (this.rosaScheduler != null)
			this.rosaScheduler.cancelAll();
		this.rosaScheduler = null;

		closeDatabases();
		this.managedConfigs.clear();
		this.locale = null;
	}

	private void runDataLoadSafely() {
		if (this.emergencyStop || !isEnabled()) return;
		try {
			onDataLoad();
		} catch (RuntimeException | Error throwable) {
			criticalErrorOnPluginStartup(throwable);
		}
	}

	public final void reloadRosaConfig() {
		for (ReloadableConfig config : this.managedConfigs) {
			ConfigLoadResult result = config.reload();
			if (!result.isSuccess())
				throw configFailure(config, result);
		}
		if (this.locale != null) {
			MessageLoadResult result = this.locale.reload();
			if (!result.isSuccess())
				throw localeFailure(result);
		}
		onConfigReload();
	}

	protected void registerCommands(String prefix, RosaCommand... commands) {
		getDebug().debug("Registering commands...");
		for (RosaCommand cmd : commands)
			if (cmd != null)
				registerCommand(prefix, cmd);
	}

	protected void registerCommands(RosaCommand... commands) {
		getDebug().debug("Registering commands...");
		for (RosaCommand cmd : commands)
			if (cmd != null)
				registerCommand(cmd);
	}

	public final void registerCommand(RosaCommand command) {
		this.registerCommand(getName(), command);
	}

	public final void registerCommand(String prefix, RosaCommand command) {
		if (command == null) return;
		if (this.registeredCommands.contains(command)) {
			throw new IllegalStateException("Command is already registered: /" + command.getName());
		}
		getDebug().debug("Registering command: /" + command.getName());
		if (!ReflectionUtils.registerCommand(prefix, command)) {
			throw new IllegalStateException("Could not register command /" + command.getName());
		}
		this.registeredCommands.add(command);
	}

	protected void registerListeners(Listener... listeners) {
		getDebug().debug("Registering listeners...");
		for (Listener listener : listeners) {
			if (listener == null) continue;
			if (listener instanceof RosaListener && !((RosaListener) listener).isAvailable()) continue;
			getDebug().debug("Registering listener: " + listener.getClass().getName());
			getServer().getPluginManager().registerEvents(listener, this);
		}
	}

	protected void emergencyStop(String... reason) {
		this.emergencyStop = true;
		debug.debug("Emergency stopped");
		if (reason != null && reason.length > 0) {
			debug.debug("Emergency stopped reason: " + reason[0]);
		}

		Bukkit.getPluginManager().disablePlugin(this);
	}

	@Override
	public final String getRosaCoreVersion() {
		return getCoreVersion();
	}

	@Override
	public final MinecraftVersion getMinecraftVersion() {
		return getServerEnvironment().getMinecraftVersion();
	}

	@Override
	public final ServerEnvironment getServerEnvironment() {
		if (this.serverEnvironment == null)
			throw new IllegalStateException("Server environment is unavailable before onLoad or after onDisable");

		return this.serverEnvironment;
	}

	protected void criticalErrorOnPluginStartup(Throwable throwable) {
		Bukkit.getLogger().log(Level.SEVERE,
				String.format(
						"Unexpected error while loading %s v%s (core v%s): Disabling plugin!",
						getDescription().getName(),
						getDescription().getVersion(),
						RosaPlugin.getCoreVersion()), throwable);

		emergencyStop(throwable.getMessage());
	}

	public void runShutdownStep(String description, Runnable action) {
		try {
			action.run();
		} catch (Exception exception) {
			getLogger().log(Level.SEVERE, "Error while " + description, exception);
		}
	}

	private void runPluginDisableHook() {
		if (!this.pluginEnableHookStarted || this.pluginDisableHookCompleted) return;
		this.pluginDisableHookCompleted = true;
		runShutdownStep("running plugin disable hook", this::onPluginDisable);
	}

	private void unregisterCommands() {
		if (this.registeredCommands.isEmpty()) return;
		try {
			Map<String, Command> knownCommands = ReflectionUtils.getKnownCommands(ReflectionUtils.getCommandMap());
			for (int index = this.registeredCommands.size() - 1; index >= 0; index--) {
				Command command = this.registeredCommands.get(index);
				try {
					ReflectionUtils.unregisterCommand(knownCommands, command);
				} catch (ReflectiveOperationException exception) {
					getLogger().log(Level.SEVERE, "Could not unregister command /" + command.getName(), exception);
				}
			}
		} catch (ReflectiveOperationException exception) {
			getLogger().log(Level.SEVERE, "Could not access Bukkit command map while disabling plugin", exception);
		} finally {
			this.registeredCommands.clear();
		}
	}

	public final RosaLogger getRosaLogger() {
		return RosaLogger.getInstance();
	}

	public final RosaScheduler getRosaScheduler() {
		if (this.rosaScheduler == null)
			throw new IllegalStateException("Scheduler is unavailable before onEnable or after onDisable");

		return this.rosaScheduler;
	}

	public final HookManager getHookManager() {
		return this.hookManager;
	}

	public final RosaMessenger getMessenger() {
		if (this.messenger == null) {
			throw new IllegalStateException("Messenger is unavailable before onEnable or after onDisable");
		}
		return this.messenger;
	}

	public final RosaBossBarManager getBossBars() {
		if (this.bossBars == null) {
			throw new IllegalStateException("Boss bars are unavailable before onEnable or after onDisable");
		}
		return this.bossBars;
	}

	public final RosaScoreboardManager getScoreboards() {
		if (this.scoreboards == null) {
			throw new IllegalStateException("Scoreboards are unavailable before onEnable or after onDisable");
		}
		return this.scoreboards;
	}

	public final RosaHologramManager getHolograms() {
		if (this.holograms == null) {
			throw new IllegalStateException("Holograms are unavailable before onEnable or after onDisable");
		}
		return this.holograms;
	}

	public final RosaCooldowns getCooldowns() {
		if (this.cooldowns == null) {
			throw new IllegalStateException("Cooldowns are unavailable before onEnable or after onDisable");
		}
		return this.cooldowns;
	}

	public final CustomAnvilManager getCustomAnvils() {
		if (this.customAnvils == null) {
			throw new IllegalStateException("Custom anvils are unavailable before onEnable or after onDisable");
		}
		return this.customAnvils;
	}

	public final NMS getNMS() {
		if (this.nms == null) {
			throw new IllegalStateException("NMS is unavailable before onLoad or after onDisable");
		}
		return this.nms;
	}

	public final void useNMS(boolean nms) {
		this.usingNMS = nms;
	}

	protected final RosaConfig loadConfig(String relativePath) {
		RosaConfig config = new RosaConfig(this, relativePath);
		ConfigLoadResult result = config.load();
		if (!result.isSuccess()) {
			throw configFailure(config, result);
		}
		this.managedConfigs.add(config);
		debug.debug("Loaded config: " + relativePath);
		return config;
	}

	protected final RosaSettings loadSettings(String relativePath, SettingsSchema schema) {
		Objects.requireNonNull(relativePath, "file");
		Objects.requireNonNull(schema, "settings schema");
		RosaSettings settings = RosaSettings.create(
				RosaConfig.builder(this, relativePath),
				schema
		);
		ConfigLoadResult result = settings.load();

		if (!result.isSuccess()) {
			throw configFailure(settings, result);
		}
		this.managedConfigs.add(settings);
		debug.debug("Loaded settings: " + relativePath);
		return settings;
	}

	protected final void reloadSettings(RosaSettings settings) {
		Objects.requireNonNull(settings, "settings");
		ConfigLoadResult result = settings.reload();

		if (!result.isSuccess()) {
			throw new IllegalStateException("Error while reloading config: " + result.getProblems());
		}
		debug.debug("Reloaded settings: " + settings.getConfig().getFile().getName());
	}

	public final void setLocale(String locale) {
		setLocale(locale, locale);
	}

	public final void setLocale(String locale, String fallbackLocale) {
		MessageCatalog candidate = MessageCatalog.builder(this)
				.dataDirectory("locales")
				.resourceDirectory("")
				.extension(".lang")
				.locale(locale)
				.fallbackLocale(fallbackLocale)
				.prefix(MessageKey.of("prefix"))
				.minecraftVersion(getMinecraftVersion())
				.build();
		MessageLoadResult result = candidate.load();
		if (!result.isSuccess()) {
			throw localeFailure(result);
		}
		this.locale = candidate;
		debug.debug("The locale has been set to: " + candidate.getLocale());
	}

	public final MessageCatalog getLocale() {
		if (this.locale == null) {
			throw new IllegalStateException("Locale has not been set. Call setLocale(...) first");
		}
		return this.locale;
	}

	public final MessageCatalog getMessages() {
		return getLocale();
	}

	public final List<RosaConfig> getRosaConfigs() {
		List<RosaConfig> configs = new ArrayList<>();
		for (ReloadableConfig managedConfig : this.managedConfigs) {
			if (managedConfig instanceof RosaConfig) {
				configs.add((RosaConfig) managedConfig);
			} else if (managedConfig instanceof RosaSettings) {
				configs.add(((RosaSettings) managedConfig).getConfig());
			}
		}
		return Collections.unmodifiableList(configs);
	}

	protected final DatabaseManager createDatabase(DatabaseSettings settings, DatabaseMigration... migrations) {
		return createDatabase(settings, "", migrations);
	}

	protected final DatabaseManager createDatabase(DatabaseSettings settings, String tablePrefix,
												   DatabaseMigration... migrations) {
		DatabaseManager database = DatabaseManager.open(this, settings, tablePrefix);
		try {
			database.migrate(migrations);
			this.databaseManagers.add(database);
			return database;
		} catch (RuntimeException | Error exception) {
			try {
				database.close();
			} catch (RuntimeException closeFailure) {
				exception.addSuppressed(closeFailure);
			}
			throw exception;
		}
	}

	public final List<DatabaseManager> getDatabaseManagers() {
		return Collections.unmodifiableList(new ArrayList<>(this.databaseManagers));
	}

	private void closeDatabases() {
		for (int index = this.databaseManagers.size() - 1; index >= 0; index--) {
			DatabaseManager database = this.databaseManagers.get(index);
			runShutdownStep("closing " + database.getType() + " database", database::close);
		}
		this.databaseManagers.clear();
	}

	private void closeMessenger() {
		if (this.messenger == null) return;
		runShutdownStep("closing Adventure audiences", this.messenger::close);
		this.messenger = null;
	}

	private void closeBossBars() {
		if (this.bossBars == null) return;
		runShutdownStep("closing boss bars", this.bossBars::close);
		this.bossBars = null;
	}

	private void closeScoreboards() {
		if (this.scoreboards == null) return;
		runShutdownStep("closing scoreboards", this.scoreboards::close);
		this.scoreboards = null;
	}

	private void closeHolograms() {
		if (this.holograms == null) return;
		runShutdownStep("closing holograms", this.holograms::close);
		this.holograms = null;
	}

	private void closeCooldowns() {
		if (this.cooldowns == null) return;
		runShutdownStep("closing cooldown manager", this.cooldowns::close);
		this.cooldowns = null;
	}

	private void closeCustomAnvils() {
		if (this.customAnvils != null) {
			this.customAnvils.close();
			this.customAnvils = null;
		}
	}

	private void closeTabLists() {
		if (this.tabListManager == null) return;
		runShutdownStep("closing tab lists", this.tabListManager::close);
		this.tabListManager = null;
	}

	private static IllegalStateException configFailure(ReloadableConfig config, ConfigLoadResult result) {
		String problem = result.getProblems().isEmpty()
				? "unknown problem"
				: result.getProblems().get(0).toString();
		return new IllegalStateException("Could not load config " + config.getPath() + ": " + problem,
				result.getCause());
	}

	private static IllegalStateException localeFailure(MessageLoadResult result) {
		return new IllegalStateException("Could not load locale " + result.getRequestedLocale()
				+ ": " + result.getProblem(), result.getCause());
	}

	public void log(String message) {
		this.console.sendMessage(ColorUtils.color(message));
	}

	public void log(String... message) {
		log(Arrays.asList(message));
	}

	public void log(List<String> message) {
		log(message, null);
	}

	public void log(List<String> message, ZColor color) {
		for (String mess : message) {
			if (color == null) {
				log(mess);
			} else {
				log(color.getChatColor() + mess);
			}
		}
	}

	public void checkUpdates(String projectId) {
		getRosaLogger().info("Checking for updates...");

		String mcVersion = Bukkit.getBukkitVersion().split("-")[0];
		String platform = this.getServerEnvironment().getPlatform().name().toLowerCase(Locale.ROOT);

		new ModrinthUpdateChecker(projectId, platform, mcVersion).checkVersion(version -> {
			this.newPluginVersion = version;
			String currentVersion = getDescription().getVersion();

			if (currentVersion.equals(version)) {
				getDebug().debug("No new version found!");
			} else {
				getRosaLogger().info("New plugin version available: " + version);
				log(" ");
				log("&7New " + getDescription().getName() + " version available &e" + version + " &7(Actual: &e" + currentVersion + "&e)");
				log(" ");
			}
		});
	}

	public static String getCoreVersion() {
		return RosaCoreBuildInfo.getVersion();
	}

	public static String getCoreName() {
		return "RosaCore";
	}

}

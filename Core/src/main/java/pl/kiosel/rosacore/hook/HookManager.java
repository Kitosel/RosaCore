package pl.kiosel.rosacore.hook;

import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import pl.kiosel.rosacore.RosaPlugin;
import pl.kiosel.rosacore.hook.economy.*;
import pl.kiosel.rosacore.hook.worldedit.WorldEditHook;
import pl.kiosel.rosacore.hook.worldedit.WorldGuardHook;

import java.util.*;
import java.util.logging.Level;

public final class HookManager implements Listener, AutoCloseable {

	public static final int PRIORITY_HIGH = 1;
	public static final int PRIORITY_NORMAL = 0;
	public static final int PRIORITY_LOW = -1;

	private final RosaPlugin plugin;
	private final Map<String, RosaHook> hooks = new LinkedHashMap<>();

	@Getter
	private final EconomyManager economy = new EconomyManager();
	@Getter
	private final WorldEditHook worldEdit = new WorldEditHook();
	@Getter
	private final WorldGuardHook worldGuard = new WorldGuardHook();

	private boolean started;

	public HookManager(RosaPlugin plugin) {
		this.plugin = plugin;

		register(new VaultEconomyHook(), PRIORITY_HIGH);
		register(new EssentialsXEconomyHook(), PRIORITY_NORMAL);
		register(new PlayerPointsEconomyHook(), PRIORITY_LOW);
		register(this.worldEdit);
		register(this.worldGuard);
	}

	public void start() {
		if (this.started) {
			refresh();
			return;
		}
		plugin.getDebug().debug("Starting hook manager");
		this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
		this.started = true;
		refresh();
	}

	public void register(RosaHook hook) {
		register(hook, PRIORITY_NORMAL);
	}

	public void register(RosaHook hook, int priority) {
		Objects.requireNonNull(hook, "hook");
		String name = normalize(hook.getName());
		plugin.getDebug().debug("Registering hook: " + name);

		if (name.isEmpty()) throw new IllegalArgumentException("Hook name cannot be empty");
		if (this.hooks.containsKey(name))
			throw new IllegalArgumentException("Hook '" + hook.getName() + "' is already registered");

		hook.attach(this.plugin);
		this.hooks.put(name, hook);
		if (hook instanceof EconomyHook) this.economy.register((EconomyHook) hook, priority);
		if (this.started) enable(hook);
	}

	public void unregister(RosaHook hook) {
		if (hook == null) return;
		RosaHook registered = this.hooks.get(normalize(hook.getName()));
		if (registered != hook) return;

		disable(hook);
		this.hooks.remove(normalize(hook.getName()));
		if (hook instanceof EconomyHook) this.economy.unregister((EconomyHook) hook);
	}

	public void refresh() {
		if (!this.started) return;
		for (RosaHook hook : this.hooks.values()) {
			if (hook.isEnabled() && !hook.canBeEnabled()) disable(hook);
			if (!hook.isEnabled()) enable(hook);
		}
		this.economy.refreshActiveHook();
		plugin.getDebug().debug("Economy hook refreshed");
	}

	public boolean isEnabled(String name) {
		RosaHook hook = this.hooks.get(normalize(name));
		return hook != null && hook.isEnabled();
	}

	public Optional<RosaHook> getHook(String name) {
		return Optional.ofNullable(this.hooks.get(normalize(name)));
	}

	public <T extends RosaHook> Optional<T> getHook(Class<T> type) {
		Objects.requireNonNull(type, "type");
		for (RosaHook hook : this.hooks.values()) {
			if (type.isInstance(hook)) return Optional.of(type.cast(hook));
		}
		return Optional.empty();
	}

	public Collection<RosaHook> getHooks() {
		return Collections.unmodifiableList(new ArrayList<>(this.hooks.values()));
	}

	public List<String> getEnabledNames() {
		List<String> names = new ArrayList<>();
		for (RosaHook hook : this.hooks.values()) {
			if (hook.isEnabled()) names.add(hook.getName());
		}
		return Collections.unmodifiableList(names);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPluginEnable(PluginEnableEvent event) {
		refresh();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onServiceRegister(ServiceRegisterEvent event) {
		if (event.getProvider().getService().getName().equals("net.milkbowl.vault.economy.Economy"))
			refresh();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPluginDisable(PluginDisableEvent event) {
		String pluginName = event.getPlugin().getName();
		for (RosaHook hook : this.hooks.values()) {
			if (hook.isEnabled() && hook.dependsOn(pluginName)) disable(hook);
		}
		this.economy.refreshActiveHook();
	}

	@Override
	public void close() {
		if (this.started) HandlerList.unregisterAll(this);

		List<RosaHook> values = new ArrayList<>(this.hooks.values());
		for (int index = values.size() - 1; index >= 0; index--) disable(values.get(index));
		this.economy.refreshActiveHook();
		this.started = false;
	}

	private void enable(RosaHook hook) {
		if (hook.isEnabled() || !hook.canBeEnabled()) return;
		try {
			if (hook.enable())
				this.plugin.getLogger().info("Hooked into " + hook.getName());
			plugin.getDebug().debug("Enabled hook: " + hook.getName());
		} catch (Exception | LinkageError exception) {
			this.plugin.getLogger().log(Level.WARNING,
					"Could not enable hook '" + hook.getName() + "'", exception);
		} finally {
			this.economy.refreshActiveHook();
		}
	}

	private void disable(RosaHook hook) {
		if (!hook.isEnabled()) return;
		try {
			hook.disable();
		} catch (Exception | LinkageError exception) {
			this.plugin.getLogger().log(Level.WARNING,
					"Could not disable hook '" + hook.getName() + "'", exception);
		} finally {
			this.economy.refreshActiveHook();
		}
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}

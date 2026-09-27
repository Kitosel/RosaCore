package pl.kiosel.rosacore.hook;

import org.bukkit.plugin.Plugin;
import pl.kiosel.rosacore.RosaPlugin;

import java.util.Arrays;
import java.util.Objects;

public abstract class RosaHook {

	private RosaPlugin plugin;
	private boolean enabled;

	public abstract String getName();

	public abstract String[] getPluginDependencies();

	protected abstract boolean onEnable(RosaPlugin plugin) throws Exception;

	protected void onDisable() throws Exception {
	}

	public final boolean isEnabled() {
		return this.enabled;
	}

	public final boolean canBeEnabled() {
		if (this.plugin == null) return false;

		for (String dependency : dependencies()) {
			if (!this.plugin.getServer().getPluginManager().isPluginEnabled(dependency)) return false;
		}
		return true;
	}

	public final boolean dependsOn(String pluginName) {
		if (pluginName == null) return false;
		for (String dependency : dependencies()) {
			if (dependency.equalsIgnoreCase(pluginName)) return true;
		}
		return false;
	}

	protected final RosaPlugin getPlugin() {
		if (this.plugin == null)
			throw new IllegalStateException("Hook '" + getName() + "' is not attached to a plugin");
		return this.plugin;
	}

	protected final Plugin getDependencyPlugin(String name) {
		Objects.requireNonNull(name, "name");
		return getPlugin().getServer().getPluginManager().getPlugin(name);
	}

	final void attach(RosaPlugin plugin) {
		if (this.plugin != null && this.plugin != plugin)
			throw new IllegalStateException("Hook '" + getName() + "' is already attached to another plugin");
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		dependencies();
	}

	final boolean enable() throws Exception {
		if (this.enabled) return true;
		if (!canBeEnabled()) return false;

		this.enabled = onEnable(getPlugin());
		return this.enabled;
	}

	final void disable() throws Exception {
		if (!this.enabled) return;
		try {
			onDisable();
		} finally {
			this.enabled = false;
		}
	}

	private String[] dependencies() {
		String[] dependencies = Objects.requireNonNull(getPluginDependencies(),
				"Plugin dependencies of hook '" + getName() + "'");
		for (String dependency : dependencies) {
			if (dependency == null || dependency.trim().isEmpty())
				throw new IllegalStateException("Hook '" + getName() + "' has an empty plugin dependency");
		}
		return Arrays.copyOf(dependencies, dependencies.length);
	}
}

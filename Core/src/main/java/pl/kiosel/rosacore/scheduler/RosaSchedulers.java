package pl.kiosel.rosacore.scheduler;

import org.bukkit.plugin.Plugin;
import pl.kiosel.rosacore.version.ServerEnvironment;

public final class RosaSchedulers {

	private RosaSchedulers() {
	}

	public static RosaScheduler create(Plugin plugin, ServerEnvironment environment) {
		return environment.isFolia() ? new FoliaRosaScheduler(plugin) : new BukkitRosaScheduler(plugin);
	}
}

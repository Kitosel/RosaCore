package pl.kiosel.rosacore.scheduler;

import org.bukkit.plugin.Plugin;
import pl.kiosel.rosacore.version.ServerEnvironment;

import java.util.Objects;

public final class RosaSchedulers {

	private RosaSchedulers() {
	}

	public static RosaScheduler create(Plugin plugin, ServerEnvironment environment) {
		Objects.requireNonNull(plugin, "plugin");
		Objects.requireNonNull(environment, "environment");
		return environment.isFolia()
				? new FoliaRosaScheduler(plugin)
				: new BukkitRosaScheduler(plugin);
	}
}

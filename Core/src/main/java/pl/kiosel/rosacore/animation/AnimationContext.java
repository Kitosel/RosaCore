package pl.kiosel.rosacore.animation;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.kiosel.rosacore.RosaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class AnimationContext {

	private final RosaPlugin plugin;
	private final List<Player> viewers;
	private final Location location;

	AnimationContext(RosaPlugin plugin, List<Player> viewers, Location location) {
		this.plugin = plugin;
		this.viewers = Collections.unmodifiableList(new ArrayList<>(viewers));
		this.location = location.clone();
	}

	RosaPlugin getPlugin() {
		return plugin;
	}

	List<Player> getViewers() {
		return viewers;
	}

	Location getLocation() {
		return location.clone();
	}
}
